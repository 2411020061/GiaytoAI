package vn.edu.mku.ecoscan.ml;

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

public class CccdClassifier {

    private static final String TAG = "CccdClassifier";
    private static final String MODEL_FILE = "cccd_classifier.tflite";
    private static final String LABEL_FILE = "labels.txt";

    private final Interpreter interpreter;
    private final List<String> labels;
    private int inputSize = 224;

    public CccdClassifier(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModelFile(context, MODEL_FILE), options);
        labels = loadLabels(context, LABEL_FILE);

        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inShape = inputTensor.shape();
        Log.d(TAG, "Input shape: " + Arrays.toString(inShape) + " dataType=" + inputTensor.dataType());

        if (inShape.length == 4) {
            inputSize = inShape[1];
        }
    }

    public static class ClassificationResult {
        public String label;        // "dung" or "sai"
        public float confidence;
        public boolean isCccd;
    }

    public ClassificationResult classify(Bitmap bitmap) {
        if (bitmap == null) {
            ClassificationResult result = new ClassificationResult();
            result.label = "sai";
            result.confidence = 0.0f;
            result.isCccd = false;
            return result;
        }

        ByteBuffer input = preprocess(bitmap);
        float[][] output = new float[1][labels.size()];

        try {
            interpreter.run(input, output);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi chạy TFLite interpreter", e);
        }

        int bestIdx = 0;
        float maxProb = output[0][0];
        for (int i = 1; i < labels.size(); i++) {
            if (output[0][i] > maxProb) {
                maxProb = output[0][i];
                bestIdx = i;
            }
        }

        ClassificationResult result = new ClassificationResult();
        result.label = labels.get(bestIdx).trim().toLowerCase();
        result.confidence = maxProb;

        // Nhãn "dung" có nghĩa là Căn cước công dân chuẩn
        result.isCccd = "dung".equalsIgnoreCase(result.label);

        Log.d(TAG, "TFLite CCCD Classifier result: label=" + result.label + " confidence=" + result.confidence + " isCccd=" + result.isCccd);
        return result;
    }

    private ByteBuffer preprocess(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputSize * inputSize];
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;

            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
        }

        buffer.rewind();
        return buffer;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = inputStream.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    private List<String> loadLabels(Context context, String fileName) throws IOException {
        List<String> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                list.add(line.trim());
            }
        }
        reader.close();
        return list;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}
