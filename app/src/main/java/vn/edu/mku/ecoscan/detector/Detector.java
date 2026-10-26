package vn.edu.mku.ecoscan.detector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Detector {

    private static final String TAG = "Detector";

    private static final float CONF_THRESHOLD = 0.20f;
    private static final float IOU_THRESHOLD = 0.45f;

    private final Interpreter interpreter;
    private final List<String> labels;

    private int inputSize;
    private boolean inputIsNCHW; // true nếu input shape là [1,3,H,W], false nếu [1,H,W,3]

    private int numClasses;
    private int numBoxes;
    private boolean channelsFirst;

    private final DataType outputDataType;
    private final float outputScale;
    private final int outputZeroPoint;

    private Boolean coordsAreNormalized = null;

    public Detector(Context context, String modelPath, String labelPath) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModelFile(context, modelPath), options);

        labels = loadLabels(context, labelPath);

        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inShape = inputTensor.shape();
        Log.d(TAG, "Input shape: " + Arrays.toString(inShape) + " dtype=" + inputTensor.dataType());

        if (inShape.length == 4) {
            if (inShape[1] == 3) {
                // [1, 3, H, W] -> NCHW
                inputIsNCHW = true;
                inputSize = inShape[2];
            } else {
                // [1, H, W, 3] -> NHWC
                inputIsNCHW = false;
                inputSize = inShape[1];
            }
        } else {
            inputIsNCHW = false;
            inputSize = 640;
        }
        Log.d(TAG, "inputSize=" + inputSize + " inputIsNCHW=" + inputIsNCHW);

        Tensor outputTensor = interpreter.getOutputTensor(0);
        int[] outShape = outputTensor.shape();
        outputDataType = outputTensor.dataType();

        Tensor.QuantizationParams qp = outputTensor.quantizationParams();
        outputScale = qp.getScale();
        outputZeroPoint = qp.getZeroPoint();

        Log.d(TAG, "Output shape: " + Arrays.toString(outShape)
                + " dtype=" + outputDataType
                + " scale=" + outputScale + " zeroPoint=" + outputZeroPoint);

        int a = outShape[1];
        int b = outShape[2];
        if (a < b) {
            channelsFirst = true;   // [1, 4+numClasses, numBoxes]
            numClasses = a - 4;
            numBoxes = b;
        } else {
            channelsFirst = false;  // [1, numBoxes, 4+numClasses]
            numClasses = b - 4;
            numBoxes = a;
        }
        Log.d(TAG, "numClasses=" + numClasses + " numBoxes=" + numBoxes + " channelsFirst=" + channelsFirst);
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = inputStream.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    private List<String> loadLabels(Context context, String labelPath) throws IOException {
        List<String> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) result.add(line.trim());
        }
        reader.close();
        return result;
    }

    /**
     * Tiền xử lý Bitmap thành ByteBuffer đúng layout mà model yêu cầu.
     * NHWC: ...R,G,B, R,G,B,...  (từng pixel liền nhau)
     * NCHW: R,R,R,...(hết ảnh), G,G,G,...(hết ảnh), B,B,B,...(hết ảnh)  (từng kênh liền nhau)
     */
    private ByteBuffer preprocess(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputSize * inputSize];
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        if (inputIsNCHW) {
            for (int pixel : pixels) {
                buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
            }
            for (int pixel : pixels) {
                buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
            }
            for (int pixel : pixels) {
                buffer.putFloat((pixel & 0xFF) / 255.0f);         // B
            }
        } else {
            for (int pixel : pixels) {
                buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
                buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
                buffer.putFloat((pixel & 0xFF) / 255.0f);
            }
        }

        buffer.rewind();
        return buffer;
    }

    private float readValue(Object rawOutput, int channelIdx, int boxIdx) {
        float raw;
        if (channelsFirst) {
            if (outputDataType == DataType.FLOAT32) {
                raw = ((float[][][]) rawOutput)[0][channelIdx][boxIdx];
            } else {
                int v = ((byte[][][]) rawOutput)[0][channelIdx][boxIdx] & 0xFF;
                raw = (v - outputZeroPoint) * outputScale;
            }
        } else {
            if (outputDataType == DataType.FLOAT32) {
                raw = ((float[][][]) rawOutput)[0][boxIdx][channelIdx];
            } else {
                int v = ((byte[][][]) rawOutput)[0][boxIdx][channelIdx] & 0xFF;
                raw = (v - outputZeroPoint) * outputScale;
            }
        }
        return raw;
    }

    public List<BoundingBox> detect(Bitmap bitmap) {
        ByteBuffer input = preprocess(bitmap);

        Object output;
        if (outputDataType == DataType.FLOAT32) {
            output = channelsFirst
                    ? new float[1][4 + numClasses][numBoxes]
                    : new float[1][numBoxes][4 + numClasses];
        } else {
            output = channelsFirst
                    ? new byte[1][4 + numClasses][numBoxes]
                    : new byte[1][numBoxes][4 + numClasses];
        }

        interpreter.run(input, output);

        List<float[]> raw = new ArrayList<>(); // [cx, cy, w, h, score, classId]
        float maxCoordSeen = 0f;
        float maxScoreSeen = 0f;

        for (int i = 0; i < numBoxes; i++) {
            float cx = readValue(output, 0, i);
            float cy = readValue(output, 1, i);
            float w = readValue(output, 2, i);
            float h = readValue(output, 3, i);

            int bestClass = -1;
            float bestScore = 0f;
            for (int c = 0; c < numClasses; c++) {
                float score = readValue(output, 4 + c, i);
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }

            if (bestScore > maxScoreSeen) maxScoreSeen = bestScore;
            maxCoordSeen = Math.max(maxCoordSeen, Math.max(cx, cy));

            if (bestScore >= 0.05f && bestClass >= 0) {
                raw.add(new float[]{cx, cy, w, h, bestScore, bestClass});
            }
        }

        Log.d(TAG, "maxScoreSeen=" + maxScoreSeen + " maxCoordSeen=" + maxCoordSeen
                + " rawCandidates(>=0.05)=" + raw.size());

        if (coordsAreNormalized == null) {
            coordsAreNormalized = maxCoordSeen <= 1.5f;
            Log.d(TAG, "Auto-detected coordsAreNormalized=" + coordsAreNormalized);
        }

        List<BoundingBox> candidates = new ArrayList<>();
        for (float[] r : raw) {
            float cx = r[0], cy = r[1], w = r[2], h = r[3], score = r[4];
            int classId = (int) r[5];

            if (score < CONF_THRESHOLD) continue;

            float ncx, ncy, nw, nh;
            if (coordsAreNormalized) {
                ncx = cx; ncy = cy; nw = w; nh = h;
            } else {
                ncx = cx / inputSize;
                ncy = cy / inputSize;
                nw = w / inputSize;
                nh = h / inputSize;
            }

            float x1 = clamp01(ncx - nw / 2f);
            float y1 = clamp01(ncy - nh / 2f);
            float x2 = clamp01(ncx + nw / 2f);
            float y2 = clamp01(ncy + nh / 2f);

            // Dịch nhãn tiếng Anh (từ labels.txt) sang tiếng Việt không dấu để hiển thị
            String englishLabel = classId < labels.size() ? labels.get(classId) : "unknown";
            String label = LabelTranslator.translate(englishLabel);

            candidates.add(new BoundingBox(x1, y1, x2, y2, ncx, ncy, nw, nh, score, classId, label));
        }

        Log.d(TAG, "candidates after threshold(" + CONF_THRESHOLD + ")=" + candidates.size());

        return nonMaxSuppression(candidates);
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private List<BoundingBox> nonMaxSuppression(List<BoundingBox> boxes) {
        List<BoundingBox> result = new ArrayList<>();
        boxes.sort((a, b) -> Float.compare(b.score, a.score));

        boolean[] removed = new boolean[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            if (removed[i]) continue;
            BoundingBox a = boxes.get(i);
            result.add(a);
            for (int j = i + 1; j < boxes.size(); j++) {
                if (removed[j]) continue;
                BoundingBox b = boxes.get(j);
                float overlap = iou(a, b);
                // Class-specific NMS: suppress if same class and IoU > IOU_THRESHOLD
                if (a.classId == b.classId && overlap > IOU_THRESHOLD) {
                    removed[j] = true;
                }
            }
        }
        return result;
    }

    private float iou(BoundingBox a, BoundingBox b) {
        float interLeft = Math.max(a.x1, b.x1);
        float interTop = Math.max(a.y1, b.y1);
        float interRight = Math.min(a.x2, b.x2);
        float interBottom = Math.min(a.y2, b.y2);

        float interArea = Math.max(0, interRight - interLeft) * Math.max(0, interBottom - interTop);
        float areaA = (a.x2 - a.x1) * (a.y2 - a.y1);
        float areaB = (b.x2 - b.x1) * (b.y2 - b.y1);

        return interArea / (areaA + areaB - interArea + 1e-6f);
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
