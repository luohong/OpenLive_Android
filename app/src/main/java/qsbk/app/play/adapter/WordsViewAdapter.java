package qsbk.app.play.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qsbk.app.play.R;
import qsbk.app.play.ui.LiveRoomActivity;
import qsbk.app.play.widget.OnItemClickListener;

public class WordsViewAdapter extends RecyclerView.Adapter<WordsViewAdapter.ViewHolder> {

    private List<String> mItems;
    private OnItemClickListener mOnItemClickListener;
    private Context context;
    private Map<String, Boolean> mSelectedMap = new HashMap<>();

    public WordsViewAdapter(Context context, List<String> words, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.mItems = words;
        this.mOnItemClickListener = onItemClickListener;
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
        holder.text.setSelected(mSelectedMap.containsKey(word) && mSelectedMap.get(word));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void notifyUiChanged(List<String> words) {
        mItems = words;
        mSelectedMap.clear();
        notifyDataSetChanged();
    }

    public void notifyItemSelected(String word) {
        if (mSelectedMap.containsKey(word)) {
            mSelectedMap.remove(word);
        }
        notifyDataSetChanged();
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

                    String word = mItems.get(position);

                    boolean filtered = false;
                    if (mOnItemClickListener != null) {
                        filtered = mOnItemClickListener.onItemClick(itemView, word, position);
                    }
                    if (!filtered) {
                        mSelectedMap.put(word, text.isSelected());
                        text.setSelected(!text.isSelected());
                    }
                }
            });
        }
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).hashCode();
    }
}
