package com.alquiler.furent.service;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de analítica predictiva sencilla para demanda de mobiliario.
 * Calcula la cantidad total de unidades reservadas por día (sumando items)
 * y aplica una media móvil para proyectar la demanda futura.
 */
@Service
public class PredictiveService {

    private final ReservationRepository reservationRepository;
    private static final List<String> DEMAND_CLASSES = List.of("BAJA", "MEDIA", "ALTA");

    public PredictiveService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Genera una serie histórica y proyecciones para:
     * - unidades (suma de cantidades de todos los items)
     * - ingresos (suma del total de las reservas)
     * - reservas (cantidad de reservas)
     */
    public Map<String, Object> generateForecasts(int historyDays, int forecastDays) {
        if (historyDays <= 0) historyDays = 60;
        if (forecastDays <= 0) forecastDays = 14;

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(historyDays - 1L);

        List<Reservation> todasReservas = reservationRepository.findAll();

        Map<LocalDate, BigDecimal> agregadosUnidades = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> agregadosIngresos = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> agregadosReservas = new LinkedHashMap<>();
        Map<String, Integer> demandaPorProducto = new HashMap<>();
        Map<String, Integer> demandaPorTipoEvento = new HashMap<>();

        for (int i = 0; i < historyDays; i++) {
            LocalDate d = from.plusDays(i);
            agregadosUnidades.put(d, BigDecimal.ZERO);
            agregadosIngresos.put(d, BigDecimal.ZERO);
            agregadosReservas.put(d, BigDecimal.ZERO);
        }

        for (Reservation r : todasReservas) {
            if (r.getFechaInicio() == null || r.getItems() == null || r.getItems().isEmpty()) continue;
            LocalDate dia = r.getFechaInicio();
            if (dia.isBefore(from) || dia.isAfter(today)) continue;

            // Unidades
            BigDecimal totalUnidades = r.getItems().stream()
                    .map(item -> BigDecimal.valueOf(item.getCantidad()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            agregadosUnidades.put(dia, agregadosUnidades.get(dia).add(totalUnidades));
            r.getItems().forEach(item -> {
                String producto = item.getProductoNombre() != null && !item.getProductoNombre().isBlank() ? item.getProductoNombre() : "Producto sin nombre";
                demandaPorProducto.merge(producto, item.getCantidad(), Integer::sum);
            });
            if (r.getTipoEvento() != null && !r.getTipoEvento().isBlank()) {
                demandaPorTipoEvento.merge(r.getTipoEvento(), totalUnidades.intValue(), Integer::sum);
            }

            // Ingresos
            BigDecimal ingresos = r.getTotal() != null ? r.getTotal() : BigDecimal.ZERO;
            agregadosIngresos.put(dia, agregadosIngresos.get(dia).add(ingresos));

            // Reservas count
            agregadosReservas.put(dia, agregadosReservas.get(dia).add(BigDecimal.ONE));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        
        result.put("history_unidades", convertToStringMap(agregadosUnidades, historyDays, from, 0));
        LinkedHashMap<String, BigDecimal> forecastUnidades = createJ48Forecast(agregadosUnidades, agregadosIngresos, agregadosReservas, forecastDays, today);
        result.put("forecast_unidades", forecastUnidades);
        
        result.put("history_ingresos", convertToStringMap(agregadosIngresos, historyDays, from, 2));
        result.put("forecast_ingresos", createForecast(agregadosIngresos, forecastDays, today, 2));
        
        result.put("history_reservas", convertToStringMap(agregadosReservas, historyDays, from, 0));
        result.put("forecast_reservas", createForecast(agregadosReservas, forecastDays, today, 0));
        result.put("j48_insights", buildJ48Insights(agregadosUnidades));
        result.put("recommendations", buildDynamicRecommendations(agregadosUnidades, forecastUnidades, demandaPorProducto, demandaPorTipoEvento));
        
        return result;
    }

    private LinkedHashMap<String, BigDecimal> convertToStringMap(Map<LocalDate, BigDecimal> agregados, int historyDays, LocalDate from, int scale) {
        LinkedHashMap<String, BigDecimal> history = new LinkedHashMap<>();
        for (int i = 0; i < historyDays; i++) {
            LocalDate d = from.plusDays(i);
            BigDecimal value = agregados.getOrDefault(d, BigDecimal.ZERO);
            history.put(d.toString(), value.setScale(scale, RoundingMode.HALF_UP));
        }
        return history;
    }

    private LinkedHashMap<String, BigDecimal> createForecast(Map<LocalDate, BigDecimal> agregados, int forecastDays, LocalDate today, int scale) {
        LinkedHashMap<String, BigDecimal> forecast = new LinkedHashMap<>();
        int window = 7;
        
        // Convert to array in chronological order (assuming agregados is already ordered, but let's be safe)
        BigDecimal[] series = agregados.values().toArray(new BigDecimal[0]);

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

            BigDecimal avg = count > 0 ? sum.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            LocalDate futureDate = today.plusDays(i + 1L);
            forecast.put(futureDate.toString(), avg.max(BigDecimal.ZERO));
        }
        return forecast;
    }

    private LinkedHashMap<String, BigDecimal> createJ48Forecast(Map<LocalDate, BigDecimal> unidades, Map<LocalDate, BigDecimal> ingresos, Map<LocalDate, BigDecimal> reservas, int forecastDays, LocalDate today) {
        try {
            List<BigDecimal> values = new ArrayList<>(unidades.values());
            if (values.stream().filter(v -> v.compareTo(BigDecimal.ZERO) > 0).count() < 7) {
                return createForecast(unidades, forecastDays, today, 0);
            }

            double lowThreshold = percentile(values, 0.33);
            double highThreshold = percentile(values, 0.66);
            Instances training = createDemandDataset("furent_daily_demand");
            List<LocalDate> dates = new ArrayList<>(unidades.keySet());

            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = dates.get(i);
                BigDecimal units = unidades.getOrDefault(date, BigDecimal.ZERO);
                Instance instance = createDemandInstance(training, date, movingAverage(values, i, 7), ingresos.getOrDefault(date, BigDecimal.ZERO).doubleValue(), reservas.getOrDefault(date, BigDecimal.ZERO).doubleValue(), demandClass(units.doubleValue(), lowThreshold, highThreshold));
                training.add(instance);
            }

            J48 tree = new J48();
            tree.setUnpruned(false);
            tree.setConfidenceFactor(0.25f);
            tree.setMinNumObj(2);
            tree.buildClassifier(training);

            LinkedHashMap<String, BigDecimal> forecast = new LinkedHashMap<>();
            BigDecimal movingIncome = averageLast(new ArrayList<>(ingresos.values()), 7, 2);
            BigDecimal movingReservations = averageLast(new ArrayList<>(reservas.values()), 7, 0);

            for (int i = 0; i < forecastDays; i++) {
                LocalDate futureDate = today.plusDays(i + 1L);
                double avg = movingAverage(values, values.size(), 7);
                Instance candidate = createDemandInstance(training, futureDate, avg, movingIncome.doubleValue(), movingReservations.doubleValue(), null);
                double classified = tree.classifyInstance(candidate);
                String predictedClass = training.classAttribute().value((int) classified);
                BigDecimal predictedUnits = averageByClass(values, lowThreshold, highThreshold, predictedClass);
                forecast.put(futureDate.toString(), predictedUnits.setScale(0, RoundingMode.HALF_UP).max(BigDecimal.ZERO));
                values.add(predictedUnits);
            }

            return forecast;
        } catch (Exception ex) {
            return createForecast(unidades, forecastDays, today, 0);
        }
    }

    private Instances createDemandDataset(String name) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("dia_semana"));
        attributes.add(new Attribute("mes"));
        attributes.add(new Attribute("fin_semana"));
        attributes.add(new Attribute("promedio_movil_7"));
        attributes.add(new Attribute("ingresos"));
        attributes.add(new Attribute("reservas"));
        attributes.add(new Attribute("demanda", new ArrayList<>(DEMAND_CLASSES)));
        Instances data = new Instances(name, attributes, 0);
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    private Instance createDemandInstance(Instances dataset, LocalDate date, double movingAverage, double income, double reservations, String demandClass) {
        double[] values = new double[dataset.numAttributes()];
        DayOfWeek day = date.getDayOfWeek();
        values[0] = day.getValue();
        values[1] = date.getMonthValue();
        values[2] = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ? 1 : 0;
        values[3] = movingAverage;
        values[4] = income;
        values[5] = reservations;
        values[6] = demandClass == null ? Utils.missingValue() : DEMAND_CLASSES.indexOf(demandClass);
        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(dataset);
        return instance;
    }

