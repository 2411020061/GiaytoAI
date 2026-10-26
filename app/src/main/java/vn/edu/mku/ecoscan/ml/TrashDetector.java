package vn.edu.mku.ecoscan.ml;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.List;

public class TrashDetector {

    private final ObjectDetector detector;

    public TrashDetector(Context context) throws IOException {
        ObjectDetector.ObjectDetectorOptions options =
                ObjectDetector.ObjectDetectorOptions.builder()
                        .setMaxResults(8)
                        .setScoreThreshold(0.5f)
                        .build();
        detector = ObjectDetector.createFromFileAndOptions(context, "ssd_mobilenet_v1.tflite", options);
    }

    public List<Detection> detect(Bitmap bitmap) {
        return detector.detect(TensorImage.fromBitmap(bitmap));
    }

    public void close() {
        detector.close();
    }
}
