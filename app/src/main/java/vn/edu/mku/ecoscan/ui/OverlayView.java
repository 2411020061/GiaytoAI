package vn.edu.mku.ecoscan.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import vn.edu.mku.ecoscan.detector.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OverlayView extends View {

    private List<BoundingBox> boxes = new ArrayList<>();
    private int imageWidth = 0;
    private int imageHeight = 0;

    private final Paint boxPaint = new Paint();
    private final Paint textBgPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final RectF badgeRect = new RectF();

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Khung xanh lá tươi sáng như trong hình chụp
        boxPaint.setColor(Color.parseColor("#00E676"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
        boxPaint.setAntiAlias(true);

        textBgPaint.setColor(Color.parseColor("#00E676"));
        textBgPaint.setStyle(Paint.Style.FILL);
        textBgPaint.setAntiAlias(true);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);
        textPaint.setAntiAlias(true);
    }

    public void setResults(List<BoundingBox> results, int imgWidth, int imgHeight) {
        this.boxes = results != null ? results : new ArrayList<>();
        this.imageWidth = imgWidth;
        this.imageHeight = imgHeight;
        invalidate();
    }

    public void setResults(List<BoundingBox> results) {
        setResults(results, 0, 0);
    }

    public void clear() {
        this.boxes = new ArrayList<>();
        this.imageWidth = 0;
        this.imageHeight = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (boxes == null || boxes.isEmpty()) return;

        float viewW = getWidth();
        float viewH = getHeight();

        float dx = 0f;
        float dy = 0f;
        float actualW = viewW;
        float actualH = viewH;

        if (imageWidth > 0 && imageHeight > 0) {
            float scale = Math.min(viewW / imageWidth, viewH / imageHeight);
            actualW = imageWidth * scale;
            actualH = imageHeight * scale;
            dx = (viewW - actualW) / 2f;
            dy = (viewH - actualH) / 2f;
        }

        for (BoundingBox box : boxes) {
            float left = dx + box.x1 * actualW;
            float top = dy + box.y1 * actualH;
            float right = dx + box.x2 * actualW;
            float bottom = dy + box.y2 * actualH;

            // 1. Vẽ khung hình chữ nhật màu xanh lá
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // 2. Nhãn hiển thị dạng: Tên_Đồ_Vật 85%
            String text = box.label + " " + String.format(Locale.getDefault(), "%.0f%%", box.score * 100);
            float textWidth = textPaint.measureText(text);
            float textHeight = 44f;

            float badgeTop = (top - textHeight < 0) ? top : top - textHeight;
            float badgeBottom = (top - textHeight < 0) ? top + textHeight : top;

            badgeRect.set(left, badgeTop, left + textWidth + 20f, badgeBottom);
            canvas.drawRect(badgeRect, textBgPaint);

            // 3. Vẽ chữ màu đen đè lên thẻ xanh
            canvas.drawText(text, left + 10f, badgeBottom - 10f, textPaint);
        }
    }
}
