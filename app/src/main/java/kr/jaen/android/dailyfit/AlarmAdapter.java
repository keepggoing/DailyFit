package kr.jaen.android.dailyfit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.VH> {
    public interface OnAlarmActionListener {
        void onEdit(AlarmItem item);
        void onDelete(AlarmItem item);
        void onToggle(AlarmItem item, boolean enabled);
    }

    private final List<AlarmItem> list;
    private final AlarmManager manager;
    private final OnAlarmActionListener listener;

    public AlarmAdapter(List<AlarmItem> list,
                        AlarmManager manager,
                        OnAlarmActionListener listener) {
        this.list     = list;
        this.manager  = manager;
        this.listener = listener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        AlarmItem a = list.get(position);
        holder.tvTime.setText(a.displayTime);
        holder.swEnable.setChecked(a.isEnabled);

        holder.swEnable.setOnCheckedChangeListener((btn, on) -> {
            listener.onToggle(a, on);
        });
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(a));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(a));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView    tvTime;
        Switch      swEnable;
        ImageButton btnEdit, btnDelete;

        VH(View itemView) {
            super(itemView);
            tvTime    = itemView.findViewById(R.id.tvItemTime);
            swEnable  = itemView.findViewById(R.id.swItemEnable);
            btnEdit   = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
