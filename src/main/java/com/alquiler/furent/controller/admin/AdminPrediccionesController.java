package com.alquiler.furent.controller.admin;

import com.alquiler.furent.service.PredictiveService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminPrediccionesController {

    private final PredictiveService predictiveService;

    public AdminPrediccionesController(PredictiveService predictiveService) {
        this.predictiveService = predictiveService;
    }

    @GetMapping("/predicciones")
    public String predicciones(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "60") int h,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "14") int f,
            Model model) {
        
        // Limitar máximo por seguridad/rendimiento
        if (h > 365) h = 365;
        if (f > 90) f = 90;

        Map<String, Object> data = predictiveService.generateForecasts(h, f);
        
        model.addAttribute("historyUnidades", data.get("history_unidades"));
        model.addAttribute("forecastUnidades", data.get("forecast_unidades"));
        
        model.addAttribute("historyIngresos", data.get("history_ingresos"));
        model.addAttribute("forecastIngresos", data.get("forecast_ingresos"));
        
        model.addAttribute("historyReservas", data.get("history_reservas"));
        model.addAttribute("forecastReservas", data.get("forecast_reservas"));
        model.addAttribute("j48Insights", data.get("j48_insights"));
        model.addAttribute("recommendations", data.get("recommendations"));

        model.addAttribute("h", h);
        model.addAttribute("f", f);
        model.addAttribute("activeMenu", "predicciones");
        return "admin/predicciones";
    }
}

