package vn.yain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=badminton;encrypt=true;trustServerCertificate=true;",
    "spring.datasource.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver",
    "spring.datasource.username=sa",
    "spring.datasource.password=123456",
    "spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect",
    "spring.jpa.hibernate.ddl-auto=none"
})
class DatabaseManagementServiceTest {

    @Autowired
    private DatabaseManagementService databaseManagementService;

    @Test
    @DisplayName("DB Mgmt 01: Lấy trạng thái CSDL thành công")
    void testGetDatabaseStatus() {
        Map<String, Object> status = databaseManagementService.getDatabaseStatus();
        assertNotNull(status);
        assertEquals("CONNECTED", status.get("connectionStatus"));
        assertTrue((Long) status.get("totalRecords") > 0);
    }

    @Test
    @DisplayName("DB Mgmt 02: Tải lên tệp sao lưu .sql hợp lệ và kiểm tra danh sách")
    void testSaveUploadedBackup() throws Exception {
        String testContent = "-- Backup test content\nSELECT 1;\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_unit_backup.sql",
                "text/plain",
                testContent.getBytes(StandardCharsets.UTF_8)
        );

        Map<String, Object> resp = databaseManagementService.saveUploadedBackup(file, "test_admin");
        assertNotNull(resp);
        assertTrue((Boolean) resp.get("success"));
        assertEquals("test_unit_backup.sql", resp.get("filename"));

        File saved = databaseManagementService.getBackupFile("test_unit_backup.sql");
        assertNotNull(saved);
        assertTrue(saved.exists());

        // Cleanup
        saved.delete();
    }

    @Test
    @DisplayName("DB Mgmt 03: Từ chối tệp không phải .sql")
    void testRejectNonSqlFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "evil".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () ->
                databaseManagementService.saveUploadedBackup(file, "test_admin"));
    }

    @Test
    @DisplayName("DB Mgmt 04: Bảo vệ chống Path Traversal khi lấy file sao lưu")
    void testPreventPathTraversal() {
        File file = databaseManagementService.getBackupFile("../../pom.xml");
        // Must either be null or sanitize to just "pom.xml" inside database/backups
        if (file != null) {
            assertTrue(file.getPath().replace('\\', '/').contains("database/backups"));
        }
    }

    @Test
    @DisplayName("DB Mgmt 05: Tạo bản sao lưu Snapshot CSDL thực tế")
    void testCreateRealBackupSnapshot() throws Exception {
        Map<String, Object> res = databaseManagementService.createBackupSnapshot("backup_ute_sport_initial.sql", "admin");
        assertNotNull(res);
        assertTrue((Boolean) res.get("success"));
        assertEquals("backup_ute_sport_initial.sql", res.get("filename"));
        File f = databaseManagementService.getBackupFile("backup_ute_sport_initial.sql");
        assertNotNull(f);
        assertTrue(f.exists());
        assertTrue(f.length() > 0);
    }
}
