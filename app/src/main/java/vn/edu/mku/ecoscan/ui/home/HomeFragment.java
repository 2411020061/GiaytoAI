package vn.edu.mku.ecoscan.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.concurrent.Executors;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.data.DocumentDao;
import vn.edu.mku.ecoscan.data.DocumentEntity;
import vn.edu.mku.ecoscan.ui.MainActivity;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvProtectedCount, tvTotalCount, tvActiveCount;
    private TextView tvExpiringTitle, tvExpiringDate, tvDaysBadge;
    private View btnQuickScan, btnViewExpiringAll, btnViewAlertsAll, cardExpiringItem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        findViews(root);
        setupListeners();
        loadDashboardData();

        return root;
    }

    private void findViews(View root) {
        tvGreeting = root.findViewById(R.id.tvGreeting);
        tvProtectedCount = root.findViewById(R.id.tvProtectedCount);
        tvTotalCount = root.findViewById(R.id.tvTotalCount);
        tvActiveCount = root.findViewById(R.id.tvActiveCount);

        tvExpiringTitle = root.findViewById(R.id.tvExpiringTitle);
        tvExpiringDate = root.findViewById(R.id.tvExpiringDate);
        tvDaysBadge = root.findViewById(R.id.tvDaysBadge);

        btnQuickScan = root.findViewById(R.id.btnQuickScan);
        btnViewExpiringAll = root.findViewById(R.id.btnViewExpiringAll);
        btnViewAlertsAll = root.findViewById(R.id.btnViewAlertsAll);
        cardExpiringItem = root.findViewById(R.id.cardExpiringItem);
    }

    private void setupListeners() {
        if (btnQuickScan != null) {
            btnQuickScan.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_scan);
                }
            });
        }

        View.OnClickListener goToVault = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_vault);
            }
        };

        if (btnViewExpiringAll != null) btnViewExpiringAll.setOnClickListener(goToVault);
        if (cardExpiringItem != null) cardExpiringItem.setOnClickListener(goToVault);

        if (btnViewAlertsAll != null) {
            btnViewAlertsAll.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_alerts);
                }
            });
        }
    }

    private void loadDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            DocumentDao docDao = db.documentDao();

            int total = docDao.getCount();
            int active = docDao.getActiveCount();
            List<DocumentEntity> expiringList = docDao.getExpiringSoon();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    tvTotalCount.setText(String.format("%02d", total));
                    tvActiveCount.setText(String.format("%02d", active));
                    tvProtectedCount.setText(total + " Căn cước công dân đang được bảo vệ");

                    if (expiringList != null && !expiringList.isEmpty()) {
                        DocumentEntity topExp = expiringList.get(0);
                        tvExpiringTitle.setText(topExp.title + " - " + topExp.holderName);
                        tvExpiringDate.setText("Hết hạn " + topExp.expiryDate);
                        tvDaysBadge.setText("Còn " + topExp.daysLeft + " ngày");
                    } else {
                        tvExpiringTitle.setText("Không có CCCD sắp hết hạn");
                        tvExpiringDate.setText("Tất cả thẻ CCCD đều còn hiệu lực");
                        tvDaysBadge.setText("An toàn");
                    }
                });
            }
        });
    }
}
