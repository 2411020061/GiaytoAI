package vn.edu.mku.ecoscan.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts")
public class AlertEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;       // e.g. "Hộ chiếu sắp hết hạn"
    public String description; // e.g. "Thời gian cấp mới thường mất 8-10 ngày làm việc."
    public String priority;    // "HIGH", "CHECK", "INFO"
    public String timeText;    // e.g. "08:20 hôm nay", "hôm qua"
    public boolean isRead;
    public int documentId;

    public AlertEntity() {}

    public AlertEntity(String title, String description, String priority, String timeText, int documentId) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.timeText = timeText;
        this.documentId = documentId;
        this.isRead = false;
    }
}
