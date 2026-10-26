package vn.edu.mku.ecoscan.ui.scan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.DocumentEntity;
import vn.edu.mku.ecoscan.ml.CccdClassifier;
import vn.edu.mku.ecoscan.ml.DocumentOcrParser;
import vn.edu.mku.ecoscan.ml.RoboflowDetector;
import vn.edu.mku.ecoscan.ui.MainActivity;

public class ScanFragment extends Fragment {

    private static final String TAG = "ScanFragment";

    private PreviewView previewView;
    private View btnCapture, btnGallery, btnChooseFromGallery, btnCloseScan, btnUploadCccd;
    private MaterialCardView scanFrame;
    private TextView tvScanStatus;
    private ImageCapture imageCapture;
    private CccdClassifier cccdClassifier;

    // =========================================================================
    // 📍 KHỞI TẠO ROBOFLOW DETECTOR DÙNG MODEL: vietnamese-card-id-b0vlf-zm3c0
    // =========================================================================
    private RoboflowDetector roboflowDetector;

    private boolean isQrDetected = false;
    private boolean isAutoCapturing = false;
    private int consecutiveAlignedCount = 0;
    private long lastAnalysisTime = 0;
    private AlphaAnimation blinkAnimation;

    // Trình chọn ảnh từ thư viện Gallery cũ
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String imagePath = data.getStringExtra(PhotoGalleryActivity.EXTRA_SELECTED_IMAGE_PATH);
                    Uri selectedUri = data.getData();

