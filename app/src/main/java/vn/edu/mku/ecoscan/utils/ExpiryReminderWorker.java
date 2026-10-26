package vn.edu.mku.ecoscan.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.TimeUnit;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AlertEntity;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.data.DocumentEntity;
import vn.edu.mku.ecoscan.ui.MainActivity;

public class ExpiryReminderWorker extends Worker {

    private static final String TAG = "ExpiryReminderWorker";
    public static final String CHANNEL_ID = "doc_expiry_notifications";
    private static final String WORK_NAME = "expiry_reminder_work";

    public ExpiryReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Kiểm tra tự động danh sách giấy tờ sắp hết hạn...");

        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<DocumentEntity> expiringDocs = db.documentDao().getExpiringSoon();

            if (expiringDocs != null && !expiringDocs.isEmpty()) {
                for (DocumentEntity doc : expiringDocs) {
                    // Cập nhật hoặc kiểm tra trạng thái
                    String notifyTitle = "🚨 Giấy tờ sắp hết hạn: " + doc.title;
                    String notifyMsg = doc.holderName + " - Mã số " + doc.documentNumber + " chỉ còn " + doc.daysLeft + " ngày (Hết hạn: " + doc.expiryDate + ")";

                    // Tạo cảnh báo trong DB nếu chưa đọc
                    db.alertDao().insert(new AlertEntity(
                            doc.title + " sắp hết hạn",
                            notifyMsg,
                            "HIGH",
                            "Nhắc nhở tự động",
                            doc.id
                    ));

                    // Phát thông báo đẩy trên thanh thông báo Android
                    sendNotification(getApplicationContext(), notifyTitle, notifyMsg);
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi thực thi WorkManager nhắc nhở giấy tờ", e);
            return Result.retry();
        }
    }

    public static void scheduleDailyReminderCheck(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ExpiryReminderWorker.class, 24, TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    public static void sendNotification(Context context, String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) return;

        // Tạo Notification Channel nếu từ Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở hết hạn Giấy tờ",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi Căn cước công dân sắp hết hạn");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        int notificationId = (int) (System.currentTimeMillis() % 100000);
        notificationManager.notify(notificationId, builder.build());
    }
}
