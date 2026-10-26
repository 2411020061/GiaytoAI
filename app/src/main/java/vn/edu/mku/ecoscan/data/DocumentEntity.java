package vn.edu.mku.ecoscan.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class DocumentEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;          // e.g. "Hộ chiếu Việt Nam", "Căn cước công dân", "Giấy phép lái xe B2", "Bảo hiểm y tế"
    public String type;           // e.g. "CCCD", "HỘ CHIẾU", "BẰNG LÁI", "BẢO HIỂM"
    public String documentNumber; // e.g. "B8391729", "079201012345"
    public String holderName;     // e.g. "NGUYỄN MINH AN"
    public String issueDate;      // e.g. "28/10/2016"
    public String expiryDate;     // e.g. "28/10/2026"
    public int daysLeft;          // e.g. 28 days
    public String status;         // "ACTIVE", "EXPIRING_SOON", "EXPIRED"
    public String imagePath;
    public String notes;
    public long createdAt;

    public DocumentEntity() {}

    public DocumentEntity(String title, String type, String documentNumber, String holderName,
                          String issueDate, String expiryDate, int daysLeft, String status, String imagePath) {
        this.title = title;
        this.type = type;
        this.documentNumber = documentNumber;
        this.holderName = holderName;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.daysLeft = daysLeft;
        this.status = status;
        this.imagePath = imagePath;
        this.createdAt = System.currentTimeMillis();
    }
}
