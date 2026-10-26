package vn.edu.mku.ecoscan.ui.scan;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AlertEntity;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.data.DocumentEntity;
import vn.edu.mku.ecoscan.ml.DocumentOcrParser;
import vn.edu.mku.ecoscan.ui.detail.DocumentDetailActivity;
import vn.edu.mku.ecoscan.utils.ExpiryReminderWorker;

public class ConfirmDocumentActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_NUMBER = "extra_number";
    public static final String EXTRA_HOLDER = "extra_holder";
    public static final String EXTRA_ISSUE_DATE = "extra_issue_date";
    public static final String EXTRA_EXPIRY_DATE = "extra_expiry_date";

    private ImageView imgDocPreview;
    private Spinner spinnerDocType;
    private EditText etHolderName, etDocNumber, etIssueDate, etExpiryDate;
    private View btnBackConfirm, btnRetake, btnChooseComputer, btnSaveConfirm;

    private String imagePath;

    private final ActivityResultLauncher<String> fileUploadLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if (bitmap != null) {
                            imagePath = uri.toString();
                            Picasso.get().load(uri).into(imgDocPreview);
                            Toast.makeText(this, "📁 Đã chọn ảnh mới từ máy tính! Đang quét lại OCR...", Toast.LENGTH_SHORT).show();

                            DocumentOcrParser.parseDocumentImage(bitmap, imagePath, new DocumentOcrParser.OcrCallback() {
                                @Override
                                public void onSuccess(DocumentEntity doc) {
                                    if (doc != null) {
                                        if (doc.holderName != null && !doc.holderName.isEmpty()) {
                                            etHolderName.setText(doc.holderName);
                                        }
                                        if (doc.documentNumber != null && !doc.documentNumber.isEmpty()) {
                                            etDocNumber.setText(doc.documentNumber);
                                        }
                                        if (doc.issueDate != null && !doc.issueDate.isEmpty()) {
                                            etIssueDate.setText(doc.issueDate);
                                        }
                                        if (doc.expiryDate != null && !doc.expiryDate.isEmpty()) {
                                            etExpiryDate.setText(doc.expiryDate);
                                        }
                                    }
                                    Toast.makeText(ConfirmDocumentActivity.this, "✨ Quét OCR ảnh mới thành công!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Log.e("ConfirmDocumentActivity", "Lỗi OCR ảnh mới", e);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("ConfirmDocumentActivity", "Lỗi đọc file từ máy tính", e);
                        Toast.makeText(this, "❌ Không thể mở file ảnh!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    private final String[] docTypeOptions = {
            "Căn cước công dân (CCCD)"
    };

    private final String[] docTypeCodes = {
            "CCCD"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_document);

        findViews();
        setupSpinner();
        loadExtractedData();
        setupListeners();
    }

    private void findViews() {
        imgDocPreview = findViewById(R.id.imgDocPreview);
        spinnerDocType = findViewById(R.id.spinnerDocType);
        etHolderName = findViewById(R.id.etHolderName);
        etDocNumber = findViewById(R.id.etDocNumber);
        etIssueDate = findViewById(R.id.etIssueDate);
        etExpiryDate = findViewById(R.id.etExpiryDate);

        btnBackConfirm = findViewById(R.id.btnBackConfirm);
        btnRetake = findViewById(R.id.btnRetake);
        btnChooseComputer = findViewById(R.id.btnChooseComputer);
        btnSaveConfirm = findViewById(R.id.btnSaveConfirm);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                docTypeOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDocType.setAdapter(adapter);
        spinnerDocType.setEnabled(false); // Chỉ quản lý CCCD
    }

    private void loadExtractedData() {
        Intent intent = getIntent();
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH);
        String number = intent.getStringExtra(EXTRA_NUMBER);
        String holder = intent.getStringExtra(EXTRA_HOLDER);
        String issueDate = intent.getStringExtra(EXTRA_ISSUE_DATE);
        String expiryDate = intent.getStringExtra(EXTRA_EXPIRY_DATE);

        // Load image preview
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Picasso.get().load(imgFile).into(imgDocPreview);
            }
        }

        // Set form values
        etHolderName.setText(holder != null && !holder.isEmpty() ? holder : "NGUYỄN VĂN A");
        etDocNumber.setText(number != null && !number.isEmpty() ? number : "");
        etIssueDate.setText(issueDate != null && !issueDate.isEmpty() ? issueDate : "01/01/2021");
        etExpiryDate.setText(expiryDate != null && !expiryDate.isEmpty() ? expiryDate : "01/01/2035");

        spinnerDocType.setSelection(0);
    }

    private void setupListeners() {
        if (btnBackConfirm != null) btnBackConfirm.setOnClickListener(v -> finish());
        if (btnRetake != null) btnRetake.setOnClickListener(v -> finish());
        if (btnChooseComputer != null) {
            btnChooseComputer.setOnClickListener(v -> fileUploadLauncher.launch("image/*"));
        }

        etIssueDate.setOnClickListener(v -> showDatePicker(etIssueDate));
        etExpiryDate.setOnClickListener(v -> showDatePicker(etExpiryDate));

        if (btnSaveConfirm != null) {
            btnSaveConfirm.setOnClickListener(v -> saveVerifiedDocument());
        }
    }

    private void showDatePicker(EditText editText) {
        Calendar cal = Calendar.getInstance();
        try {
            String currentText = editText.getText().toString();
            if (!currentText.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                cal.setTime(sdf.parse(currentText));
            }
        } catch (Exception ignored) {}

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dateStr = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    editText.setText(dateStr);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void saveVerifiedDocument() {
        String holderName = etHolderName.getText().toString().trim();
        String docNumber = etDocNumber.getText().toString().trim();
        String issueDate = etIssueDate.getText().toString().trim();
        String expiryDate = etExpiryDate.getText().toString().trim();

        if (holderName.isEmpty()) {
            etHolderName.setError("Vui lòng nhập Họ tên");
            return;
        }
        if (docNumber.isEmpty()) {
            etDocNumber.setError("Vui lòng nhập số CCCD (12 số)");
            return;
        }

        String typeCode = "CCCD";
        String title = "Căn cước công dân";

        int daysLeft = DocumentOcrParser.calculateDaysLeft(expiryDate);
        String calculatedStatus = "ACTIVE";
        if (daysLeft <= 0) {
            calculatedStatus = "EXPIRED";
        } else if (daysLeft <= 30) {
            calculatedStatus = "EXPIRING_SOON";
        }

        final String status = calculatedStatus;

        DocumentEntity doc = new DocumentEntity(
                title, typeCode, docNumber, holderName.toUpperCase(),
                issueDate, expiryDate, daysLeft, status, imagePath
        );
        doc.notes = "CCCD số hóa tự động từ Camera OCR";

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            long newId = db.documentDao().insert(doc);

            if ("EXPIRING_SOON".equals(status) || daysLeft <= 30) {
                db.alertDao().insert(new AlertEntity(
                        title + " sắp hết hạn",
                        "CCCD của " + holderName + " còn " + daysLeft + " ngày là hết hạn. Vui lòng đi cấp đổi.",
                        "HIGH",
                        "Vừa xong",
                        (int) newId
                ));
                ExpiryReminderWorker.sendNotification(
                        this,
                        title,
                        "CCCD của " + holderName + " sắp hết hạn vào ngày " + expiryDate + ". Hãy kiểm tra ngay!"
                );
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "⚡ Đã xác nhận & lưu CCCD thành công!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, DocumentDetailActivity.class);
                intent.putExtra("document_id", (int) newId);
                startActivity(intent);
                finish();
            });
        });
    }
}
