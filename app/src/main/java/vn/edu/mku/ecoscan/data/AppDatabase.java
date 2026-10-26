package vn.edu.mku.ecoscan.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executors;

@Database(entities = {DocumentEntity.class, AlertEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DocumentDao documentDao();
    public abstract AlertDao alertDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "docai_guard.db"
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateData(INSTANCE);
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void prepopulateData(AppDatabase db) {
        DocumentDao docDao = db.documentDao();
        AlertDao alertDao = db.alertDao();

        if (docDao.getCount() == 0) {
            docDao.insert(new DocumentEntity(
                    "Căn cước công dân", "CCCD", "079201012345", "NGUYỄN MINH AN",
                    "01/01/2021", "20/05/2030", 1800, "ACTIVE", null
            ));

            int cccdExpId = (int) docDao.insert(new DocumentEntity(
                    "Căn cước công dân", "CCCD", "079202098765", "TRẦN THỊ HÒA",
                    "15/10/2016", "15/10/2026", 28, "EXPIRING_SOON", null
            ));

            alertDao.insert(new AlertEntity(
                    "CCCD sắp hết hạn",
                    "CCCD của TRẦN THỊ HÒA còn 28 ngày là hết hạn. Vui lòng đi làm thủ tục cấp đổi.",
                    "HIGH",
                    "Hôm nay",
                    cccdExpId
            ));

            alertDao.insert(new AlertEntity(
                    "Bảo mật sinh trắc học đã bật",
                    "Khóa vân tay/khuôn mặt đã sẵn sàng bảo mật toàn bộ Căn cước công dân.",
                    "INFO",
                    "Vừa xong",
                    0
            ));
        }
    }
}
