package com.filesharing.controller;

import com.filesharing.entity.User;
import com.filesharing.service.GeoIpService;
import com.filesharing.service.ShareService;
import com.filesharing.service.UserService;
import com.filesharing.util.FileStorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShareControllerTest {

    @Mock
    private ShareService shareService;

    @Mock
    private UserService userService;

    @Mock
    private GeoIpService geoIpService;

    @Mock
    private FileStorageUtil fileStorageUtil;

    @InjectMocks
    private ShareController shareController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shareController).build();
    }

    @Test
    void getShareMonitoring_shouldReturnSuccess() throws Exception {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("tester");

        Map<String, Object> monitoring = Map.of(
                "pv", 12,
                "uv", 5,
                "uip", 3,
                "recentVisits", List.of()
        );

        when(userService.getCurrentUser(any(HttpServletRequest.class))).thenReturn(currentUser);
        when(shareService.getShareMonitoringDetails(eq(7L), eq(currentUser), eq(30))).thenReturn(monitoring);

        mockMvc.perform(get("/api/shares/7/monitoring").param("limit", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pv").value(12))
                .andExpect(jsonPath("$.data.uv").value(5))
                .andExpect(jsonPath("$.data.uip").value(3));
    }
}
