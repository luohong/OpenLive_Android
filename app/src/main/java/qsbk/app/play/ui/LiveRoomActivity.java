package qsbk.app.play.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import qsbk.app.play.AppController;
import qsbk.app.play.utils.PhoneUtils;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import qsbk.app.play.R;
import qsbk.app.play.adapter.AnswerViewAdapter;
import qsbk.app.play.adapter.SmallVideoViewAdapter;
import qsbk.app.play.adapter.WordsViewAdapter;
import qsbk.app.play.common.Constants;
import qsbk.app.play.model.AGEventHandler;
import qsbk.app.play.model.ConstantApp;
import qsbk.app.play.model.VideoStatusData;
import qsbk.app.play.websocket.model.BaseMessage;
import qsbk.app.play.websocket.model.GameStartMessage;
import qsbk.app.play.websocket.model.MatchProgressMessage;
import qsbk.app.play.websocket.model.PerformMessage;
import qsbk.app.play.websocket.model.PerformTopicAnswerMessage;
import qsbk.app.play.websocket.model.PerformTopicAnswerResultMessage;
import qsbk.app.play.websocket.model.PerformTopicMessage;
import qsbk.app.play.websocket.model.PerformTopicSelectedMessage;
import qsbk.app.play.widget.GridVideoViewContainer;
import qsbk.app.play.widget.OnItemClickListener;
import qsbk.app.play.widget.VideoViewEventListener;

public class LiveRoomActivity extends BaseActivity implements AGEventHandler {

    private final static Logger log = LoggerFactory.getLogger(LiveRoomActivity.class);

    private GridVideoViewContainer mGridVideoViewContainer;

    private RelativeLayout mSmallVideoViewDock;

    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid

    private TextView tvMatchProgress;
    private TextView tvCountDown;

    private Gson mGson = new Gson();

    private ImageView btnSwitchClientRole;
    private ImageView btnSwitchCamera;

    private ImageView btnMicrophoneMute;

    private Handler mHandler;
    private WordsViewAdapter mWordsViewAdapter;

    private AnswerViewAdapter mAnswerViewAdapter;
    private WebSocket mWebSocket;
    private int mRoomId;
    private CountDownTimer mCountDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_room);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    private boolean isBroadcaster(int cRole) {
        return cRole == io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER;
    }

    private boolean isBroadcaster() {
        return isBroadcaster(config().mClientRole);
    }

    @Override
    protected void initUIandEvent() {
        mHandler = new Handler();
        event().addEventHandler(this);

        Intent i = getIntent();
        int cRole = i.getIntExtra(ConstantApp.ACTION_KEY_CROLE, 0);

        if (cRole == 0) {
            throw new RuntimeException("Should not reach here");
        }

        doConfigEngine(cRole);

        mGridVideoViewContainer = (GridVideoViewContainer) findViewById(R.id.grid_video_view_container);
        mGridVideoViewContainer.setItemEventHandler(new VideoViewEventListener() {
            @Override
            public void onItemDoubleClick(View v, Object item) {
                log.debug("onItemDoubleClick " + v + " " + item);

                if (mUidsList.size() < 2) {
                    return;
                }

                if (mViewType == VIEW_TYPE_DEFAULT)
                    switchToSmallVideoView(((VideoStatusData) item).mUid);
                else
                    switchToDefaultVideoView();
            }
        });

        btnSwitchClientRole = (ImageView) findViewById(R.id.btn_1);
        btnSwitchCamera = (ImageView) findViewById(R.id.btn_2);
        btnMicrophoneMute = (ImageView) findViewById(R.id.btn_3);

        if (isBroadcaster(cRole)) {
            SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
            rtcEngine().setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, 0));
            surfaceV.setZOrderOnTop(true);
            surfaceV.setZOrderMediaOverlay(true);

            mUidsList.put(0, surfaceV); // get first surface view

            mGridVideoViewContainer.initViewContainer(getApplicationContext(), 0, mUidsList); // first is now full view
            worker().preview(true, surfaceV, 0);
            broadcasterUI();
        } else {
            audienceUI();
        }

