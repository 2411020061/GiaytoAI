package vn.edu.mku.ecoscan.ml;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LỚP HỖ TRỢ KẾT NỐI VỚI ROBOFLOW HOSTEAD INFERENCE API
 * Model URL: https://universe.roboflow.com/vietnamidcardinfo/vietnamese-card-id-b0vlf-zm3c0
 */
public class RoboflowDetector {

    private static final String TAG = "RoboflowDetector";

    // =========================================================================
    // 📍 [CHỖ 1] DÁN API KEY ROBOFLOW CỦA BẠN VÀO ĐÂY:
    // Bạn lấy API Key tại: Roboflow Dashboard -> Account Settings -> API Key
    // =========================================================================
    public static final String ROBOFLOW_API_KEY = "PFacu99jHNbO9ZiirZhG"; // <-- Thay bằng API Key thật của bạn

    // =========================================================================
    // 📍 [CHỖ 2] CẤU HÌNH TÊN MODEL VÀ PHIÊN BẢN (VERSION):
    // Model: vietnamese-card-id-b0vlf-zm3c0
    // Version: 1 (hoặc số version hiển thị trên Roboflow Universe)
    // =========================================================================
    public static final String MODEL_ID = "vietnamese-card-id-b0vlf-zm3c0";
    public static final String MODEL_VERSION = "1";

    // Endpoint chuẩn của Roboflow Inference API
    private static final String ROBOFLOW_API_URL = "https://detect.roboflow.com/" + MODEL_ID + "/" + MODEL_VERSION;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RoboflowCallback {
        void onSuccess(List<RoboflowPrediction> predictions, String rawResponse);
        void onError(Exception e);
    }

    public static class RoboflowPrediction {
        public String className;
        public float confidence;
        public float x;
        public float y;
        public float width;
        public float height;

        @Override
        public String toString() {
            return "RoboflowPrediction{" +
                    "class='" + className + '\'' +
                    ", confidence=" + confidence +
                    ", x=" + x +
                    ", y=" + y +
                    ", w=" + width +
                    ", h=" + height +
                    '}';
        }
    }

    /**
     * Gửi ảnh lên Roboflow Hosted API để nhận diện mặt CCCD / Thông tin CCCD
     *
     * @param bitmap   Ảnh CCCD cần nhận diện
     * @param callback Callback trả về kết quả nhận diện từ Roboflow
     */
    public void detectCard(Bitmap bitmap, RoboflowCallback callback) {
        if (bitmap == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Bitmap null"));
            return;
        }

        executorService.execute(() -> {
            try {
                // 1. Chuyển Bitmap thành Base64 string
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // 2. Tạo URL gửi Request Roboflow
                String urlString = ROBOFLOW_API_URL + "?api_key=" + ROBOFLOW_API_KEY;
                URL url = new URL(urlString);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                // Gửi dữ liệu Base64
                OutputStream os = conn.getOutputStream();
                os.write(base64Image.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Roboflow API Response Code: " + responseCode);

                BufferedReader br;
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                String jsonString = response.toString();
                Log.d(TAG, "Roboflow Response: " + jsonString);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Lỗi API Roboflow (" + responseCode + "): " + jsonString);
                }

                // =========================================================================
                // 📍 [CHỖ 3] CODE PHÂN TÍCH KẾT QUẢ JSON TRẢ VỀ TỪ ROBOFLOW MODEL:
                // =========================================================================
                List<RoboflowPrediction> predictions = parseRoboflowJson(jsonString);

                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(predictions, jsonString);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Lỗi gọi Roboflow Inference API", e);
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    private List<RoboflowPrediction> parseRoboflowJson(String jsonStr) {
        List<RoboflowPrediction> list = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            if (jsonObject.has("predictions")) {
                JSONArray array = jsonObject.getJSONArray("predictions");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    RoboflowPrediction p = new RoboflowPrediction();
                    p.className = item.optString("class", "unknown");
                    p.confidence = (float) item.optDouble("confidence", 0.0);
                    p.x = (float) item.optDouble("x", 0.0);
                    p.y = (float) item.optDouble("y", 0.0);
                    p.width = (float) item.optDouble("width", 0.0);
                    p.height = (float) item.optDouble("height", 0.0);
                    list.add(p);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi parse JSON từ Roboflow", e);
        }
        return list;
    }
}
