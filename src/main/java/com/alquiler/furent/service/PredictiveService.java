package com.alquiler.furent.service;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de analítica predictiva sencilla para demanda de mobiliario.
 * Calcula la cantidad total de unidades reservadas por día (sumando items)
 * y aplica una media móvil para proyectar la demanda futura.
 */
@Service
public class PredictiveService {

    private final ReservationRepository reservationRepository;

    public PredictiveService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Genera una serie histórica y una proyección simple de demanda diaria de mobiliario.
     * La métrica utilizada es el total de unidades reservadas por día
     * (suma de cantidades de todos los items de reservas activas).
     *
     * @param historyDays  número de días hacia atrás a considerar (incluye hoy)
     * @param forecastDays número de días futuros a proyectar
     * @return mapa con dos entradas:
     *  - history: LinkedHashMap&lt;String, BigDecimal&gt; fecha ISO → unidades
     *  - forecast: LinkedHashMap&lt;String, BigDecimal&gt; fecha ISO → unidades estimadas
     */
    public Map<String, Object> generateRevenueForecast(int historyDays, int forecastDays) {
        if (historyDays <= 0) {
            throw new IllegalArgumentException("historyDays debe ser mayor a 0");
        }
        if (forecastDays <= 0) {
            throw new IllegalArgumentException("forecastDays debe ser mayor a 0");
        }

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(historyDays - 1L);

        // Tomamos todas las reservas y filtramos por rango y estados relevantes
        List<Reservation> reservas = reservationRepository.findAll();

        Map<LocalDate, BigDecimal> agregados = new LinkedHashMap<>();
        for (int i = 0; i < historyDays; i++) {
            LocalDate d = from.plusDays(i);
            agregados.put(d, BigDecimal.ZERO);
        }

        for (Reservation r : reservas) {
            if (r.getFechaInicio() == null || r.getItems() == null || r.getItems().isEmpty()) {
                continue;
            }
            LocalDate dia = r.getFechaInicio();
            if (dia.isBefore(from) || dia.isAfter(today)) {
                continue;
            }
            BigDecimal totalUnidades = r.getItems().stream()
                    .map(item -> BigDecimal.valueOf(item.getCantidad()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            agregados.put(dia, agregados.getOrDefault(dia, BigDecimal.ZERO).add(totalUnidades));
        }

        // Serie histórica completa (incluyendo días en cero)
        LinkedHashMap<String, BigDecimal> history = new LinkedHashMap<>();
        for (int i = 0; i < historyDays; i++) {
            LocalDate d = from.plusDays(i);
            BigDecimal value = agregados.getOrDefault(d, BigDecimal.ZERO);
            history.put(d.toString(), value.setScale(0, RoundingMode.HALF_UP));
        }

        // Proyección usando media móvil de ventana 7 (o menos si no hay suficientes datos)
        LinkedHashMap<String, BigDecimal> forecast = new LinkedHashMap<>();
        int window = 7;
        BigDecimal[] series = history.values().toArray(new BigDecimal[0]);

        for (int i = 0; i < forecastDays; i++) {
            int count = Math.min(window, series.length + i);
            if (count == 0) {
                forecast.put(today.plusDays(i + 1L).toString(), BigDecimal.ZERO);
                continue;
            }

            BigDecimal sum = BigDecimal.ZERO;
            for (int k = 0; k < count; k++) {
                int index = series.length + i - 1 - k;
                if (index >= 0 && index < series.length) {
                    sum = sum.add(series[index]);
                } else {
                    int forecastIndex = (series.length + i - 1) - series.length - (count - 1 - k);
                    if (forecastIndex >= 0) {
                        BigDecimal projected = forecast.values().toArray(new BigDecimal[0])[forecastIndex];
                        sum = sum.add(projected);
                    }
                }
            }

            BigDecimal avg = count > 0 ? sum.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            LocalDate futureDate = today.plusDays(i + 1L);
            forecast.put(futureDate.toString(), avg.max(BigDecimal.ZERO));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("history", history);
        result.put("forecast", forecast);
        return result;
    }
}