//        String roomName = i.getStringExtra(ConstantApp.ACTION_KEY_ROOM_NAME);
//        worker().joinChannel(roomName, config().mUid);
//
//        TextView textRoomName = (TextView) findViewById(R.id.room_name);
//        textRoomName.setText(roomName);

        tvMatchProgress = (TextView) findViewById(R.id.tv_match_progress);
        tvCountDown = (TextView) findViewById(R.id.tv_count_down);

        connectWebsocket();
    }

    private void connectWebsocket() {
        //新建client
        final OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(1000, TimeUnit.MILLISECONDS)
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .build();
        //构造request对象
        final String uid = PhoneUtils.getAndroidId();
        Request request = new Request.Builder()
                .url("ws://172.16.0.109:8080/Play/websocket?uid=" + uid)
//                .url("ws://192.168.199.239:8080/Play/websocket?uid=" + uid)
                .build();
        //new 一个websocket调用对象并建立连接
        client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(final WebSocket webSocket, Response response) {
                //保存引用，用于后续操作
                mWebSocket = webSocket;
                //打印一些内容
                Log.d("websocket", "client onOpen");
                Log.d("websocket", "client request header:" + response.request().headers());
                Log.d("websocket", "client response header:" + response.headers());
                Log.d("websocket", "client response:" + response);
            }

            @Override
            public void onMessage(final WebSocket webSocket, final String message) {
                //打印一些内容
                String string = message;
                Log.d("websocket", "client onMessage");
                Log.d("websocket", "message:" + string);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BaseMessage baseMsg = mGson.fromJson(message, BaseMessage.class);
                        if (baseMsg != null) {
                            switch (baseMsg.type) {
                                case Constants.MessageType.MATCH_PROGRESS:
                                    MatchProgressMessage progressMsg = mGson.fromJson(message, MatchProgressMessage.class);
                                    tvMatchProgress.setVisibility(View.VISIBLE);
                                    tvMatchProgress.setText(String.format("已匹配 %d/%d", progressMsg.progress, progressMsg.total));
                                    break;
                                case Constants.MessageType.GAME_START:
                                    GameStartMessage startMessage = mGson.fromJson(message, GameStartMessage.class);
                                    mRoomId = startMessage.roomId;
                                    tvMatchProgress.setVisibility(View.GONE);

                                    worker().joinChannel(String.valueOf(mRoomId), config().mUid);

                                    TextView textRoomName = (TextView) findViewById(R.id.room_name);
                                    textRoomName.setText(String.format("房间号:%s", mRoomId));
                                    break;
                                case Constants.MessageType.PERFORM:
                                    PerformMessage performMsg = mGson.fromJson(message, PerformMessage.class);

                                    if (!TextUtils.isEmpty(performMsg.who)) {
                                        doSwitchToBroadcaster(performMsg.who.equals(uid));
                                    }

                                    if (performMsg.topics != null) {
                                        final String[] topics = performMsg.topics.toArray(new String[performMsg.topics.size()]);
                                        AlertDialog.Builder builder = new AlertDialog.Builder(LiveRoomActivity.this);

                                        builder.setTitle("ok")
                                                .setIcon(R.drawable.ic_launcher)
                                                .setItems(topics, new DialogInterface.OnClickListener() {

                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();

                                                        String topic = topics[which];
                                                        Toast.makeText(LiveRoomActivity.this, topic, Toast.LENGTH_SHORT).show();
                                                        Log.i("abc", "i" + which);

                                                        PerformTopicSelectedMessage topicMsg = new PerformTopicSelectedMessage(mRoomId, topic);
                                                        mWebSocket.send(mGson.toJson(topicMsg));
                                                    }
                                                }).show();
                                    }

                                    startCountDown(performMsg.count);
                                    break;
                                case Constants.MessageType.PERFORM_TOPIC:
                                    PerformTopicMessage performTopicMsg = mGson.fromJson(message, PerformTopicMessage.class);
                                    bindToAnswerView(performTopicMsg.wordCount);
                                    bindToWordsView(performTopicMsg.words);
                                    break;
                                case Constants.MessageType.PERFORM_TOPIC_ANSWER_RESULT:
                                    PerformTopicAnswerResultMessage resultMessage = mGson.fromJson(message, PerformTopicAnswerResultMessage.class);
                                    Toast.makeText(LiveRoomActivity.this, "答案 [" + resultMessage.answer + "] " + (resultMessage.result ? "正确" : "不正确"), Toast.LENGTH_SHORT).show();
                                    break;
                                case Constants.MessageType.GAME_OVER:
                                    Toast.makeText(LiveRoomActivity.this, "游戏结束", Toast.LENGTH_SHORT).show();
                                    finish();
                                    break;
                            }
                        }
                    }
                });
            }

            @Override
            public void onMessage(final WebSocket webSocket, ByteString bytes) {
                //打印一些内容
                String string = bytes.toString();
                Log.d("websocket", "client onMessage");
                Log.d("websocket", "message:" + string);
            }

            @Override
            public void onClosing(final WebSocket webSocket, int code, String reason) {
                //打印一些内容
                Log.d("websocket", "client onClose");
                Log.d("websocket", "code:" + code + " reason:" + reason);
                // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
                client.dispatcher().executorService().shutdown();
            }

            @Override
            public void onFailure(final WebSocket webSocket, Throwable t, Response response) {
                //发生错误时会回调到这
                Log.d("websocket", "client onFailure");
                Log.d("websocket", "throwable:" + t);
                Log.d("websocket", "response:" + response);

//                webSocket.send("Hello...");
//                webSocket.send("...World!");
//                webSocket.send(ByteString.decodeHex("deadbeef"));
//                webSocket.close(1000, "Goodbye, World!");
            }
        });
    }

    private void startCountDown(long countDown) {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }

        // 此处必须每次重新new一个实例，否则下一局游戏的倒计时开始时间 依然是 刚进直播间看到的那一局的倒计时开始时间
        mCountDownTimer = new CountDownTimer(countDown * 1000 + 500, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                long count = millisUntilFinished / 1000;
                if (count >= 0 && count <= 10) {
                    if (count % 2 == 0) {
                        tvCountDown.setTextColor(AppController.instance().getResources().getColor(android.R.color.holo_red_light));
                    } else {
                        tvCountDown.setTextColor(AppController.instance().getResources().getColor(android.R.color.white));
                    }
                } else {
                    tvCountDown.setTextColor(AppController.instance().getResources().getColor(android.R.color.white));
                }
                long minute = count / 60;
                long second = count % 60;
                StringBuilder builder = new StringBuilder();
                if (minute < 10) {
                    builder.append(0);
                }
                builder.append(minute).append(':');
                if (second < 10) {
                    builder.append(0);
                }
                builder.append(second);
                tvCountDown.setText(builder.toString());
            }

            @Override
            public void onFinish() {
                tvCountDown.setText("00:00");
            }
        };
        mCountDownTimer.start();
    }

    private void bindToAnswerView(int wordCount) {
        RecyclerView recycler = (RecyclerView) findViewById(R.id.answer_view_container);

        boolean create = false;

        if (mAnswerViewAdapter == null) {
            create = true;
            mAnswerViewAdapter = new AnswerViewAdapter(this, wordCount, new OnItemClickListener() {

                @Override
                public boolean onItemClick(View itemView, String word, int position) {
                    if (!TextUtils.isEmpty(word) && mWordsViewAdapter != null) {
                        mWordsViewAdapter.notifyItemSelected(word);
                    }
                    return true;
                }

            });
//            mAnswerViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);

        recycler.setLayoutManager(new LinearLayoutManager(this, GridLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(mAnswerViewAdapter);

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mAnswerViewAdapter.notifyUiChanged(wordCount);
        }
        recycler.setVisibility(View.VISIBLE);

        findViewById(R.id.line_under_answer).setVisibility(View.VISIBLE);
    }

    private void bindToWordsView(List<String> words) {
        RecyclerView recycler = (RecyclerView) findViewById(R.id.words_view_container);

        boolean create = false;

        if (mWordsViewAdapter == null) {
            create = true;
            mWordsViewAdapter = new WordsViewAdapter(this, words, new OnItemClickListener() {

                @Override
                public boolean onItemClick(View itemView, String word, int position) {
                    String answer = mAnswerViewAdapter.notifyItemSelected(word);
                    boolean isNotEmpty = !TextUtils.isEmpty(answer);
                    if (isNotEmpty) {
                        if (mWebSocket != null) {
                            PerformTopicAnswerMessage answerMessage = new PerformTopicAnswerMessage(mRoomId, answer);
                            mWebSocket.send(mGson.toJson(answerMessage));
                        }
                    }
                    return isNotEmpty;
                }

            });
            mWordsViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);

        recycler.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(mWordsViewAdapter);

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mWordsViewAdapter.notifyUiChanged(words);
        }
        recycler.setVisibility(View.VISIBLE);
    }

    private void broadcasterUI() {
        btnSwitchClientRole.setTag(true);
        btnSwitchClientRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = v.getTag();
                if (tag != null && (boolean) tag) {
                    doSwitchToBroadcaster(false);
                } else {
                    doSwitchToBroadcaster(true);
                }
            }
        });
        btnSwitchClientRole.setColorFilter(getResources().getColor(R.color.agora_blue), PorterDuff.Mode.MULTIPLY);

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                worker().getRtcEngine().switchCamera();
            }
        });

        btnMicrophoneMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = v.getTag();
                boolean flag = true;
                if (tag != null && (boolean) tag) {
                    flag = false;
                }
                worker().getRtcEngine().muteLocalAudioStream(flag);
                ImageView button = (ImageView) v;
                button.setTag(flag);
                if (flag) {
                    button.setColorFilter(getResources().getColor(R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
                } else {
                    button.clearColorFilter();
                }
            }
        });
    }

    private void audienceUI() {
        btnSwitchClientRole.setTag(null);
        btnSwitchClientRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = v.getTag();
                if (tag != null && (boolean) tag) {
                    doSwitchToBroadcaster(false);
                } else {
                    doSwitchToBroadcaster(true);
                }
            }
        });
        btnSwitchClientRole.clearColorFilter();
        btnSwitchCamera.setVisibility(View.GONE);
        btnMicrophoneMute.setTag(null);
        btnMicrophoneMute.setVisibility(View.GONE);
        btnMicrophoneMute.clearColorFilter();
    }

    private void doConfigEngine(int cRole) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int prefIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
        if (prefIndex > ConstantApp.VIDEO_PROFILES.length - 1) {
            prefIndex = ConstantApp.DEFAULT_PROFILE_IDX;
        }
        int vProfile = ConstantApp.VIDEO_PROFILES[prefIndex];

        worker().configEngine(cRole, vProfile);
    }

    @Override
    protected void deInitUIandEvent() {
        mHandler.removeCallbacksAndMessages(null);
        doLeaveChannel();
        event().removeEventHandler(this);

        mUidsList.clear();
    }

    private void doLeaveChannel() {
        worker().leaveChannel(config().mChannel);
        if (isBroadcaster()) {
            worker().preview(false, null, 0);
        }
    }

    public void onClickClose(View view) {
        finish();
    }

    public void onShowHideClicked(View view) {
        boolean toHide = true;
        if (view.getTag() != null && (boolean) view.getTag()) {
            toHide = false;
        }
        view.setTag(toHide);

        doShowButtons(toHide);
    }

    private void doShowButtons(boolean hide) {
        View topArea = findViewById(R.id.top_area);
        topArea.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);

        View button1 = findViewById(R.id.btn_1);
        button1.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);

        View button2 = findViewById(R.id.btn_2);
        View button3 = findViewById(R.id.btn_3);
        if (isBroadcaster()) {
            button2.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
            button3.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        } else {
            button2.setVisibility(View.INVISIBLE);
            button3.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        doRenderRemoteUi(uid);
    }

    private void doSwitchToBroadcaster(boolean broadcaster) {
        final int currentHostCount = mUidsList.size();
        final int uid = config().mUid;
        log.debug("doSwitchToBroadcaster " + currentHostCount + " " + (uid & 0XFFFFFFFFL) + " " + broadcaster);

        if (broadcaster) {
            doConfigEngine(io.agora.rtc.Constants.CLIENT_ROLE_BROADCASTER);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doRenderRemoteUi(uid);

                    broadcasterUI();

                    doShowButtons(false);
                }
            }, 1000); // wait for reconfig engine
        } else {
            stopInteraction(currentHostCount, uid);
        }
    }

    private void stopInteraction(final int currentHostCount, final int uid) {
        doConfigEngine(io.agora.rtc.Constants.CLIENT_ROLE_AUDIENCE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doRemoveRemoteUi(uid);

                audienceUI();

                doShowButtons(false);
            }
        }, 1000); // wait for reconfig engine
    }

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
                surfaceV.setZOrderOnTop(true);
                surfaceV.setZOrderMediaOverlay(true);
                mUidsList.put(uid, surfaceV);
                if (config().mUid == uid) {
                    rtcEngine().setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                } else {
                    rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                }

                if (mViewType == VIEW_TYPE_DEFAULT) {
                    log.debug("doRenderRemoteUi VIEW_TYPE_DEFAULT" + " " + (uid & 0xFFFFFFFFL));
                    switchToDefaultVideoView();
                } else {
                    int bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                    log.debug("doRenderRemoteUi VIEW_TYPE_SMALL" + " " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL));
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    @Override
    public void onJoinChannelSuccess(final String channel, final int uid, final int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                if (mUidsList.containsKey(uid)) {
                    log.debug("already added to UI, ignore it " + (uid & 0xFFFFFFFFL) + " " + mUidsList.get(uid));
                    return;
                }

                final boolean isBroadcaster = isBroadcaster();
                log.debug("onJoinChannelSuccess " + channel + " " + uid + " " + elapsed + " " + isBroadcaster);

                worker().getEngineConfig().mUid = uid;

                SurfaceView surfaceV = mUidsList.remove(0);
                if (surfaceV != null) {
                    mUidsList.put(uid, surfaceV);
                }
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        log.debug("onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
        doRemoveRemoteUi(uid);
    }

    private void requestRemoteStreamType(final int currentHostCount) {
        log.debug("requestRemoteStreamType " + currentHostCount);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                HashMap.Entry<Integer, SurfaceView> highest = null;
                for (HashMap.Entry<Integer, SurfaceView> pair : mUidsList.entrySet()) {
                    log.debug("requestRemoteStreamType " + currentHostCount + " local " + (config().mUid & 0xFFFFFFFFL) + " " + (pair.getKey() & 0xFFFFFFFFL) + " " + pair.getValue().getHeight() + " " + pair.getValue().getWidth());
                    if (pair.getKey() != config().mUid && (highest == null || highest.getValue().getHeight() < pair.getValue().getHeight())) {
                        if (highest != null) {
                            rtcEngine().setRemoteVideoStreamType(highest.getKey(), io.agora.rtc.Constants.VIDEO_STREAM_LOW);
                            log.debug("setRemoteVideoStreamType switch highest VIDEO_STREAM_LOW " + currentHostCount + " " + (highest.getKey() & 0xFFFFFFFFL) + " " + highest.getValue().getWidth() + " " + highest.getValue().getHeight());
                        }
                        highest = pair;
                    } else if (pair.getKey() != config().mUid && (highest != null && highest.getValue().getHeight() >= pair.getValue().getHeight())) {
                        rtcEngine().setRemoteVideoStreamType(pair.getKey(), io.agora.rtc.Constants.VIDEO_STREAM_LOW);
                        log.debug("setRemoteVideoStreamType VIDEO_STREAM_LOW " + currentHostCount + " " + (pair.getKey() & 0xFFFFFFFFL) + " " + pair.getValue().getWidth() + " " + pair.getValue().getHeight());
                    }
                }
                if (highest != null && highest.getKey() != 0) {
                    rtcEngine().setRemoteVideoStreamType(highest.getKey(), io.agora.rtc.Constants.VIDEO_STREAM_HIGH);
                    log.debug("setRemoteVideoStreamType VIDEO_STREAM_HIGH " + currentHostCount + " " + (highest.getKey() & 0xFFFFFFFFL) + " " + highest.getValue().getWidth() + " " + highest.getValue().getHeight());
                }
            }
        }, 500);
    }

    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                mUidsList.remove(uid);

                int bigBgUid = -1;
                if (mSmallVideoViewAdapter != null) {
                    bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                }

                log.debug("doRemoveRemoteUi " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL));

                if (mViewType == VIEW_TYPE_DEFAULT || uid == bigBgUid) {
                    switchToDefaultVideoView();
                } else {
                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }

    private SmallVideoViewAdapter mSmallVideoViewAdapter;

    private void switchToDefaultVideoView() {
        if (mSmallVideoViewDock != null)
            mSmallVideoViewDock.setVisibility(View.GONE);
        mGridVideoViewContainer.initViewContainer(getApplicationContext(), config().mUid, mUidsList);

        mViewType = VIEW_TYPE_DEFAULT;

        int sizeLimit = mUidsList.size();
        if (sizeLimit > ConstantApp.MAX_PEER_COUNT + 1) {
            sizeLimit = ConstantApp.MAX_PEER_COUNT + 1;
        }
        for (int i = 0; i < sizeLimit; i++) {
            int uid = mGridVideoViewContainer.getItem(i).mUid;
            if (config().mUid != uid) {
                rtcEngine().setRemoteVideoStreamType(uid, io.agora.rtc.Constants.VIDEO_STREAM_HIGH);
                log.debug("setRemoteVideoStreamType VIDEO_STREAM_HIGH " + mUidsList.size() + " " + (uid & 0xFFFFFFFFL));
            }
        }
    }

    private void switchToSmallVideoView(int uid) {
        HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(uid, mUidsList.get(uid));
        mGridVideoViewContainer.initViewContainer(getApplicationContext(), uid, slice);

        bindToSmallVideoView(uid);

        mViewType = VIEW_TYPE_SMALL;

        requestRemoteStreamType(mUidsList.size());
    }

    public int mViewType = VIEW_TYPE_DEFAULT;

    public static final int VIEW_TYPE_DEFAULT = 0;

    public static final int VIEW_TYPE_SMALL = 1;

    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        RecyclerView recycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, exceptUid, mUidsList, new VideoViewEventListener() {
                @Override
                public void onItemDoubleClick(View v, Object item) {
                    switchToDefaultVideoView();
                }
            });
            mSmallVideoViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);

        recycler.setLayoutManager(new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false));
        recycler.setAdapter(mSmallVideoViewAdapter);

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        recycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }

    @Override
    public void finish() {
        if (mWebSocket != null) {
            mWebSocket.close(1001, "Going away");
        }
        super.finish();
    }
}
