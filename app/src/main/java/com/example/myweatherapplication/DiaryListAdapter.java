package com.example.myweatherapplication; // 确保包名正确

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DiaryListAdapter extends RecyclerView.Adapter<DiaryListAdapter.ViewHolder> {

    private List<DiaryEntry> diaryEntries;
    private Context context;
    private OnDiaryItemClickListener clickListener; // 【新增】用于回调的监听器接口实例

    /**
     * 【新增】定义列表项点击事件的接口
     */
    public interface OnDiaryItemClickListener {
        void onItemClick(DiaryEntry diaryEntry);
        // 未来可以添加其他事件，如长按删除等
        // void onItemLongClick(DiaryEntry diaryEntry);
    }

    /**
     * 【修改】构造函数，接收Context, 数据列表, 和点击监听器
     */
    public DiaryListAdapter(Context context, List<DiaryEntry> diaryEntries, OnDiaryItemClickListener listener) {
        this.context = context;
        this.diaryEntries = diaryEntries;
        this.clickListener = listener; // 保存监听器实例
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_diary_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DiaryEntry currentEntry = diaryEntries.get(position);

        holder.textViewTitle.setText(currentEntry.getTitle());
        holder.textViewDate.setText(currentEntry.getDate());

        String contentPreview = currentEntry.getContent();
        if (contentPreview != null && contentPreview.length() > 50) { // 简单的内容预览截断
            contentPreview = contentPreview.substring(0, 50) + "...";
        }
        holder.textViewContentPreview.setText(contentPreview);

        // 【新增】为整个列表项视图(itemView)设置点击监听器
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                // 通过接口回调，将被点击的日记条目传递出去
                clickListener.onItemClick(currentEntry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return diaryEntries == null ? 0 : diaryEntries.size();
    }

    // 用于从Activity更新数据列表的方法 (这个方法很好，保留)
    public void setDiaryEntries(List<DiaryEntry> newEntries) {
        this.diaryEntries = newEntries;
        notifyDataSetChanged();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewDate;
        TextView textViewContentPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewItemDiaryTitle);
            textViewDate = itemView.findViewById(R.id.textViewItemDiaryDate);
            textViewContentPreview = itemView.findViewById(R.id.textViewItemDiaryContentPreview);
        }
    }
}