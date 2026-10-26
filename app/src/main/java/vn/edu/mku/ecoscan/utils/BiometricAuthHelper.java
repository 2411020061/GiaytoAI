package vn.edu.mku.ecoscan.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricAuthHelper {

    private static final String TAG = "BiometricAuthHelper";
    private static final String PREF_NAME = "doc_guard_prefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    public interface BiometricCallback {
        void onSuccess();
        void onError(String errorMsg);
        void onCancelled();
    }

    public static boolean isBiometricEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true);
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticate(FragmentActivity activity, BiometricCallback callback) {
        if (!isBiometricAvailable(activity)) {
            Log.w(TAG, "Cảm biến sinh trắc học không khả dụng hoặc chưa cài đặt");
            // Mở thẳng nếu thiết bị không hỗ trợ sinh trắc học (hoặc giả lập)
            callback.onSuccess();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "Lỗi xác thực: " + errString);
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            callback.onCancelled();
                        } else {
                            callback.onError(errString.toString());
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Log.d(TAG, "Xác thực vân tay/khuôn mặt thành công!");
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.w(TAG, "Xác thực không chính xác, thử lại");
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực Vân tay / Khuôn mặt")
                .setSubtitle("Bảo mật truy cập ứng dụng & Kho giấy tờ cá nhân")
                .setDescription("Chạm vào cảm biến vân tay hoặc nhìn vào ống kính để tiếp tục")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                                | BiometricManager.Authenticators.BIOMETRIC_WEAK
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
