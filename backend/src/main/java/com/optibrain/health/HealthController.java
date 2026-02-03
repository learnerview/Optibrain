package com.optibrain.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint to verify system operational status
 * Part of project verification to demonstrate real utility
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "OptiBrain Backend");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "OptiBrain Backend");
        health.put("timestamp", System.currentTimeMillis());
        
        // Check database connectivity
        Map<String, Object> database = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            database.put("status", "UP");
            database.put("type", conn.getMetaData().getDatabaseProductName());
            database.put("version", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            log.error("Database health check failed", e);
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
        }
        health.put("database", database);
        
        // Check JVM status
        Map<String, Object> jvm = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        jvm.put("totalMemory", runtime.totalMemory());
        jvm.put("freeMemory", runtime.freeMemory());
        jvm.put("maxMemory", runtime.maxMemory());
        jvm.put("processors", runtime.availableProcessors());
        health.put("jvm", jvm);
        
        // Overall status
        boolean allHealthy = database.get("status").equals("UP");
        health.put("status", allHealthy ? "UP" : "DEGRADED");
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> ready = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            ready.put("ready", true);
            ready.put("status", "READY");
            return ResponseEntity.ok(ready);
        } catch (Exception e) {
            log.error("Readiness check failed", e);
            ready.put("ready", false);
            ready.put("status", "NOT_READY");
            ready.put("error", e.getMessage());
            return ResponseEntity.status(503).body(ready);
        }
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> live = new HashMap<>();
        live.put("alive", true);
        live.put("status", "ALIVE");
        live.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(live);
    }
}
