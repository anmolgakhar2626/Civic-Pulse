package com.smartcity.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping(value = {"/", "/api"})
    public ResponseEntity<Map<String, Object>> getRootInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "CivicPulse Smart City Command Centre Backend");
        info.put("status", "UP");
        info.put("version", "1.0.0");
        info.put("frontend", "http://localhost:5173");
        info.put("health", "/actuator/health");
        info.put("documentation", "/swagger-ui.html");
        return ResponseEntity.ok(info);
    }
}
