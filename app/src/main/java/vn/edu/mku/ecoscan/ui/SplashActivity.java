package vn.edu.mku.ecoscan.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.utils.BiometricAuthHelper;
import vn.edu.mku.ecoscan.utils.ExpiryReminderWorker;

public class SplashActivity extends AppCompatActivity {
    private final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Đặt lịch tự động kiểm tra giấy tờ hết hạn
        ExpiryReminderWorker.scheduleDailyReminderCheck(this);

        View btnStart = findViewById(R.id.btnStart);
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> startMain());
        }

        View btnSkip = findViewById(R.id.btnSkip);
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> startMain());
        }
    }

    private void checkPermissionsAndAuthenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CODE);
                return;
            }
        }

        performBiometricAuth();
    }

    private void performBiometricAuth() {
        if (BiometricAuthHelper.isBiometricEnabled(this) && BiometricAuthHelper.isBiometricAvailable(this)) {
            BiometricAuthHelper.authenticate(this, new BiometricAuthHelper.BiometricCallback() {
                @Override
                public void onSuccess() {
                    startMain();
                }

                @Override
                public void onError(String errorMsg) {
                    Toast.makeText(SplashActivity.this, "🔒 Chưa xác thực: " + errorMsg, Toast.LENGTH_SHORT).show();
                    // Cho phép tiếp tục nếu bấm hủy / nhập mật khẩu máy
                    startMain();
                }

                @Override
                public void onCancelled() {
                    Toast.makeText(SplashActivity.this, "Đã hủy xác thực sinh trắc học", Toast.LENGTH_SHORT).show();
                    startMain();
                }
            });
        } else {
            startMain();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performBiometricAuth();
            } else {
                startMain();
            }
        }
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
