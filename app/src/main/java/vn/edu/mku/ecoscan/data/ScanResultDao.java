package vn.edu.mku.ecoscan.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ScanResultDao {

    @Insert
    void insert(ScanResult result);

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    List<ScanResult> getAll();

    @Query("DELETE FROM scan_results WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT label, COUNT(*) as count FROM scan_results GROUP BY label")
    List<LabelCount> getStatsByLabel();

    class LabelCount {
        public String label;
        public int count;
    }
}
