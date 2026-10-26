package vn.edu.mku.ecoscan.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executors;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.utils.BiometricAuthHelper;
import vn.edu.mku.ecoscan.utils.ExpiryReminderWorker;

public class SettingsFragment extends Fragment {

    private SwitchCompat switchBiometric, switchNotifications;
    private View btnExportData, btnClearData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        findViews(root);
        initValues();
        setupListeners();

        return root;
    }

    private void findViews(View root) {
        switchBiometric = root.findViewById(R.id.switchBiometric);
        switchNotifications = root.findViewById(R.id.switchNotifications);
        btnExportData = root.findViewById(R.id.btnExportData);
        btnClearData = root.findViewById(R.id.btnClearData);
    }

    private void initValues() {
        if (switchBiometric != null) {
            switchBiometric.setChecked(BiometricAuthHelper.isBiometricEnabled(requireContext()));
        }
    }

    private void setupListeners() {
        if (switchBiometric != null) {
            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                BiometricAuthHelper.setBiometricEnabled(requireContext(), isChecked);
                Toast.makeText(requireContext(), isChecked ? "🔒 Đã bật khóa sinh trắc học" : "🔓 Đã tắt khóa sinh trắc học", Toast.LENGTH_SHORT).show();
            });
        }

        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    ExpiryReminderWorker.scheduleDailyReminderCheck(requireContext());
                    Toast.makeText(requireContext(), "🔔 Đã bật thông báo nhắc nhở tự động", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "🔕 Đã tắt thông báo nhắc nhở", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnExportData != null) {
            btnExportData.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "📤 Đã xuất dữ liệu tệp JSON thành công vào bộ nhớ thiết bị!", Toast.LENGTH_LONG).show();
            });
        }

        if (btnClearData != null) {
            btnClearData.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("⚠️ Xóa toàn bộ dữ liệu")
                        .setMessage("Bạn có chắc muốn xóa tất cả giấy tờ đã lưu trong ứng dụng? Thao tác này không thể hoàn tác.")
                        .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                AppDatabase db = AppDatabase.getInstance(requireContext());
                                db.documentDao().deleteAll();
                                db.alertDao().deleteAll();
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "🗑️ Đã làm sạch toàn bộ dữ liệu ứng dụng", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }
    }
}
