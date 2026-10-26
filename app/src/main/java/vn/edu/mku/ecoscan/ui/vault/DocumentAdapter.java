package vn.edu.mku.ecoscan.ui.vault;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.DocumentEntity;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DocumentEntity document);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(DocumentEntity document);
    }

    private List<DocumentEntity> list = new ArrayList<>();
    private final OnItemClickListener clickListener;
    private OnItemDeleteListener deleteListener;

    public DocumentAdapter(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public DocumentAdapter(OnItemClickListener clickListener, OnItemDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setData(List<DocumentEntity> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentEntity doc = list.get(position);
        holder.tvDocTitle.setText(doc.title);
        holder.tvDocNumber.setText(doc.documentNumber);

        if ("EXPIRING_SOON".equalsIgnoreCase(doc.status) || doc.daysLeft <= 30) {
            holder.tvDocStatusBadge.setText("Còn " + doc.daysLeft + " ngày");
            holder.tvDocStatusBadge.setBackgroundResource(R.drawable.badge_rounded_warning);
            holder.tvDocStatusBadge.setTextColor(Color.parseColor("#D9531E"));
        } else {
            holder.tvDocStatusBadge.setText("Còn hiệu lực");
            holder.tvDocStatusBadge.setBackgroundResource(R.drawable.badge_rounded_active);
            holder.tvDocStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(doc);
        });

        if (holder.btnDeleteDocItem != null) {
            holder.btnDeleteDocItem.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onItemDelete(doc);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDocIcon;
        TextView tvDocTitle, tvDocNumber, tvDocStatusBadge;
        View btnDeleteDocItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgDocIcon = itemView.findViewById(R.id.imgDocIcon);
            tvDocTitle = itemView.findViewById(R.id.tvDocTitle);
            tvDocNumber = itemView.findViewById(R.id.tvDocNumber);
            tvDocStatusBadge = itemView.findViewById(R.id.tvDocStatusBadge);
            btnDeleteDocItem = itemView.findViewById(R.id.btnDeleteDocItem);
        }
    }
}
