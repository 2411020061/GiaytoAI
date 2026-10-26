package vn.edu.mku.ecoscan.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    List<DocumentEntity> getAll();

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    DocumentEntity getById(int id);

    @Query("SELECT * FROM documents WHERE status = 'EXPIRING_SOON' OR daysLeft <= 30 ORDER BY daysLeft ASC")
    List<DocumentEntity> getExpiringSoon();

    @Query("SELECT COUNT(*) FROM documents")
    int getCount();

    @Query("SELECT COUNT(*) FROM documents WHERE status = 'ACTIVE' OR daysLeft > 30")
    int getActiveCount();

    @Insert
    long insert(DocumentEntity document);

    @Update
    void update(DocumentEntity document);

    @Query("DELETE FROM documents WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM documents")
    void deleteAll();
}