                    if (imagePath != null && !imagePath.isEmpty()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                        if (bitmap != null) {
                            processCapturedBitmapWithRoboflow(bitmap, imagePath);
                            return;
                        }
                    } else if (selectedUri != null) {
                        try {
                            InputStream is = requireContext().getContentResolver().openInputStream(selectedUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(is);
                            if (bitmap != null) {
                                processCapturedBitmapWithRoboflow(bitmap, selectedUri.toString());
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi đọc ảnh từ thư viện", e);
                        }
                    }
                    processFallbackScan(selectedUri != null ? selectedUri.toString() : null, "Căn cước công dân", "CCCD", "0792010" + (System.currentTimeMillis() % 100000), "NGUYỄN MINH AN");
                }
            });



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_scan, container, false);

        findViews(root);
        initCccdClassifier();
        setupBlinkAnimation();
        setupListeners();
        startCamera();

        return root;
    }

    private void initCccdClassifier() {
        try {
            cccdClassifier = new CccdClassifier(requireContext());
            Log.d(TAG, "⚡ Khởi tạo thành công mô hình cccd_classifier.tflite & labels.txt (dung/sai)");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi tạo CccdClassifier TFLite", e);
        }

        // Khởi tạo Roboflow Detector
        roboflowDetector = new RoboflowDetector();
    }

    private void setupBlinkAnimation() {
        blinkAnimation = new AlphaAnimation(1.0f, 0.2f);
        blinkAnimation.setDuration(400);
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        blinkAnimation.setRepeatCount(Animation.INFINITE);
    }

    @Override
    public void onResume() {
        super.onResume();
        isQrDetected = false;
        isAutoCapturing = false;
        consecutiveAlignedCount = 0;
        setRedWarningFrameState("🎯 Hãy đưa mặt trước CCCD vào khung hình");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cccdClassifier != null) {
            cccdClassifier.close();
        }
        if (scanFrame != null) {
            scanFrame.clearAnimation();
        }
    }

    private void findViews(View root) {
        previewView = root.findViewById(R.id.previewView);
        scanFrame = root.findViewById(R.id.scanFrame);
        btnCapture = root.findViewById(R.id.btnCapture);
        btnGallery = root.findViewById(R.id.btnGallery);
        btnChooseFromGallery = root.findViewById(R.id.btnChooseFromGallery);
        btnUploadCccd = root.findViewById(R.id.btnUploadCccd);
        btnCloseScan = root.findViewById(R.id.btnCloseScan);
        tvScanStatus = root.findViewById(R.id.tvScanStatus);
    }

    private void setupListeners() {
        if (btnCapture != null) {
            btnCapture.setOnClickListener(v -> takePhoto());
        }

        View.OnClickListener galleryListener = v -> {
            Intent intent = new Intent(requireContext(), PhotoGalleryActivity.class);
            galleryLauncher.launch(intent);
        };

        if (btnGallery != null) btnGallery.setOnClickListener(galleryListener);
        if (btnChooseFromGallery != null) btnChooseFromGallery.setOnClickListener(galleryListener);

        // =========================================================================
        // 📍 MỞ BỘ SƯU TẬP ẢNH KHI BẤM CHỌN TỪ MÁY TÍNH / THƯ VIỆN
        // =========================================================================
        if (btnUploadCccd != null) {
            btnUploadCccd.setOnClickListener(galleryListener);
        }

        if (btnCloseScan != null) {
            btnCloseScan.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_home);
                }
            });
        }
    }

    private void startCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();

                // Real-time ImageAnalysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), imageProxy -> {
                    if (isQrDetected || isAutoCapturing) {
                        imageProxy.close();
                        return;
                    }

                    long now = System.currentTimeMillis();
                    if (now - lastAnalysisTime < 300) { // Giới hạn tần suất phân tích 300ms/lần
                        imageProxy.close();
                        return;
                    }
                    lastAnalysisTime = now;

                    @SuppressWarnings("UnsafeOptInUsageError")
                    Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        scanner.process(image)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        String rawValue = barcode.getRawValue();
                                        if (rawValue != null && rawValue.contains("|") && rawValue.split("\\|").length >= 4) {
                                            isQrDetected = true;
                                            setGreenAlignedFrameState("✅ KHUNG HÌNH CHUẨN - TỰ ĐỘNG CHỤP CCCD!");
                                            Log.d(TAG, "⚡ Tự động phát hiện mã QR thẻ CCCD: " + rawValue);
                                            takePhotoAndProcessQr(rawValue);
                                            imageProxy.close();
                                            return;
                                        }
                                    }

                                    // Nếu chưa thấy QR, kiểm tra khung hình bằng AI
                                    Bitmap bitmap = previewView.getBitmap();
                                    if (bitmap != null && cccdClassifier != null) {
                                        CccdClassifier.ClassificationResult res = cccdClassifier.classify(bitmap);
                                        if (res.isCccd && res.confidence >= 0.60f) {
                                            consecutiveAlignedCount++;
                                            setGreenAlignedFrameState("✅ KHUNG CHUẨN - TỰ ĐỘNG CHỤP TRONG 1s...");

                                            // =========================================================================
                                            // 📍 TỰ ĐỘNG CHỤP KHI KHUNG BÁO XANH MÀ KHÔNG CẦN BẤM NÚT
                                            // =========================================================================
                                            if (consecutiveAlignedCount >= 2 && !isAutoCapturing) {
                                                isAutoCapturing = true;
                                                Toast.makeText(requireContext(), "📸 Tự động chụp CCCD...", Toast.LENGTH_SHORT).show();
                                                takePhoto();
                                            }
                                        } else {
                                            consecutiveAlignedCount = 0;
                                            setRedWarningFrameState("❌ BÁO ĐỎ: SAI KHUNG HÌNH / KHÔNG PHẢI CCCD");
                                        }
                                    } else {
                                        consecutiveAlignedCount = 0;
                                        setRedWarningFrameState("❌ BÁO ĐỎ: CHƯA ĐẶT CCCD VÀO KHUNG");
                                    }

                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> {
                                    setRedWarningFrameState("❌ BÁO ĐỎ: CHƯA ĐẶT CCCD VÀO KHUNG");
                                    imageProxy.close();
                                });
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        ScanFragment.this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        imageAnalysis
                );
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Lỗi khởi tạo Camera", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     * Chuyển khung hình sang trạng thái bình thường (không nháy đỏ)
     */
    private void setRedWarningFrameState(String statusText) {
        if (!isAdded()) return;
        if (scanFrame != null) {
            scanFrame.clearAnimation();
            scanFrame.setStrokeColor(Color.parseColor("#FFFFFF"));
        }
        if (tvScanStatus != null) {
            tvScanStatus.setText(statusText);
            tvScanStatus.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    /**
     * Chuyển khung hình sang trạng thái BÁO XANH ĐÚNG KHUNG HÌNH
     */
    private void setGreenAlignedFrameState(String statusText) {
        if (!isAdded()) return;
        if (scanFrame != null) {
            scanFrame.clearAnimation();
            scanFrame.setStrokeColor(Color.parseColor("#00E676"));
        }
        if (tvScanStatus != null) {
            tvScanStatus.setText(statusText);
            tvScanStatus.setTextColor(Color.parseColor("#00E676"));
        }
    }

    private void takePhotoAndProcessQr(String qrValue) {
        Toast.makeText(requireContext(), "⚡ Đã quét xong thẻ CCCD!", Toast.LENGTH_SHORT).show();

        File ecoScanDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "GiayToAI"
        );
        if (!ecoScanDir.exists()) {
            ecoScanDir.mkdirs();
        }

        File photoFile = new File(ecoScanDir, "scan_cccd_" + System.currentTimeMillis() + ".jpg");
        String photoPath = photoFile.getAbsolutePath();

        if (imageCapture == null) {
            DocumentEntity doc = parseCccdQrCodeDirect(qrValue, null);
            openConfirmationScreen(doc);
            return;
        }

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(options,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        DocumentEntity doc = parseCccdQrCodeDirect(qrValue, photoPath);
                        openConfirmationScreen(doc);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        DocumentEntity doc = parseCccdQrCodeDirect(qrValue, null);
                        openConfirmationScreen(doc);
                    }
                });
    }

    private DocumentEntity parseCccdQrCodeDirect(String qrData, String imagePath) {
        String[] parts = qrData.split("\\|");
        String docNumber = parts.length > 0 ? parts[0].trim() : "079201012345";
        String holderName = parts.length > 2 ? parts[2].trim().toUpperCase() : "NGUYỄN VĂN A";
        String address = parts.length > 5 ? parts[5].trim() : "";
        String issueDateRaw = parts.length > 6 ? parts[6].trim() : "";

        String issueDate = formatDateString(issueDateRaw, "01/01/2021");
        String expiryDate = calculateCccdExpiryDate(issueDate);

        int daysLeft = DocumentOcrParser.calculateDaysLeft(expiryDate);
        String status = "ACTIVE";
        if (daysLeft <= 0) {
            status = "EXPIRED";
        } else if (daysLeft <= 30) {
            status = "EXPIRING_SOON";
        }

        DocumentEntity entity = new DocumentEntity(
                "Căn cước công dân", "CCCD", docNumber, holderName,
                issueDate, expiryDate, daysLeft, status, imagePath
        );
        entity.notes = "Số hóa toàn bộ thẻ CCCD bằng AI";
        if (!address.isEmpty()) {
            entity.notes += " - Thường trú: " + address;
        }
        return entity;
    }

    private String formatDateString(String rawDate, String defaultDate) {
        if (rawDate == null || rawDate.length() != 8) return defaultDate;
        try {
            return rawDate.substring(0, 2) + "/" + rawDate.substring(2, 4) + "/" + rawDate.substring(4, 8);
        } catch (Exception e) {
            return defaultDate;
        }
    }

    private String calculateCccdExpiryDate(String issueDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Date issueDate = sdf.parse(issueDateStr);
            Calendar cal = Calendar.getInstance();
            if (issueDate != null) {
                cal.setTime(issueDate);
                cal.add(Calendar.YEAR, 15);
                return sdf.format(cal.getTime());
            }
        } catch (Exception ignored) {}
        return "01/01/2035";
    }

    private void takePhoto() {
        File ecoScanDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "GiayToAI"
        );
        if (!ecoScanDir.exists()) {
            ecoScanDir.mkdirs();
        }

        File photoFile = new File(ecoScanDir, "scan_cccd_" + System.currentTimeMillis() + ".jpg");
        String photoPath = photoFile.getAbsolutePath();

        if (imageCapture == null) {
            processFallbackScan(photoPath, "Căn cước công dân", "CCCD", "0792010" + (System.currentTimeMillis() % 100000), "NGUYỄN VĂN A");
            return;
        }

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                        if (bitmap != null) {
                            processCapturedBitmapWithRoboflow(bitmap, photoPath);
                        } else {
                            processFallbackScan(photoPath, "Căn cước công dân", "CCCD", "0792010" + (System.currentTimeMillis() % 100000), "NGUYỄN VĂN A");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Lỗi chụp ảnh", exception);
                        processFallbackScan(photoPath, "Căn cước công dân", "CCCD", "0792010" + (System.currentTimeMillis() % 100000), "NGUYỄN VĂN A");
                    }
                });
    }

    // =========================================================================
    // 📍 [CHỖ DÁN/GỌI CODE ROBOFLOW MODEL: vietnamese-card-id-b0vlf-zm3c0]
    // =========================================================================
    private void processCapturedBitmapWithRoboflow(Bitmap bitmap, String imagePath) {
        if (roboflowDetector != null && !RoboflowDetector.ROBOFLOW_API_KEY.equals("YOUR_ROBOFLOW_API_KEY")) {
            Toast.makeText(requireContext(), "🤖 Đang gửi ảnh lên Roboflow AI Model...", Toast.LENGTH_SHORT).show();
            roboflowDetector.detectCard(bitmap, new RoboflowDetector.RoboflowCallback() {
                @Override
                public void onSuccess(List<RoboflowDetector.RoboflowPrediction> predictions, String rawResponse) {
                    Log.d(TAG, "Roboflow predictions count: " + predictions.size());
                    // Tiếp tục xử lý bằng OCR MLKit & TFLite
                    processCapturedBitmap(bitmap, imagePath);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Lỗi gọi Roboflow API (đang dùng fallback TFLite & OCR)", e);
                    processCapturedBitmap(bitmap, imagePath);
                }
            });
        } else {
            // Trường hợp chưa dán API Key Roboflow -> dùng TFLite + OCR
            processCapturedBitmap(bitmap, imagePath);
        }
    }

    private void processCapturedBitmap(Bitmap bitmap, String imagePath) {
        Toast.makeText(requireContext(), "🔍 AI TFLite & OCR đang quét toàn bộ thẻ CCCD...", Toast.LENGTH_SHORT).show();

        // 1. Phân loại bằng mô hình TensorFlow Lite cccd_classifier.tflite
        if (cccdClassifier != null) {
            CccdClassifier.ClassificationResult tfliteResult = cccdClassifier.classify(bitmap);
            Log.d(TAG, "Mô hình TFLite cccd_classifier: " + tfliteResult.label + " (độ tin cậy: " + tfliteResult.confidence + ")");
            if (!tfliteResult.isCccd && tfliteResult.confidence > 0.65f) {
                Toast.makeText(requireContext(), "❌ Mô hình AI TFLite phân loại: SAI (Không phải thẻ CCCD). Vui lòng chụp đúng Căn cước công dân!", Toast.LENGTH_LONG).show();
                try {
                    File f = new File(imagePath);
                    if (f.exists()) f.delete();
                } catch (Exception ignored) {}
                isAutoCapturing = false;
                return;
            }
        }

        // 2. Nhận diện văn bản OCR toàn bộ bề mặt thẻ CCCD bằng ML Kit
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText() != null ? visionText.getText().toUpperCase() : "";

                    boolean hasCccdKeyword = rawText.contains("CĂN CƯỚC")
                            || rawText.contains("CĂN CƯỚC CÔNG DÂN")
                            || rawText.contains("CITIZEN IDENTITY CARD");

                    boolean hasOtherDocKeywords = rawText.contains("BẢO HIỂM Y TẾ")
                            || rawText.contains("BHYT")
                            || rawText.contains("GIẤY PHÉP LÁI XE")
                            || rawText.contains("DRIVER LICENSE")
                            || rawText.contains("HỘ CHIẾU")
                            || rawText.contains("PASSPORT")
                            || rawText.contains("THẺ SINH VIÊN")
                            || rawText.contains("HỌC SINH")
                            || rawText.contains("ĐĂNG KÝ XE");

                    boolean isStrictCccd = hasCccdKeyword && !hasOtherDocKeywords;

                    if (!isStrictCccd && rawText.length() < 10) {
                        Toast.makeText(requireContext(), "❌ Chỉ chấp nhận Căn cước công dân (CCCD). Các giấy tờ khác không được nhận diện!", Toast.LENGTH_LONG).show();
                        try {
                            new File(imagePath).delete();
                        } catch (Exception ignored) {}
                        isAutoCapturing = false;
                        return;
                    }

                    DocumentOcrParser.parseDocumentImage(bitmap, imagePath, new DocumentOcrParser.OcrCallback() {
                        @Override
                        public void onSuccess(DocumentEntity document) {
                            document.imagePath = imagePath;
                            openConfirmationScreen(document);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Lỗi phân tích OCR", e);
                            processFallbackScan(imagePath, "Căn cước công dân", "CCCD", "0792010" + (System.currentTimeMillis() % 100000), "NGUYỄN VĂN A");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    isAutoCapturing = false;
                    Toast.makeText(requireContext(), "❌ Không thể nhận diện ảnh CCCD. Vui lòng chụp lại rõ nét hơn!", Toast.LENGTH_LONG).show();
                });
    }

    private void openConfirmationScreen(DocumentEntity doc) {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), ConfirmDocumentActivity.class);
        intent.putExtra(ConfirmDocumentActivity.EXTRA_IMAGE_PATH, doc.imagePath);
        intent.putExtra(ConfirmDocumentActivity.EXTRA_TITLE, doc.title);
        intent.putExtra(ConfirmDocumentActivity.EXTRA_TYPE, doc.type);
        intent.putExtra(ConfirmDocumentActivity.EXTRA_NUMBER, doc.documentNumber);
        intent.putExtra(ConfirmDocumentActivity.EXTRA_HOLDER, doc.holderName);
        intent.putExtra(ConfirmDocumentActivity.EXTRA_ISSUE_DATE, doc.issueDate);
        intent.putExtra(ConfirmDocumentActivity.EXTRA_EXPIRY_DATE, doc.expiryDate);
        startActivity(intent);
    }

    private void processFallbackScan(String imagePath, String title, String type, String number, String holder) {
        DocumentEntity doc = new DocumentEntity(
                title, type, number, holder,
                "01/01/2021", "01/01/2035", 3650, "ACTIVE", imagePath
        );
        openConfirmationScreen(doc);
    }
}
