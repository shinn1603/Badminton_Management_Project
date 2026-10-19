package vn.yain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vn.yain.entity.AuditLog;
import vn.yain.repository.AuditLogRepository;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseManagementService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final AuditLogRepository auditLogRepository;

    public static final String BASE_SCRIPT_PATH = "database/init_badminton.sql";
    public static final String BACKUP_DIR_PATH = "database/backups";

    // Danh sách 25 bảng cốt lõi theo đúng thứ tự ràng buộc khóa ngoại (Foreign Key)
    public static final List<String> CORE_TABLES = List.of(
            "branches",
            "courts",
            "time_slots",
            "pricing_rules",
            "customers",
            "staff",
            "users",
            "equipment",
            "products",
            "bookings",
            "booking_services",
            "invoices",
            "invoice_equipment_details",
            "invoice_product_details",
            "tournaments",
            "tournament_registrations",
            "tournament_matches",
            "payments",
            "work_shifts",
            "stock_receipts",
            "stock_equipment_details",
            "stock_product_details",
            "promotions",
            "system_settings",
            "audit_logs"
    );

    /**
     * Lấy thông tin thống kê trạng thái hiện tại của cơ sở dữ liệu
     */
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("connectionStatus", "CONNECTED");

        String dbProduct = "Unknown";
        try (Connection conn = dataSource.getConnection()) {
            dbProduct = conn.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            log.warn("Không thể lấy tên hệ quản trị CSDL: {}", e.getMessage());
        }
        result.put("databaseProduct", dbProduct);

        // Thống kê file script gốc init_badminton.sql
        File baseFile = new File(BASE_SCRIPT_PATH);
        Map<String, Object> baseInfo = new LinkedHashMap<>();
        baseInfo.put("path", BASE_SCRIPT_PATH);
        baseInfo.put("exists", baseFile.exists());
        if (baseFile.exists()) {
            baseInfo.put("sizeBytes", baseFile.length());
            baseInfo.put("sizeKb", String.format("%.2f KB", baseFile.length() / 1024.0));
            LocalDateTime lastMod = LocalDateTime.ofInstant(Instant.ofEpochMilli(baseFile.lastModified()), ZoneId.systemDefault());
            baseInfo.put("lastModified", lastMod);
            baseInfo.put("lastModifiedFormatted", lastMod.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        } else {
            baseInfo.put("sizeKb", "0 KB");
            baseInfo.put("lastModified", null);
            baseInfo.put("lastModifiedFormatted", "Chưa tạo");
        }
        result.put("baseScript", baseInfo);

        // Thống kê số lượng bản ghi của từng bảng trong CSDL
        Map<String, Long> tableCounts = new LinkedHashMap<>();
        long totalRecords = 0;
        for (String table : CORE_TABLES) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                long c = (count != null) ? count : 0;
                tableCounts.put(table, c);
                totalRecords += c;
            } catch (Exception ignored) {
                tableCounts.put(table, 0L);
            }
        }
        result.put("tableCounts", tableCounts);
        result.put("totalRecords", totalRecords);

        // Số lượng file sao lưu hiện có
        List<Map<String, Object>> backups = listBackups();
        result.put("totalBackups", backups.size());
        result.put("backups", backups);

        return result;
    }

    /**
     * Cập nhật lại Database gốc: Lấy toàn bộ dữ liệu đang có trong CSDL trực tiếp,
     * đồng bộ vào file database/init_badminton.sql để biến file này thành bản mới nhất.
     */
    public synchronized Map<String, Object> syncToBaseDatabase(String adminUsername) throws Exception {
        ensureBackupDirectory();

        File baseFile = new File(BASE_SCRIPT_PATH);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // 1. Sao lưu file gốc cũ trước khi ghi đè để tuyệt đối an toàn
        if (baseFile.exists()) {
            File safetyBackup = new File(BACKUP_DIR_PATH, "init_badminton_backup_" + timestamp + ".sql");
            Files.copy(baseFile.toPath(), safetyBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("[DATABASE SYNC] Đã tạo bản sao lưu an toàn file gốc cũ tại: {}", safetyBackup.getPath());
        }

        // 2. Trích xuất toàn bộ dữ liệu hiện tại thành khối INSERT
        Map<String, Integer> tableStats = new LinkedHashMap<>();
        int totalRows = 0;
        StringBuilder dataSb = new StringBuilder();
        dataSb.append("\n-- =====================================================================\n");
        dataSb.append("-- 5. CHÈN DỮ LIỆU MẪU ĐẦY ĐỦ CHO TẤT CẢ 25 BẢNG (SEED DATA - BẢN CẬP NHẬT MỚI NHẤT)\n");
        dataSb.append("-- Đồng bộ tự động từ cơ sở dữ liệu thực tế vào: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        dataSb.append("-- Người thực hiện cập nhật: ").append(adminUsername != null ? adminUsername : "admin").append("\n");
        dataSb.append("-- =====================================================================\n\n");
        dataSb.append("-- Tạm thời vô hiệu hóa kiểm tra ràng buộc toàn vẹn khóa ngoại\n");
        dataSb.append("EXEC sp_MSforeachtable \"ALTER TABLE ? NOCHECK CONSTRAINT all\";\nGO\n\n");

        try (Connection conn = dataSource.getConnection()) {
            for (String tableName : CORE_TABLES) {
                TableDumpResult dumpRes = dumpTableData(tableName, conn);
                dataSb.append(dumpRes.sql).append("\n");
                tableStats.put(tableName, dumpRes.rowCount);
                totalRows += dumpRes.rowCount;
            }
        }

        dataSb.append("-- Bật lại ràng buộc toàn vẹn khóa ngoại sau khi nạp dữ liệu xong\n");
        dataSb.append("EXEC sp_MSforeachtable \"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all\";\nGO\n");

        // 3. Đọc nội dung file gốc hiện tại để giữ lại nguyên vẹn DDL (Phần 1->4) và Indexes/Procedures/Triggers (Phần 6->9)
        String originalContent = "";
        if (baseFile.exists()) {
            originalContent = Files.readString(baseFile.toPath(), StandardCharsets.UTF_8);
        }

        String finalContent;
        int sec5Index = originalContent.indexOf("-- 5. CHÈN DỮ LIỆU MẪU ĐẦY ĐỦ");
        if (sec5Index < 0) sec5Index = originalContent.indexOf("-- 5.");

        int sec6Index = originalContent.indexOf("-- 6. TẠO CÁC CHỈ MỤC INDEX");
        if (sec6Index < 0) sec6Index = originalContent.indexOf("-- 6.");

        if (sec5Index >= 0 && sec6Index > sec5Index) {
            String headerPart = originalContent.substring(0, sec5Index);
            String footerPart = originalContent.substring(sec6Index);
            finalContent = headerPart + dataSb.toString() + "\n" + footerPart;
        } else {
            // Nếu không tìm thấy dấu phân tách, tạo file hoàn chỉnh mới
            finalContent = originalContent + "\n" + dataSb.toString();
        }

        // 4. Ghi đè vào file database/init_badminton.sql với encoding UTF-8
        Files.writeString(baseFile.toPath(), finalContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("[DATABASE SYNC] Đã cập nhật thành công file Database gốc {} ({} bản ghi, kích thước: {} KB)",
                BASE_SCRIPT_PATH, totalRows, baseFile.length() / 1024);

        // 5. Ghi nhật ký Audit Log hệ thống
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(adminUsername != null ? adminUsername : "admin");
        auditLog.setActionType("Cập nhật Database gốc");
        auditLog.setDetails(String.format("Đồng bộ %d bản ghi mới nhất trên CSDL vào file %s (Dung lượng: %.2f KB)",
                totalRows, BASE_SCRIPT_PATH, baseFile.length() / 1024.0));
        auditLog.setModule("Cơ sở dữ liệu");
        auditLog.setStatus("Thành công");
        auditLog.setActionTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Đã cập nhật Database gốc init_badminton.sql thành bản mới nhất thành công!");
        resp.put("filePath", BASE_SCRIPT_PATH);
        resp.put("totalRecords", totalRows);
        resp.put("fileSizeKb", String.format("%.2f KB", baseFile.length() / 1024.0));
        resp.put("tableStats", tableStats);
        resp.put("updatedAt", LocalDateTime.now());
        return resp;
    }

    /**
     * Tạo một bản sao lưu toàn diện (.sql) của cơ sở dữ liệu hiện tại
     */
    public synchronized Map<String, Object> createBackupSnapshot(String requestedFilename, String adminUsername) throws Exception {
        ensureBackupDirectory();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = (requestedFilename != null && !requestedFilename.trim().isEmpty())
                ? requestedFilename.trim()
                : "backup_ute_sport_" + timestamp + ".sql";

        if (!filename.endsWith(".sql")) {
            filename += ".sql";
        }
        // Bảo vệ chống Path Traversal
        filename = new File(filename).getName();

        File backupFile = new File(BACKUP_DIR_PATH, filename);

        // Sinh nội dung SQL hoàn chỉnh (Bao gồm DDL và DML)
        String fullSqlDump = generateFullSqlDump(adminUsername);
        Files.writeString(backupFile.toPath(), fullSqlDump, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Đếm tổng số bản ghi
        long totalRecords = 0;
        for (String table : CORE_TABLES) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                if (count != null) totalRecords += count;
            } catch (Exception ignored) {}
        }

        // Ghi AuditLog
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(adminUsername != null ? adminUsername : "admin");
        auditLog.setActionType("Sao lưu cơ sở dữ liệu");
        auditLog.setDetails(String.format("Tạo bản sao lưu %s (Dung lượng: %.2f KB, %d bản ghi) thành công",
                filename, backupFile.length() / 1024.0, totalRecords));
        auditLog.setModule("Hệ thống sao lưu");
        auditLog.setStatus("Thành công");
        auditLog.setActionTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("filename", filename);
        resp.put("filePath", backupFile.getPath());
        resp.put("sizeBytes", backupFile.length());
        resp.put("sizeKb", String.format("%.2f KB", backupFile.length() / 1024.0));
        resp.put("totalRecords", totalRecords);
        resp.put("createdAt", LocalDateTime.now());
        resp.put("message", "Đã tạo bản sao lưu " + filename + " thành công!");
        return resp;
    }

    /**
     * Danh sách các tệp sao lưu hiện có trong thư mục database/backups
     */
    public List<Map<String, Object>> listBackups() {
        ensureBackupDirectory();
        File dir = new File(BACKUP_DIR_PATH);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql") || name.endsWith(".sql.gz"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (File f : files) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("filename", f.getName());
            item.put("sizeBytes", f.length());
            item.put("sizeKb", String.format("%.2f KB", f.length() / 1024.0));
            LocalDateTime modTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
            item.put("lastModified", modTime);
            item.put("lastModifiedFormatted", modTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            item.put("isBaseBackup", f.getName().startsWith("init_badminton"));
            item.put("downloadUrl", "/api/admin/database/download?file=" + f.getName());
            list.add(item);
        }
        return list;
    }

    /**
     * Lấy tệp sao lưu để tải về (Bảo vệ chống Directory Traversal)
     */
    public File getBackupFile(String filename) {
        if (filename == null || filename.isBlank()) return null;
        String cleanName = new File(filename).getName();
        File file = new File(BACKUP_DIR_PATH, cleanName);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    /**
     * Lấy file database gốc init_badminton.sql để tải về
     */
    public File getBaseScriptFile() {
        File file = new File(BASE_SCRIPT_PATH);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    /**
     * Tải lên một tệp sao lưu (.sql) từ máy tính vào thư mục database/backups
     */
    public synchronized Map<String, Object> saveUploadedBackup(org.springframework.web.multipart.MultipartFile file, String adminUsername) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp tin sao lưu (.sql) để tải lên!");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.toLowerCase().endsWith(".sql") && !originalName.toLowerCase().endsWith(".sql.gz"))) {
            throw new IllegalArgumentException("Chỉ chấp nhận tệp định dạng .sql hoặc .sql.gz!");
        }
        ensureBackupDirectory();
        String safeName = new File(originalName).getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!safeName.toLowerCase().endsWith(".sql") && !safeName.toLowerCase().endsWith(".sql.gz")) {
            safeName += ".sql";
        }
        File targetFile = new File(BACKUP_DIR_PATH, safeName);
        file.transferTo(targetFile);

        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(adminUsername != null ? adminUsername : "admin");
        auditLog.setActionType("Tải lên bản sao lưu");
        auditLog.setDetails(String.format("Tải lên tệp sao lưu %s (Dung lượng: %.2f KB) từ máy tính",
                safeName, targetFile.length() / 1024.0));
        auditLog.setModule("Hệ thống sao lưu");
        auditLog.setStatus("Thành công");
        auditLog.setActionTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("filename", safeName);
        resp.put("sizeBytes", targetFile.length());
        resp.put("sizeKb", String.format("%.2f KB", targetFile.length() / 1024.0));
        resp.put("message", "Đã tải lên tệp sao lưu " + safeName + " thành công!");
        return resp;
    }

    /**
     * Phục hồi cơ sở dữ liệu từ file sao lưu
     */
    public synchronized Map<String, Object> restoreDatabase(String filename, String adminUsername) throws Exception {
        File restoreFile = getBackupFile(filename);
        if (restoreFile == null) {
            throw new FileNotFoundException("Không tìm thấy tệp sao lưu: " + filename);
        }

        String sqlScript = Files.readString(restoreFile.toPath(), StandardCharsets.UTF_8);
        executeSqlScriptBatches(sqlScript);

        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(adminUsername != null ? adminUsername : "admin");
        auditLog.setActionType("Khôi phục cơ sở dữ liệu");
        auditLog.setDetails("Khôi phục toàn bộ CSDL từ tệp sao lưu " + filename + " thành công");
        auditLog.setModule("Hệ thống sao lưu");
        auditLog.setStatus("Thành công");
        auditLog.setActionTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Đã khôi phục cơ sở dữ liệu từ bản sao lưu " + filename + " thành công!");
        resp.put("filename", filename);
        resp.put("restoredAt", LocalDateTime.now());
        return resp;
    }

    /**
     * Khôi phục toàn bộ cơ sở dữ liệu về lại bản Database gốc chuẩn init_badminton.sql
     */
    public synchronized Map<String, Object> resetToBaseDatabase(String adminUsername) throws Exception {
        File baseFile = getBaseScriptFile();
        if (baseFile == null) {
            throw new FileNotFoundException("Không tìm thấy file Database gốc " + BASE_SCRIPT_PATH);
        }

        String sqlScript = Files.readString(baseFile.toPath(), StandardCharsets.UTF_8);
        executeSqlScriptBatches(sqlScript);

        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(adminUsername != null ? adminUsername : "admin");
        auditLog.setActionType("Khôi phục Database gốc");
        auditLog.setDetails("Khôi phục toàn bộ dữ liệu hệ thống về bản chuẩn gốc " + BASE_SCRIPT_PATH);
        auditLog.setModule("Cơ sở dữ liệu");
        auditLog.setStatus("Thành công");
        auditLog.setActionTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Đã khôi phục toàn bộ hệ thống về Database gốc " + BASE_SCRIPT_PATH + " thành công!");
        resp.put("restoredAt", LocalDateTime.now());
        return resp;
    }

    /**
     * Thực thi kịch bản SQL theo từng lô phân cách bởi lệnh GO
     */
    private void executeSqlScriptBatches(String sqlScript) throws SQLException {
        String[] batches = sqlScript.split("(?m)^\\s*GO\\s*$");
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(true);
                for (String batch : batches) {
                    String trimmed = batch.trim();
                    if (!trimmed.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(trimmed);
                        } catch (SQLException ex) {
                            log.warn("[SQL RESTORE WARNING] Lỗi khi thực thi batch: {}. Nội dung: {}",
                                    ex.getMessage(), trimmed.length() > 120 ? trimmed.substring(0, 120) + "..." : trimmed);
                        }
                    }
                }
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    /**
     * Trích xuất dữ liệu của 1 bảng thành các lệnh INSERT SQL Server tương thích UTF-8
     */
    private static class TableDumpResult {
        String sql;
        int rowCount;
        TableDumpResult(String sql, int rowCount) {
            this.sql = sql;
            this.rowCount = rowCount;
        }
    }

    private TableDumpResult dumpTableData(String tableName, Connection conn) {
        StringBuilder sb = new StringBuilder();
        int rowCount = 0;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            if (colCount == 0) {
                return new TableDumpResult("-- Bảng " + tableName + ": Không có cột nào.\n", 0);
            }

            boolean hasIdentity = false;
            List<String> colNames = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                String cName = meta.getColumnName(i);
                colNames.add(cName);
                try {
                    if (meta.isAutoIncrement(i)) {
                        hasIdentity = true;
                    }
                } catch (Exception ignored) {}
            }

            List<String> rowSqlList = new ArrayList<>();
            while (rs.next()) {
                rowCount++;
                StringBuilder rowSb = new StringBuilder("(");
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) rowSb.append(", ");
                    Object val = rs.getObject(i);
                    if (val == null) {
                        rowSb.append("NULL");
                    } else if (val instanceof Boolean) {
                        rowSb.append(((Boolean) val) ? "1" : "0");
                    } else if (val instanceof Number) {
                        rowSb.append(val.toString());
                    } else if (val instanceof byte[]) {
                        byte[] bytes = (byte[]) val;
                        rowSb.append("0x");
                        for (byte b : bytes) {
                            rowSb.append(String.format("%02x", b));
                        }
                    } else {
                        String strVal = val.toString();
                        rowSb.append("N'").append(strVal.replace("'", "''")).append("'");
                    }
                }
                rowSb.append(")");
                rowSqlList.add(rowSb.toString());
            }

            if (rowSqlList.isEmpty()) {
                return new TableDumpResult("-- Bảng " + tableName + ": Không có dữ liệu.\n", 0);
            }

            sb.append("\n-- -----------------------------------------------------\n");
            sb.append("-- Dữ liệu bảng: ").append(tableName).append(" (").append(rowCount).append(" bản ghi)\n");
            sb.append("-- -----------------------------------------------------\n");

            if (hasIdentity) {
                sb.append("SET IDENTITY_INSERT ").append(tableName).append(" ON;\n");
            }

            int batchSize = 250;
            for (int i = 0; i < rowSqlList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, rowSqlList.size());
                sb.append("INSERT INTO ").append(tableName).append(" (");
                sb.append(String.join(", ", colNames));
                sb.append(") VALUES\n");
                for (int j = i; j < end; j++) {
                    sb.append(rowSqlList.get(j));
                    if (j < end - 1) {
                        sb.append(",\n");
                    } else {
                        sb.append(";\n");
                    }
                }
            }

            if (hasIdentity) {
                sb.append("SET IDENTITY_INSERT ").append(tableName).append(" OFF;\n");
            }
            sb.append("GO\n");

        } catch (Exception e) {
            log.warn("Lỗi trích xuất dữ liệu bảng {}: {}", tableName, e.getMessage());
            return new TableDumpResult("-- Lỗi trích xuất bảng " + tableName + ": " + e.getMessage() + "\n", 0);
        }

        return new TableDumpResult(sb.toString(), rowCount);
    }

    /**
     * Sinh toàn bộ script SQL Dump (DDL + DML) từ trạng thái hiện tại
     */
    private String generateFullSqlDump(String adminUsername) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- =====================================================================\n");
        sb.append("-- BẢN SAO LƯU TOÀN DIỆN HỆ THỐNG QUẢN LÝ SÂN CẦU LÔNG UTE SPORT\n");
        sb.append("-- Thời gian xuất: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("-- Người tạo: ").append(adminUsername != null ? adminUsername : "admin").append("\n");
        sb.append("-- =====================================================================\n\n");

        File baseFile = new File(BASE_SCRIPT_PATH);
        if (baseFile.exists()) {
            try {
                String baseContent = Files.readString(baseFile.toPath(), StandardCharsets.UTF_8);
                int sec5Index = baseContent.indexOf("-- 5. CHÈN DỮ LIỆU MẪU ĐẦY ĐỦ");
                if (sec5Index < 0) sec5Index = baseContent.indexOf("-- 5.");

                int sec6Index = baseContent.indexOf("-- 6. TẠO CÁC CHỈ MỤC INDEX");
                if (sec6Index < 0) sec6Index = baseContent.indexOf("-- 6.");

                if (sec5Index >= 0 && sec6Index > sec5Index) {
                    sb.append(baseContent, 0, sec5Index);

                    // Chèn dữ liệu thực tế
                    sb.append("\n-- 5. DỮ LIỆU THỰC TẾ SAO LƯU\n");
                    sb.append("EXEC sp_MSforeachtable \"ALTER TABLE ? NOCHECK CONSTRAINT all\";\nGO\n\n");
                    try (Connection conn = dataSource.getConnection()) {
                        for (String table : CORE_TABLES) {
                            TableDumpResult r = dumpTableData(table, conn);
                            sb.append(r.sql).append("\n");
                        }
                    } catch (Exception e) {
                        log.warn("Lỗi kết nối CSDL khi tạo backup: {}", e.getMessage());
                    }
                    sb.append("EXEC sp_MSforeachtable \"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all\";\nGO\n\n");

                    sb.append(baseContent.substring(sec6Index));
                    return sb.toString();
                }
            } catch (Exception e) {
                log.warn("Lỗi đọc file base khi sinh dump: {}", e.getMessage());
            }
        }

        // Fallback: chỉ xuất dữ liệu DML
        sb.append("EXEC sp_MSforeachtable \"ALTER TABLE ? NOCHECK CONSTRAINT all\";\nGO\n\n");
        try (Connection conn = dataSource.getConnection()) {
            for (String table : CORE_TABLES) {
                TableDumpResult r = dumpTableData(table, conn);
                sb.append(r.sql).append("\n");
            }
        } catch (Exception e) {
            log.warn("Lỗi kết nối CSDL: {}", e.getMessage());
        }
        sb.append("EXEC sp_MSforeachtable \"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all\";\nGO\n");
        return sb.toString();
    }

    private void ensureBackupDirectory() {
        File dir = new File(BACKUP_DIR_PATH);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("Khởi tạo thư mục sao lưu {}: {}", BACKUP_DIR_PATH, created);
        }
    }
}
