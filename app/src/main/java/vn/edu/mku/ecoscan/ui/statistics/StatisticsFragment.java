package vn.edu.mku.ecoscan.ui.statistics;

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

public class StatisticsFragment extends Fragment {

    private TextView tvStatTotal, tvStatActive, tvStatExpiring, tvStatExpired;
    private TextView tvCountCccd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_statistics, container, false);

        findViews(root);
        loadStatistics();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
    }

    private void findViews(View root) {
        tvStatTotal = root.findViewById(R.id.tvStatTotal);
        tvStatActive = root.findViewById(R.id.tvStatActive);
        tvStatExpiring = root.findViewById(R.id.tvStatExpiring);
        tvStatExpired = root.findViewById(R.id.tvStatExpired);

        tvCountCccd = root.findViewById(R.id.tvCountCccd);
    }

    private void loadStatistics() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            DocumentDao dao = db.documentDao();
            List<DocumentEntity> list = dao.getAll();

            int total = list.size();
            int active = 0;
            int expiring = 0;
            int expired = 0;

            for (DocumentEntity doc : list) {
                if ("ACTIVE".equalsIgnoreCase(doc.status) && doc.daysLeft > 30) {
                    active++;
                } else if ("EXPIRED".equalsIgnoreCase(doc.status) || doc.daysLeft <= 0) {
                    expired++;
                } else {
                    expiring++;
                }
            }

            if (isAdded()) {
                int finalActive = active;
                int finalExpiring = expiring;
                int finalExpired = expired;

                requireActivity().runOnUiThread(() -> {
                    tvStatTotal.setText(String.valueOf(total));
                    tvStatActive.setText(String.valueOf(finalActive));
                    tvStatExpiring.setText(String.valueOf(finalExpiring));
                    tvStatExpired.setText(String.valueOf(finalExpired));

                    if (tvCountCccd != null) {
                        tvCountCccd.setText(String.valueOf(total));
                    }
                });
            }
        });
    }
}
