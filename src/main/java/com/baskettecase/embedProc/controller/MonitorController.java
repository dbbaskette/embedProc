package com.baskettecase.embedProc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;
import com.baskettecase.embedProc.service.MonitorService;
import java.util.Map;

@Controller
@Profile({"local", "cloud", "scdf"})
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @GetMapping("/")
    public String index() {
        return "index.html";
    }

    @GetMapping("/api/metrics")
    @ResponseBody
    public Map<String, Object> getMetrics() {
        return monitorService.getMetrics();
    }
}