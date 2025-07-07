package com.baskettecase.embedProc.controller;

import com.baskettecase.embedProc.service.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@Profile({"local", "cloud"})
public class MonitorController {

    private static final Logger logger = LoggerFactory.getLogger(MonitorController.class);
    
    private final MonitorService monitorService;

    @Autowired
    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
        logger.info("MonitorController initialized");
    }

    /**
     * Serve the monitoring UI (HTML page)
     */
    @GetMapping("/")
    public String monitoringUI(Model model) {
        MonitorService.MonitoringData data = monitorService.getMonitoringData();
        model.addAttribute("monitoringData", data);
        return "monitoring"; // Refers to monitoring.html template
    }

    /**
     * REST API endpoint for monitoring data (JSON)
     */
    @GetMapping("/api/metrics")
    @ResponseBody
    public ResponseEntity<MonitorService.MonitoringData> getMetrics() {
        try {
            MonitorService.MonitoringData data = monitorService.getMonitoringData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("Error retrieving monitoring data: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}