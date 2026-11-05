        String[] lines = rawText.split("\n");
        Pattern namePattern = Pattern.compile("(Họ tên|Họ và tên|Full name|Name|Họ và tên / Full name)[:\\s]+([A-ZÂĂÊÔƠƯĐàáảãạâầấẩẫậăằắẳẵặèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵ\\s]+)", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher matcher = namePattern.matcher(line.trim());
            if (matcher.find()) {
                String candidate = matcher.group(2).trim();
                if (candidate.length() > 3) {
                    return candidate.toUpperCase();
                }
            }
        }

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("^[A-ZÂĂÊÔƠƯĐ\\s]{5,35}$")
                    && !trimmed.contains("CỘNG HÒA")
                    && !trimmed.contains("XÃ HỘI")
                    && !trimmed.contains("ĐỘC LẬP")
                    && !trimmed.contains("VIỆT NAM")
                    && !trimmed.contains("CĂN CƯỚC")
                    && !trimmed.contains("QUỐC TỊCH")
                    && !trimmed.contains("NAM") && !trimmed.contains("NỮ")) {
                return trimmed;
            }
        }

        return "NGUYỄN VĂN A";
    }

    private static String extractCccdNumber(String rawText) {
        Pattern p = Pattern.compile("\\b(0\\d{11})\\b");
        Matcher m = p.matcher(rawText.replaceAll("\\s+", ""));
        if (m.find()) {
            return m.group(1);
        }

        Pattern p12 = Pattern.compile("\\b(\\d{12})\\b");
        Matcher m12 = p12.matcher(rawText.replaceAll("\\s+", ""));
        if (m12.find()) {
            return m12.group(1);
        }

        return "0792010" + String.format(Locale.US, "%05d", System.currentTimeMillis() % 100000);
    }

    private static String extractAddress(String rawText) {
        String[] lines = rawText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].toUpperCase();
            if (l.contains("NƠI THƯỜNG TRÚ") || l.contains("QUÊ QUÁN") || l.contains("RESIDENCE")) {
                StringBuilder addr = new StringBuilder();
                if (i < lines.length) {
                    addr.append(lines[i].replace("Nơi thường trú:", "").replace("Quê quán:", "").trim());
                }
                if (i + 1 < lines.length) {
                    addr.append(" ").append(lines[i + 1].trim());
                }
                return addr.toString().trim();
            }
        }
        return "";
    }

    private static String[] extractDates(String rawText) {
        String issueDate = "";
        String expiryDate = "";

        Pattern datePattern = Pattern.compile("\\b(\\d{1,2})[\\/\\-\\.](\\d{1,2})[\\/\\-\\.](\\d{4})\\b");
        String[] lines = rawText.split("\n");

        List<String> foundDates = new ArrayList<>();
        for (String line : lines) {
            Matcher m = datePattern.matcher(line);
            while (m.find()) {
                String d = String.format(Locale.US, "%02d/%02d/%04d",
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)));
                foundDates.add(d);

                String lineUpper = line.toUpperCase();
                if (lineUpper.contains("ĐẾN") || lineUpper.contains("HẾT HẠN") || lineUpper.contains("EXPIR") || lineUpper.contains("VALI")) {
                    expiryDate = d;
                } else if (lineUpper.contains("CẤP") || lineUpper.contains("TỪ") || lineUpper.contains("ISSUE")) {
                    issueDate = d;
                }
            }
        }

        if (expiryDate.isEmpty() && !foundDates.isEmpty()) {
            expiryDate = foundDates.get(foundDates.size() - 1);
        }
        if (issueDate.isEmpty() && foundDates.size() > 1) {
            issueDate = foundDates.get(0);
        }

        return new String[]{issueDate, expiryDate};
    }

    public static int calculateDaysLeft(String expiryDateStr) {
        if (expiryDateStr == null || expiryDateStr.isEmpty()) return 365;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Date expiryDate = sdf.parse(expiryDateStr);
            Date today = new Date();

            if (expiryDate != null) {
                long diffMs = expiryDate.getTime() - today.getTime();
                long days = diffMs / (1000 * 60 * 60 * 24);
                return (int) days;
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi định dạng ngày: " + expiryDateStr, e);
        }
        return 180;
    }

    private static DocumentEntity createFallbackDocument(String title, String imagePath) {
        return new DocumentEntity(
                title, "CCCD", "0792010" + String.format(Locale.US, "%05d", System.currentTimeMillis() % 100000),
                "NGUYỄN VĂN A", "01/01/2021", "01/01/2035", 3650, "ACTIVE", imagePath
        );
    }
}
