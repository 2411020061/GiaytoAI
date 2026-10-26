package vn.edu.mku.ecoscan.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scan_results")
public class ScanResult {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String label;
    public float confidence;
    public boolean recyclable;
    public String imagePath;
    public long timestamp;
}
