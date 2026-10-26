package vn.edu.mku.ecoscan.detector;

import java.util.HashMap;
import java.util.Map;

/** Ánh xạ nhãn COCO (tiếng Anh) sang tiếng Việt có dấu để hiển thị lên màn hình. */
public class LabelTranslator {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        MAP.put("person", "Người");
        MAP.put("bicycle", "Xe đạp");
        MAP.put("car", "Ô tô");
        MAP.put("motorcycle", "Xe máy");
        MAP.put("airplane", "Máy bay");
        MAP.put("bus", "Xe buýt");
        MAP.put("train", "Tàu hỏa");
        MAP.put("truck", "Xe tải");
        MAP.put("boat", "Thuyền");
        MAP.put("traffic light", "Đèn giao thông");
        MAP.put("fire hydrant", "Trụ cứu hỏa");
        MAP.put("stop sign", "Biển dừng");
        MAP.put("parking meter", "Máy thu phí đỗ xe");
        MAP.put("bench", "Ghế dài");
        MAP.put("bird", "Chim");
        MAP.put("cat", "Mèo");
        MAP.put("dog", "Chó");
        MAP.put("horse", "Ngựa");
        MAP.put("sheep", "Cừu");
        MAP.put("cow", "Bò");
        MAP.put("elephant", "Voi");
        MAP.put("bear", "Gấu");
        MAP.put("zebra", "Ngựa vằn");
        MAP.put("giraffe", "Hươu cao cổ");
        MAP.put("backpack", "Ba lô");
        MAP.put("umbrella", "Ô / Dù");
        MAP.put("handbag", "Túi xách");
        MAP.put("tie", "Cà vạt");
        MAP.put("suitcase", "Vali");
        MAP.put("frisbee", "Đĩa ném");
        MAP.put("skis", "Ván trượt tuyết");
        MAP.put("snowboard", "Ván trượt tuyết đơn");
        MAP.put("sports ball", "Quả bóng");
        MAP.put("kite", "Diều");
        MAP.put("baseball bat", "Gậy bóng chày");
        MAP.put("baseball glove", "Găng bóng chày");
        MAP.put("skateboard", "Ván trượt");
        MAP.put("surfboard", "Ván lướt sóng");
        MAP.put("tennis racket", "Vợt tennis");
        MAP.put("bottle", "Chai / Lọ");
        MAP.put("wine glass", "Ly rượu");
        MAP.put("cup", "Cốc / Ly");
        MAP.put("fork", "Nĩa");
        MAP.put("knife", "Dao");
        MAP.put("spoon", "Thìa / Muỗng");
        MAP.put("bowl", "Bát / Chén");
        MAP.put("banana", "Chuối");
        MAP.put("apple", "Táo");
        MAP.put("sandwich", "Bánh sandwich");
        MAP.put("orange", "Cam");
        MAP.put("broccoli", "Súp lơ xanh");
        MAP.put("carrot", "Cà rốt");
        MAP.put("hot dog", "Xúc xích");
        MAP.put("pizza", "Bánh pizza");
        MAP.put("donut", "Bánh donut");
        MAP.put("cake", "Bánh kem");
        MAP.put("chair", "Ghế");
        MAP.put("couch", "Ghế sofa");
        MAP.put("potted plant", "Cây cảnh");
        MAP.put("bed", "Giường");
        MAP.put("dining table", "Bàn ăn");
        MAP.put("toilet", "Bồn cầu");
        MAP.put("tv", "Tivi");
        MAP.put("laptop", "Máy tính xách tay");
        MAP.put("mouse", "Chuột máy tính");
        MAP.put("remote", "Điều khiển");
        MAP.put("keyboard", "Bàn phím");
        MAP.put("cell phone", "Điện thoại");
        MAP.put("microwave", "Lò vi sóng");
        MAP.put("oven", "Lò nướng");
        MAP.put("toaster", "Máy nướng bánh mì");
        MAP.put("sink", "Bồn rửa");
        MAP.put("refrigerator", "Tủ lạnh");
        MAP.put("book", "Sách");
        MAP.put("clock", "Đồng hồ");
        MAP.put("vase", "Bình hoa");
        MAP.put("scissors", "Kéo");
        MAP.put("teddy bear", "Gấu bông");
        MAP.put("hair drier", "Máy sấy tóc");
        MAP.put("toothbrush", "Bàn chải đánh răng");
    }

    /** Trả về nhãn tiếng Việt có dấu; nếu không tìm thấy thì trả lại nhãn gốc. */
    public static String translate(String englishLabel) {
        if (englishLabel == null) return "Không xác định";
        String result = MAP.get(englishLabel.toLowerCase().trim());
        return result != null ? result : englishLabel;
    }
}
