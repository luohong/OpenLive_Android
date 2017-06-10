package qsbk.app.play.common;

import io.agora.rtc.RtcEngine;

public class Constants {

    public static final String MEDIA_SDK_VERSION;

    static {
        String sdk = "undefined";
        try {
            sdk = RtcEngine.getSdkVersion();
        } catch (Throwable e) {
        }
        MEDIA_SDK_VERSION = sdk;
    }

    public static boolean PRP_ENABLED = true;
    public static float PRP_DEFAULT_LIGHTNESS = .65f;
    public static float PRP_DEFAULT_SMOOTHNESS = 1.0f;
    public static final float PRP_MAX_LIGHTNESS = 1.0f;
    public static final float PRP_MAX_SMOOTHNESS = 1.0f;

    public interface MessageType {

        int MATCH_PROGRESS = 1005;

        int GAME_START = 2001;
        int PERFORM = 2002;
        int PERFORM_TOPIC_SELECTED = 2003;
        int PERFORM_TOPIC = 2004;
        int PERFORM_TOPIC_ANSWER = 2005;
        int PERFORM_TOPIC_ANSWER_RESULT = 2006;
        int GAME_OVER = 2999;

    }

}
