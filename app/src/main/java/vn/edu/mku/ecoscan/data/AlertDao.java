package vn.edu.mku.ecoscan.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY id DESC")
    List<AlertEntity> getAll();

    @Query("SELECT COUNT(*) FROM alerts WHERE isRead = 0")
    int getUnreadCount();

    @Insert
    long insert(AlertEntity alert);

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    void markAsRead(int id);

    @Query("DELETE FROM alerts")
    void deleteAll();
}
