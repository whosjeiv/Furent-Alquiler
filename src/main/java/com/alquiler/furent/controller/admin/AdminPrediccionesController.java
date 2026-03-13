package com.alquiler.furent.controller.admin;

import com.alquiler.furent.service.PredictiveService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminPrediccionesController {

    private final PredictiveService predictiveService;

    public AdminPrediccionesController(PredictiveService predictiveService) {
        this.predictiveService = predictiveService;
    }

    @GetMapping("/predicciones")
    public String predicciones(Model model) {
        Map<String, Object> data = predictiveService.generateRevenueForecast(60, 14);
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> history = (Map<String, BigDecimal>) data.getOrDefault("history", new LinkedHashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> forecast = (Map<String, BigDecimal>) data.getOrDefault("forecast", new LinkedHashMap<>());

        model.addAttribute("historyData", history);
        model.addAttribute("forecastData", forecast);
        model.addAttribute("activeMenu", "predicciones");
        return "admin/predicciones";
    }
}

