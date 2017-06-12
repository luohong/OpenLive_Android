package qsbk.app.play.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import qsbk.app.play.R;
import qsbk.app.play.widget.OnItemClickListener;

public class AnswerViewAdapter extends RecyclerView.Adapter<AnswerViewAdapter.ViewHolder> {

    private List<String> mItems;
    private OnItemClickListener mOnItemClickListener;
    private Context context;

    public AnswerViewAdapter(Context context, int wordCount, OnItemClickListener onItemClickListener) {
        this.context = context;
        initItems(wordCount);
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.words_view_item, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String word = mItems.get(position);
        holder.text.setText(word);
        holder.text.setSelected(TextUtils.isEmpty(word));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void notifyUiChanged(int wordCount) {
        initItems(wordCount);
        notifyDataSetChanged();
    }

    private void initItems(int wordCount) {
        mItems = new ArrayList<>(wordCount);
        for (int i = 0; i < wordCount; i++) {
            mItems.add("");
        }
    }

    public String notifyItemSelected(String word) {
        String answer = "";
        if (!mItems.contains("")) {
            return answer;// 文字已满
        }

        int position = -1;
        if (mItems.contains(word)) {
            position = mItems.indexOf(word);
            mItems.set(position, null);
        } else {
            for (int i = 0; i < mItems.size(); i++) {
                String item = mItems.get(i);
                if (TextUtils.isEmpty(item)) {
                    position = i;
                    mItems.set(i, word);
                    break;
                }
            }
        }
        if (position >= 0) {
            notifyItemChanged(position);
        }

        if (mItems.contains("")) {
            return answer;
        } else {
            for (int i = 0; i < mItems.size(); i++) {
                String item = mItems.get(i);
                answer += item;
            }
            return answer;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView text;

        public ViewHolder(final View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.text);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getLayoutPosition();

                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(itemView, mItems.get(position), position);
                    }
                    mItems.set(position, null);
                    notifyItemChanged(position);
                }
            });
        }
    }

}
