package com.filesharing.controller;

import com.filesharing.backup.DataBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackupControllerTest {

    @Mock
    private DataBackupService backupService;

    @InjectMocks
    private BackupController backupController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backupController).build();
    }

    @Test
    void createFullBackup_shouldReturnSuccess() throws Exception {
        DataBackupService.BackupResult result = new DataBackupService.BackupResult();
        result.setTaskId("full_1001");
        result.setSuccess(true);
        result.setMessage("ok");
        result.setBackupPath("./backups/full_1001");

        when(backupService.createFullBackup("daily", true)).thenReturn(result);

        mockMvc.perform(post("/api/backup/full")
                .param("backupName", "daily")
                .param("includeFiles", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.taskId").value("full_1001"));
    }

    @Test
    void createBackupAsync_shouldRejectInvalidType() throws Exception {
        Map<String, Object> payload = Map.of(
            "backupName", "nightly",
            "backupType", "CUSTOM"
        );

        mockMvc.perform(post("/api/backup/async")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createBackupAsync_shouldRejectMissingSinceTimeForIncremental() throws Exception {
        Map<String, Object> payload = Map.of(
            "backupName", "inc-nightly",
            "backupType", "INCREMENTAL"
        );

        mockMvc.perform(post("/api/backup/async")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getBackupTaskStatus_shouldSupportAsyncRequestId() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("backupName", "nightly");
        payload.put("backupType", "FULL");
        payload.put("includeFiles", true);

        CompletableFuture<DataBackupService.BackupResult> pendingFuture = new CompletableFuture<>();
        when(backupService.createBackupAsync(eq("nightly"), eq("FULL"), eq(true), isNull())).thenReturn(pendingFuture);

        MvcResult createResult = mockMvc.perform(post("/api/backup/async")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.taskId").exists())
            .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        String requestId = objectMapper.readTree(createResponseBody).path("data").path("taskId").asText();

        when(backupService.getBackupTask(requestId)).thenReturn(null);

        mockMvc.perform(get("/api/backup/task/{taskId}", requestId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.taskId").value(requestId))
            .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void listBackups_shouldReturnData() throws Exception {
        DataBackupService.BackupInfo info = new DataBackupService.BackupInfo();
        info.setBackupName("daily");
        info.setBackupType("FULL");
        info.setBackupPath("./backups/daily");
        info.setCreateTime(LocalDateTime.now());
        info.setValid(true);

        when(backupService.listBackups()).thenReturn(List.of(info));

        mockMvc.perform(get("/api/backup/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].backupName").value("daily"));
    }

    @Test
    void deleteBackupByQuery_shouldCallService() throws Exception {
        String backupPath = "./backups/daily_2026-03-19T22-00-00";
        doNothing().when(backupService).deleteBackup(backupPath);

        mockMvc.perform(delete("/api/backup/delete")
                .param("backupPath", backupPath))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(backupService).deleteBackup(backupPath);
    }

    @Test
    void validateBackup_shouldReturnInvalidWhenNotFound() throws Exception {
        when(backupService.listBackups()).thenReturn(List.of());

        mockMvc.perform(post("/api/backup/validate")
                .param("backupPath", "./backups/not-found"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.isValid").value(false));
    }

    @Test
    void cleanupExpiredBackups_shouldReturnCleanupResult() throws Exception {
        DataBackupService.CleanupResult cleanupResult = new DataBackupService.CleanupResult();
        cleanupResult.setSuccess(true);
        cleanupResult.setMessage("done");
        cleanupResult.setDeletedCount(2);

        when(backupService.cleanupExpiredBackups(30)).thenReturn(cleanupResult);

        mockMvc.perform(delete("/api/backup/cleanup")
                .param("daysToKeep", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.deletedCount").value(2));
    }

    @Test
    void getBackupTaskStatus_shouldReturnBadRequestWhenMissingTask() throws Exception {
        when(backupService.getBackupTask("missing")).thenReturn(null);

        mockMvc.perform(get("/api/backup/task/missing"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
