package vn.edu.mku.ecoscan.ui.alerts;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AlertEntity;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    private List<AlertEntity> list = new ArrayList<>();

    public void setData(List<AlertEntity> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertEntity alert = list.get(position);
        holder.tvAlertTitle.setText(alert.title);
        holder.tvAlertDesc.setText(alert.description);
        holder.tvAlertTime.setText(alert.timeText);

        if ("HIGH".equalsIgnoreCase(alert.priority)) {
            holder.tvAlertBadge.setText("ƯU TIÊN CAO");
            holder.tvAlertBadge.setBackgroundResource(R.drawable.badge_rounded_warning);
            holder.tvAlertBadge.setTextColor(Color.parseColor("#D9531E"));
        } else if ("CHECK".equalsIgnoreCase(alert.priority)) {
            holder.tvAlertBadge.setText("CẦN KIỂM TRA");
            holder.tvAlertBadge.setBackgroundResource(R.drawable.badge_rounded_outline);
            holder.tvAlertBadge.setTextColor(Color.parseColor("#E65100"));
        } else {
            holder.tvAlertBadge.setText("THÔNG TIN");
            holder.tvAlertBadge.setBackgroundResource(R.drawable.badge_rounded_outline);
            holder.tvAlertBadge.setTextColor(Color.parseColor("#1565C0"));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlertBadge, tvAlertTime, tvAlertTitle, tvAlertDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlertBadge = itemView.findViewById(R.id.tvAlertBadge);
            tvAlertTime = itemView.findViewById(R.id.tvAlertTime);
            tvAlertTitle = itemView.findViewById(R.id.tvAlertTitle);
            tvAlertDesc = itemView.findViewById(R.id.tvAlertDesc);
        }
    }
}
