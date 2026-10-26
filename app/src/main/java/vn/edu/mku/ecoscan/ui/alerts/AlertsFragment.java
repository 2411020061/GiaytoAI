package vn.edu.mku.ecoscan.ui.alerts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AlertDao;
import vn.edu.mku.ecoscan.data.AlertEntity;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.utils.BiometricAuthHelper;
import vn.edu.mku.ecoscan.utils.ExpiryReminderWorker;

public class AlertsFragment extends Fragment {

    private RecyclerView rvAlerts;
    private AlertAdapter adapter;
    private TextView chipAlertAll, chipAlertAction, chipAlertRead, tvAlertSubtitle;
    private View btnCreateReminder, btnAskService, btnSecurityCheck;

    private List<AlertEntity> allAlerts = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_alerts, container, false);

        findViews(root);
        setupRecyclerView();
        setupListeners();
        loadAlerts();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAlerts();
    }

    private void findViews(View root) {
        rvAlerts = root.findViewById(R.id.rvAlerts);
        chipAlertAll = root.findViewById(R.id.chipAlertAll);
        chipAlertAction = root.findViewById(R.id.chipAlertAction);
        chipAlertRead = root.findViewById(R.id.chipAlertRead);
        tvAlertSubtitle = root.findViewById(R.id.tvAlertSubtitle);

        btnCreateReminder = root.findViewById(R.id.btnCreateReminder);
        btnAskService = root.findViewById(R.id.btnAskService);
        btnSecurityCheck = root.findViewById(R.id.btnSecurityCheck);
    }

    private void setupRecyclerView() {
        adapter = new AlertAdapter();
        rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlerts.setAdapter(adapter);
    }

    private void setupListeners() {
        chipAlertAll.setOnClickListener(v -> applyFilter("ALL"));
        chipAlertAction.setOnClickListener(v -> applyFilter("ACTION"));
        chipAlertRead.setOnClickListener(v -> applyFilter("READ"));

        if (btnCreateReminder != null) {
            btnCreateReminder.setOnClickListener(v -> {
                ExpiryReminderWorker.scheduleDailyReminderCheck(requireContext());
                ExpiryReminderWorker.sendNotification(
                        requireContext(),
                        "🔔 Đã kích hoạt lịch nhắc tự động",
                        "Hệ thống sẽ tự động phát thông báo khi Căn cước công dân sắp hết hạn."
                );
                Toast.makeText(requireContext(), "⚡ Đã cài đặt nhắc nhở tự động trên thiết bị!", Toast.LENGTH_LONG).show();
            });
        }

        if (btnAskService != null) {
            btnAskService.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Trợ lý AI: Dịch vụ gia hạn trực tuyến tại dichvucong.gov.vn", Toast.LENGTH_LONG).show()
            );
        }

        if (btnSecurityCheck != null) {
            btnSecurityCheck.setOnClickListener(v -> {
                BiometricAuthHelper.authenticate(requireActivity(), new BiometricAuthHelper.BiometricCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "🛡️ Xác thực Vân tay/Khuôn mặt thành công! Tất cả giấy tờ cá nhân đã được bảo mật an toàn.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Toast.makeText(requireContext(), "Xác thực không thành công: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled() {
                        Toast.makeText(requireContext(), "Hủy kiểm tra bảo mật", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    private void applyFilter(String filter) {
        this.currentFilter = filter;

        chipAlertAll.setBackgroundResource("ALL".equals(filter) ? R.drawable.badge_rounded_active : R.drawable.badge_rounded_outline);
        chipAlertAll.setTextColor("ALL".equals(filter) ? getResources().getColor(R.color.badge_active_text, null) : getResources().getColor(R.color.text_secondary, null));

        chipAlertAction.setBackgroundResource("ACTION".equals(filter) ? R.drawable.badge_rounded_active : R.drawable.badge_rounded_outline);
        chipAlertAction.setTextColor("ACTION".equals(filter) ? getResources().getColor(R.color.badge_active_text, null) : getResources().getColor(R.color.text_secondary, null));

        chipAlertRead.setBackgroundResource("READ".equals(filter) ? R.drawable.badge_rounded_active : R.drawable.badge_rounded_outline);
        chipAlertRead.setTextColor("READ".equals(filter) ? getResources().getColor(R.color.badge_active_text, null) : getResources().getColor(R.color.text_secondary, null));

        filterList();
    }

    private void loadAlerts() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AlertDao dao = AppDatabase.getInstance(requireContext()).alertDao();
            List<AlertEntity> list = dao.getAll();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    allAlerts = list != null ? list : new ArrayList<>();
                    filterList();
                });
            }
        });
    }

    private void filterList() {
        List<AlertEntity> filtered = new ArrayList<>();
        for (AlertEntity alert : allAlerts) {
            if ("ACTION".equals(currentFilter)) {
                if ("HIGH".equalsIgnoreCase(alert.priority) || "CHECK".equalsIgnoreCase(alert.priority)) {
                    filtered.add(alert);
                }
            } else if ("READ".equals(currentFilter)) {
                if (alert.isRead) {
                    filtered.add(alert);
                }
            } else {
                filtered.add(alert);
            }
        }

        adapter.setData(filtered);
        tvAlertSubtitle.setText(filtered.size() + " mục cần bạn chú ý");
    }
}
