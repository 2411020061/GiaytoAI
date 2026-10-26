package vn.edu.mku.ecoscan.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.ScanResult;

public class HistoryRvAdapter extends RecyclerView.Adapter<HistoryRvAdapter.ViewHolder>{
    private final int LAYOUT_ID;
    private List<ScanResult> data;

    public HistoryRvAdapter(int layoutId, List<ScanResult> data) {
        this.LAYOUT_ID = layoutId;
        this.data = data;
    }

    @NonNull
    @Override
    public HistoryRvAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(LAYOUT_ID, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryRvAdapter.ViewHolder holder, int position) {
        ScanResult item = (ScanResult) data.get(holder.getAdapterPosition());
        holder.textLabel.setText(item.label);
        holder.textInfo.setText(String.format("%s", item.recyclable ? "Recyclable" : "Non-Recyclable"));
        holder.textConfidence.setText(String.format(Locale.getDefault(), "%.0f%%", item.confidence * 100));

        if (item.imagePath != null) {
            Picasso.get()
                    .load(new File(item.imagePath))
                    .into(holder.imageThumb);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public ScanResult getItemAt(int position) {
        return (ScanResult) data.get(position);
    }

    public void removeAt(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageThumb;
        TextView textLabel, textInfo, textConfidence;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumb = itemView.findViewById(R.id.imageThumb);
            textLabel = itemView.findViewById(R.id.textLabel);
            textInfo = itemView.findViewById(R.id.textInfo);
            textConfidence = itemView.findViewById(R.id.textConfidence);
        }
    }
}
