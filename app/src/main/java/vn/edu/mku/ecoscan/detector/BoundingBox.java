package vn.edu.mku.ecoscan.detector;

public class BoundingBox {
    public float x1, y1, x2, y2; // toa do chuan hoa 0..1
    public float cx, cy, w, h;   // tam va kich thuoc box
    public float score;          // do tin cay (0..1)
    public int classId;          // ma lop trong COCO
    public String label;         // ten hien thi (da dich)

    public BoundingBox(float x1, float y1, float x2, float y2,
                       float cx, float cy, float w, float h,
                       float score, int classId, String label) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        this.cx = cx; this.cy = cy; this.w = w; this.h = h;
        this.score = score;
        this.classId = classId;
        this.label = label;
    }
}
