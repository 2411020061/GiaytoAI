package vn.edu.mku.ecoscan.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
    static {
        OpenCVLoader.initLocal();
    }

    public static Bitmap enhance(Bitmap input) {
        Mat src = new Mat();
        Utils.bitmapToMat(input, src);

        Mat enhanced = new Mat();
        src.convertTo(enhanced, -1, 1.15, 10);

        Bitmap output = Bitmap.createBitmap(enhanced.cols(), enhanced.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(enhanced, output);
        return output;
    }

    public static Bitmap cropMainObject(Bitmap input) {
        Mat src = new Mat();
        Utils.bitmapToMat(input, src);

        Rect biggest = findMainObjectRect(src);

        Bitmap result = input;
        if (biggest != null) {
            int padX = (int) (biggest.width * 0.1);
            int padY = (int) (biggest.height * 0.1);
            Rect padded = new Rect(
                    Math.max(0, biggest.x - padX),
                    Math.max(0, biggest.y - padY),
                    Math.min(src.cols() - Math.max(0, biggest.x - padX), biggest.width + 2 * padX),
                    Math.min(src.rows() - Math.max(0, biggest.y - padY), biggest.height + 2 * padY));
            Mat cropped = new Mat(src, padded);
            result = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped, result);
            cropped.release();
        }

        src.release();
        return result;
    }

    public static Bitmap drawMainObjectBox(Bitmap input) {
        Mat src = new Mat();
        Utils.bitmapToMat(input, src);

        Rect biggest = findMainObjectRect(src);
        src.release();

        Bitmap output = input.copy(Bitmap.Config.ARGB_8888, true);
        if (biggest != null) {
            Canvas canvas = new Canvas(output);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8f);
            canvas.drawRect(biggest.x, biggest.y,
                    biggest.x + biggest.width, biggest.y + biggest.height, paint);
        }
        return output;
    }

    private static Rect findMainObjectRect(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);
        Imgproc.dilate(edges, edges, Mat.ones(5, 5, CvType.CV_8U));

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double minArea = src.rows() * src.cols() * 0.05;
        double centerX = src.cols() / 2.0;
        double centerY = src.rows() / 2.0;

        Rect best = null;
        double bestScore = 0;
        for (MatOfPoint c : contours) {
            Rect r = Imgproc.boundingRect(c);
            double area = r.area();
            if (area < minArea) continue;

            double rectCenterX = r.x + r.width / 2.0;
            double rectCenterY = r.y + r.height / 2.0;
            double distanceToCenter = Math.hypot(rectCenterX - centerX, rectCenterY - centerY);

            double score = area / (1 + distanceToCenter);
            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }

        gray.release();
        edges.release();

        return best;
    }
}
