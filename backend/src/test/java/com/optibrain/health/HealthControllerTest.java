package com.optibrain.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for health check endpoints
 * Validates that the project has real, operational functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
public class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testBasicHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("OptiBrain Backend"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testDetailedHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.database").exists())
                .andExpect(jsonPath("$.database.status").exists())
                .andExpect(jsonPath("$.jvm").exists())
                .andExpect(jsonPath("$.jvm.totalMemory").exists());
    }

    @Test
    public void testReadinessProbe() throws Exception {
        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    public void testLivenessProbe() throws Exception {
        mockMvc.perform(get("/api/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive").value(true))
                .andExpect(jsonPath("$.status").value("ALIVE"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
