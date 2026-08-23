package com.alquiler.furent.service;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PredictiveServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PredictiveService predictiveService;

    @Test
    void parseJ48Tree_shouldCorrectlyParseTreeStructure() {
        String treeStr = "J48 pruned tree\n" +
                "------------------\n" +
                "\n" +
                "promedio_movil_7 <= 15\n" +
                "|   fin_semana <= 0: BAJA (10.0)\n" +
                "|   fin_semana > 0: MEDIA (4.0/1.0)\n" +
                "promedio_movil_7 > 15: ALTA (8.0)\n" +
                "\n" +
                "Number of Leaves  : \t3\n" +
                "Size of the tree : \t5";

        List<Map<String, Object>> result = PredictiveService.parseJ48Tree(treeStr);

        assertNotNull(result);
        assertEquals(2, result.size()); // Two roots: "promedio_movil_7 <= 15" and "promedio_movil_7 > 15: ALTA (8.0)"

        Map<String, Object> root1 = result.get(0);
        assertEquals("promedio_movil_7 <= 15", root1.get("condition"));
        assertFalse((Boolean) root1.get("isLeaf"));
        List<Map<String, Object>> children = (List<Map<String, Object>>) root1.get("children");
        assertEquals(2, children.size());

        Map<String, Object> leaf1 = children.get(0);
        assertEquals("fin_semana <= 0", leaf1.get("condition"));
        assertTrue((Boolean) leaf1.get("isLeaf"));
        assertEquals("BAJA", leaf1.get("classLabel"));
        assertEquals("(10.0)", leaf1.get("stats"));

        Map<String, Object> leaf2 = children.get(1);
        assertEquals("fin_semana > 0", leaf2.get("condition"));
        assertTrue((Boolean) leaf2.get("isLeaf"));
        assertEquals("MEDIA", leaf2.get("classLabel"));
        assertEquals("(4.0/1.0)", leaf2.get("stats"));

        Map<String, Object> root2 = result.get(1);
        assertEquals("promedio_movil_7 > 15", root2.get("condition"));
        assertTrue((Boolean) root2.get("isLeaf"));
        assertEquals("ALTA", root2.get("classLabel"));
        assertEquals("(8.0)", root2.get("stats"));
    }

    @Test
    void generateForecasts_withInsufficientData_shouldFallbackToMovingAverage() {
        // Less than 7 days of active reservation units (e.g. empty)
        when(reservationRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> forecasts = predictiveService.generateForecasts(30, 7);

        assertNotNull(forecasts);
        assertTrue(forecasts.containsKey("j48_insights"));
        Map<String, Object> insights = (Map<String, Object>) forecasts.get("j48_insights");
        assertTrue(insights.containsKey("error"));
        assertTrue(((String) insights.get("error")).contains("Datos insuficientes"));

        // Forecast fields should still exist and not be null (moving average returns zeroes)
        Map<String, BigDecimal> forecastUnidades = (Map<String, BigDecimal>) forecasts.get("forecast_unidades");
        assertNotNull(forecastUnidades);
        assertEquals(7, forecastUnidades.size());
    }

    @Test
    void generateForecasts_withSufficientData_shouldBuildJ48Tree() {
        // Prepare mock data: 12 days of reservations
        List<Reservation> reservations = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(15);
        for (int i = 0; i < 12; i++) {
            Reservation r = new Reservation();
            r.setFechaInicio(start.plusDays(i));
            r.setTotal(BigDecimal.valueOf(100 + i * 10));
            r.setTipoEvento("Boda");

            Reservation.ItemReserva item = new Reservation.ItemReserva();
            item.setProductoNombre("Silla");
            item.setCantidad(i % 3 == 0 ? 5 : (i % 3 == 1 ? 15 : 30)); // Variance in units to trigger classes

            r.setItems(List.of(item));
            reservations.add(r);
        }

        when(reservationRepository.findAll()).thenReturn(reservations);
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // Perform forecast generation with parameters
        Map<String, Object> forecasts = predictiveService.generateForecasts(15, 5, 0.25, 2, false, false, false, 5);

        assertNotNull(forecasts);
        Map<String, Object> insights = (Map<String, Object>) forecasts.get("j48_insights");
        assertNotNull(insights);
        assertNull(insights.get("error"));
        assertEquals("J48 (Weka)", insights.get("model"));

        Map<String, Object> evaluation = (Map<String, Object>) insights.get("evaluation");
        assertNotNull(evaluation);
        assertTrue(evaluation.containsKey("trainingAccuracy"));

        Map<String, BigDecimal> forecastUnidades = (Map<String, BigDecimal>) forecasts.get("forecast_unidades");
        assertNotNull(forecastUnidades);
        assertEquals(5, forecastUnidades.size());

        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) forecasts.get("recommendations");
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }
}