    private double percentile(List<BigDecimal> values, double percentile) {
        List<BigDecimal> positives = values.stream()
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .sorted()
                .collect(Collectors.toList());
        if (positives.isEmpty()) return 0;
        int index = Math.min(positives.size() - 1, Math.max(0, (int) Math.floor(percentile * (positives.size() - 1))));
        return positives.get(index).doubleValue();
    }

    private String demandClass(double value, double lowThreshold, double highThreshold) {
        if (value <= lowThreshold) return "BAJA";
        if (value <= highThreshold) return "MEDIA";
        return "ALTA";
    }

    private double movingAverage(List<BigDecimal> values, int endExclusive, int window) {
        int from = Math.max(0, endExclusive - window);
        List<BigDecimal> slice = values.subList(from, Math.max(from, endExclusive));
        if (slice.isEmpty()) return 0;
        return slice.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
    }

    private BigDecimal averageLast(List<BigDecimal> values, int window, int scale) {
        int from = Math.max(0, values.size() - window);
        List<BigDecimal> slice = values.subList(from, values.size());
        if (slice.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = slice.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(slice.size()), scale, RoundingMode.HALF_UP);
    }

    private BigDecimal averageByClass(List<BigDecimal> values, double lowThreshold, double highThreshold, String predictedClass) {
        List<BigDecimal> matching = values.stream()
                .filter(value -> demandClass(value.doubleValue(), lowThreshold, highThreshold).equals(predictedClass))
                .collect(Collectors.toList());
        if (matching.isEmpty()) return averageLast(values, 7, 0);
        BigDecimal sum = matching.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(matching.size()), 0, RoundingMode.HALF_UP);
    }

    private Map<String, Object> buildJ48Insights(Map<LocalDate, BigDecimal> unidades) {
        Map<String, Object> insights = new LinkedHashMap<>();
        List<BigDecimal> values = new ArrayList<>(unidades.values());
        double low = percentile(values, 0.33);
        double high = percentile(values, 0.66);
        insights.put("model", "J48 (Weka)");
        insights.put("target", "Clasificación diaria de demanda: BAJA, MEDIA, ALTA");
        insights.put("lowThreshold", BigDecimal.valueOf(low).setScale(0, RoundingMode.HALF_UP));
        insights.put("highThreshold", BigDecimal.valueOf(high).setScale(0, RoundingMode.HALF_UP));
        insights.put("trainingDays", values.size());
        return insights;
    }

    private List<Map<String, String>> buildDynamicRecommendations(Map<LocalDate, BigDecimal> history, Map<String, BigDecimal> forecast, Map<String, Integer> demandaPorProducto, Map<String, Integer> demandaPorTipoEvento) {
        List<Map<String, String>> recommendations = new ArrayList<>();
        BigDecimal historicalAvg = averageLast(new ArrayList<>(history.values()), Math.min(30, history.size()), 1);
        BigDecimal forecastAvg = averageLast(new ArrayList<>(forecast.values()), forecast.size(), 1);
        BigDecimal forecastPeak = forecast.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        if (forecastAvg.compareTo(historicalAvg.multiply(BigDecimal.valueOf(1.2))) > 0) {
            recommendations.add(recommendation("Alta demanda prevista", "Refuerza inventario, personal de logística y ventanas de entrega para los próximos días."));
        } else if (forecastAvg.compareTo(historicalAvg.multiply(BigDecimal.valueOf(0.8))) < 0) {
            recommendations.add(recommendation("Demanda moderada o baja", "Aprovecha para mantenimiento de mobiliario, promociones y optimización de rutas."));
        } else {
            recommendations.add(recommendation("Demanda estable", "Mantén la planificación operativa actual y monitorea cambios en reservas nuevas."));
        }

        forecast.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> recommendations.add(recommendation("Pico estimado", "El día " + entry.getKey() + " concentra el mayor volumen previsto con " + forecastPeak.setScale(0, RoundingMode.HALF_UP) + " unidades.")));

        demandaPorProducto.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> recommendations.add(recommendation("Producto más demandado", "Prioriza disponibilidad de " + entry.getKey() + " por su comportamiento histórico reciente.")));

        demandaPorTipoEvento.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> recommendations.add(recommendation("Tipo de evento dominante", "Ajusta paquetes y comunicación comercial hacia eventos de tipo " + entry.getKey() + ".")));

        return recommendations;
    }

    private Map<String, String> recommendation(String title, String detail) {
        Map<String, String> recommendation = new LinkedHashMap<>();
        recommendation.put("title", title);
        recommendation.put("detail", detail);
        return recommendation;
    }
}


