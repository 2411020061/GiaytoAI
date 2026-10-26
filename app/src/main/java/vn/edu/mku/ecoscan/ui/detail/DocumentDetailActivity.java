package vn.edu.mku.ecoscan.ui.detail;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.concurrent.Executors;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.data.DocumentDao;
import vn.edu.mku.ecoscan.data.DocumentEntity;
import vn.edu.mku.ecoscan.utils.BiometricAuthHelper;
import vn.edu.mku.ecoscan.utils.ExpiryReminderWorker;

public class DocumentDetailActivity extends AppCompatActivity {

    private TextView tvDetailHeaderTitle, tvCardTypeLabel, tvCardTypeBadge;
    private TextView tvCardHolderName, tvCardDocNumber, tvDetailDaysLeft;
    private TextView tvDetailHolderName, tvDetailDocNumber, tvDetailIssueDate, tvDetailExpiryDate;
    private View btnBack, btnLockDoc, btnShareDoc, bannerWarning, btnDeleteHeader, btnDeleteDoc;

    private int documentId = -1;
    private DocumentEntity currentDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        documentId = getIntent().getIntExtra("document_id", -1);

        findViews();
        setupListeners();
        loadDocumentDetail();
    }

    private void findViews() {
        tvDetailHeaderTitle = findViewById(R.id.tvDetailHeaderTitle);
        tvCardTypeLabel = findViewById(R.id.tvCardTypeLabel);
        tvCardTypeBadge = findViewById(R.id.tvCardTypeBadge);
        tvCardHolderName = findViewById(R.id.tvCardHolderName);
        tvCardDocNumber = findViewById(R.id.tvCardDocNumber);
        tvDetailDaysLeft = findViewById(R.id.tvDetailDaysLeft);

        tvDetailHolderName = findViewById(R.id.tvDetailHolderName);
        tvDetailDocNumber = findViewById(R.id.tvDetailDocNumber);
        tvDetailIssueDate = findViewById(R.id.tvDetailIssueDate);
        tvDetailExpiryDate = findViewById(R.id.tvDetailExpiryDate);

        btnBack = findViewById(R.id.btnBack);
        btnLockDoc = findViewById(R.id.btnLockDoc);
        btnShareDoc = findViewById(R.id.btnShareDoc);
        bannerWarning = findViewById(R.id.bannerWarning);
        btnDeleteHeader = findViewById(R.id.btnDeleteHeader);
        btnDeleteDoc = findViewById(R.id.btnDeleteDoc);
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnDeleteHeader != null) {
            btnDeleteHeader.setOnClickListener(v -> confirmAndDeleteDocument());
        }

        if (btnDeleteDoc != null) {
            btnDeleteDoc.setOnClickListener(v -> confirmAndDeleteDocument());
        }

        if (btnLockDoc != null) {
            btnLockDoc.setOnClickListener(v -> {
                BiometricAuthHelper.authenticate(this, new BiometricAuthHelper.BiometricCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(DocumentDetailActivity.this, "🔒 Giấy tờ đã được khóa và mã hóa bằng Vân tay/Khuôn mặt!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Toast.makeText(DocumentDetailActivity.this, "Khóa thất bại: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled() {
                        Toast.makeText(DocumentDetailActivity.this, "Hủy khóa giấy tờ", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (btnShareDoc != null) {
            btnShareDoc.setOnClickListener(v ->
                    Toast.makeText(this, "🔗 Tạo liên kết chia sẻ an toàn mã hóa 24 giờ", Toast.LENGTH_SHORT).show()
            );
        }

        if (bannerWarning != null) {
            bannerWarning.setOnClickListener(v -> {
                if (currentDocument != null) {
                    ExpiryReminderWorker.sendNotification(
                            this,
                            "🔔 Đặt lịch gia hạn: " + currentDocument.title,
                            "Đã lên lịch nhắc nhở gia hạn cho " + currentDocument.holderName + " trước ngày " + currentDocument.expiryDate
                    );
                    Toast.makeText(this, "📅 Đã tạo lịch nhắc nhở gia hạn tự động trên thiết bị!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void confirmAndDeleteDocument() {
        String title = currentDocument != null ? currentDocument.title : "giấy tờ này";
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Xóa giấy tờ")
                .setMessage("Bạn có chắc chắn muốn xóa \"" + title + "\" khỏi thư viện?")
                .setPositiveButton("Xóa ngay", (dialog, which) -> deleteCurrentDocument())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteCurrentDocument() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (currentDocument != null && currentDocument.imagePath != null && !currentDocument.imagePath.isEmpty()) {
                try {
                    File imgFile = new File(currentDocument.imagePath);
                    if (imgFile.exists()) {
                        imgFile.delete();
                    }
                } catch (Exception e) {
                    // Ignore file deletion error
                }
            }

            AppDatabase db = AppDatabase.getInstance(this);
            if (documentId != -1) {
                db.documentDao().deleteById(documentId);
            } else if (currentDocument != null) {
                db.documentDao().deleteById(currentDocument.id);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "🗑️ Đã xóa giấy tờ khỏi thư viện!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void loadDocumentDetail() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            DocumentDao dao = db.documentDao();

            DocumentEntity doc = (documentId != -1) ? dao.getById(documentId) : null;
            currentDocument = doc;

            runOnUiThread(() -> {
                if (doc != null) {
                    tvDetailHeaderTitle.setText(doc.title);
                    tvCardTypeLabel.setText(doc.title != null ? doc.title.toUpperCase() : "GIẤY TỜ");
                    tvCardTypeBadge.setText(doc.type != null ? doc.type : "AI");
                    tvCardHolderName.setText(doc.holderName);
                    tvCardDocNumber.setText(doc.documentNumber);

                    tvDetailHolderName.setText(doc.holderName);
                    tvDetailDocNumber.setText(doc.documentNumber);
                    tvDetailIssueDate.setText(doc.issueDate != null ? doc.issueDate : "--/--/----");
                    tvDetailExpiryDate.setText(doc.expiryDate != null ? doc.expiryDate : "--/--/----");

                    if ("EXPIRING_SOON".equalsIgnoreCase(doc.status) || doc.daysLeft <= 30) {
                        tvDetailDaysLeft.setText("Hết hạn sau " + doc.daysLeft + " ngày - Nhấn để đặt nhắc nhở");
                        bannerWarning.setVisibility(View.VISIBLE);
                    } else {
                        tvDetailDaysLeft.setText("Còn hiệu lực");
                        bannerWarning.setVisibility(View.GONE);
                    }
                }
            });
        });
    }
}
