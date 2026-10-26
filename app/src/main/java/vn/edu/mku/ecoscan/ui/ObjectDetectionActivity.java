package vn.edu.mku.ecoscan.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.detector.BoundingBox;
import vn.edu.mku.ecoscan.detector.Detector;

import java.io.IOException;
import java.util.List;

public class ObjectDetectionActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String MODEL_FILE = "yolov8n.tflite";
    private static final String LABEL_FILE = "labels.txt";

    private ImageView imageView;
    private OverlayView overlayView;
    private Detector detector;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    runDetection(bitmap);
                } else {
                    Toast.makeText(this, "Không chụp được ảnh", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detection);

        imageView = findViewById(R.id.imageView);
        overlayView = findViewById(R.id.overlayView);
        Button btnCapture = findViewById(R.id.btnCapture);

        requestCameraPermissionIfNeeded();
        initDetector();

        btnCapture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                overlayView.clear();
                cameraLauncher.launch(null);
            } else {
                requestCameraPermissionIfNeeded();
            }
        });
    }

    private void requestCameraPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void initDetector() {
        try {
            detector = new Detector(this, MODEL_FILE, LABEL_FILE);
        } catch (IOException e) {
            Toast.makeText(this, "Không tải được model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi tạo Detector: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void runDetection(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);

        if (detector == null) {
            Toast.makeText(this, "Detector chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        List<BoundingBox> results = detector.detect(bitmap);
        overlayView.setResults(results, bitmap.getWidth(), bitmap.getHeight());

        if (results.isEmpty()) {
            Toast.makeText(this, "Không phát hiện được vật thể nào", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Đã nhận diện được " + results.size() + " đồ vật", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) detector.close();
    }
}