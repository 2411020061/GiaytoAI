package vn.edu.mku.ecoscan.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrashClassifier {

    private static final int INPUT_SIZE = 224;

    private static final Set<String> RECYCLABLE = new HashSet<>(
            Arrays.asList(
                    "Brown-glass",
                    "Paper/Cardboard",
                    "Green-glass",
                    "Metal",
                    "Paper",
                    "Plastic",
                    "White-glass"
            )
    );

    private final Interpreter interpreter;
    private final List<String> labels;

    public TrashClassifier(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context, "trash_classifier.tflite"));
        labels = loadLabels(context, "trash_labels.txt");
    }

    public static class ClassificationResult {
        public String label;
        public float confidence;
        public boolean recyclable;

        public String specialGuidance;
    }

    public ClassificationResult classify(Bitmap bitmap) {
        ByteBuffer input = preprocess(bitmap);
        float[][] output = new float[1][labels.size()];
        interpreter.run(input, output);

        int bestIdx = 0;
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > output[0][bestIdx]) bestIdx = i;
        }

        ClassificationResult result = new ClassificationResult();
        result.label = labels.get(bestIdx);
        result.confidence = output[0][bestIdx];
        result.recyclable = RECYCLABLE.contains(result.label);

        switch (result.label) {
            case "Battery":
                result.specialGuidance =
                        "Hazardous waste — take to a battery collection point.";
                break;

            case "Clothes":
            case "Shoes":
                result.specialGuidance =
                        "If still usable, donate or reuse.";
                break;

            case "Biological":
                result.specialGuidance =
                        "Can be composted when appropriate.";
                break;

            default:
                result.specialGuidance = null;
        }

        return result;
    }

    private ByteBuffer preprocess(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        for (int pixel : pixels) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 127.5f - 1f);
            buffer.putFloat(((pixel >> 8) & 0xFF) / 127.5f - 1f);
            buffer.putFloat((pixel & 0xFF) / 127.5f - 1f);
        }
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
        while ((line = reader.readLine()) != null) list.add(line);
        reader.close();
        return list;
    }

    public void close() {
        interpreter.close();
    }
}
