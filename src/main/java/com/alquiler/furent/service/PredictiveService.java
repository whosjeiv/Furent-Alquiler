package com.alquiler.furent.service;

import com.alquiler.furent.model.Product;
import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ProductRepository;
import com.alquiler.furent.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import weka.classifiers.Evaluation;
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
    private final ProductRepository productRepository;
    private static final List<String> DEMAND_CLASSES = List.of("BAJA", "MEDIA", "ALTA");

    public PredictiveService(ReservationRepository reservationRepository, ProductRepository productRepository) {
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
    }

    /**
     * Genera una serie histórica y proyecciones con hiperparámetros por defecto.
     */
    public Map<String, Object> generateForecasts(int historyDays, int forecastDays) {
        return generateForecasts(historyDays, forecastDays, 0.25, 2, false, false, false, 10);
    }

    /**
     * Genera una serie histórica y proyecciones completas permitiendo afinar el clasificador J48 de Weka.
     */
    public Map<String, Object> generateForecasts(
            int historyDays,
            int forecastDays,
            double confidenceFactor,
            int minNumObj,
            boolean unpruned,
            boolean useLaplace,
            boolean reducedErrorPruning,
            int cvFolds) {
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
        Map<String, Object> j48Insights = new LinkedHashMap<>();
        
        result.put("history_unidades", convertToStringMap(agregadosUnidades, historyDays, from, 0));
        
        LinkedHashMap<String, BigDecimal> forecastUnidades = createJ48Forecast(
                agregadosUnidades, agregadosIngresos, agregadosReservas, forecastDays, today,
                confidenceFactor, minNumObj, unpruned, useLaplace, reducedErrorPruning, cvFolds, j48Insights);
        
        result.put("forecast_unidades", forecastUnidades);
        
        result.put("history_ingresos", convertToStringMap(agregadosIngresos, historyDays, from, 2));
        result.put("forecast_ingresos", createForecast(agregadosIngresos, forecastDays, today, 2));
        
        result.put("history_reservas", convertToStringMap(agregadosReservas, historyDays, from, 0));
        result.put("forecast_reservas", createForecast(agregadosReservas, forecastDays, today, 0));
        
        result.put("j48_insights", j48Insights);
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

    private LinkedHashMap<String, BigDecimal> createJ48Forecast(
            Map<LocalDate, BigDecimal> unidades,
            Map<LocalDate, BigDecimal> ingresos,
            Map<LocalDate, BigDecimal> reservas,
            int forecastDays,
            LocalDate today,
            double confidenceFactor,
            int minNumObj,
            boolean unpruned,
            boolean useLaplace,
            boolean reducedErrorPruning,
            int cvFolds,
            Map<String, Object> j48Insights) {
        try {
            List<BigDecimal> values = new ArrayList<>(unidades.values());
            if (values.stream().filter(v -> v.compareTo(BigDecimal.ZERO) > 0).count() < 7) {
                j48Insights.put("error", "Datos insuficientes para entrenar J48 (se requieren al menos 7 días con reservas). Mostrando proyecciones basadas en media móvil simple.");
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
            tree.setUnpruned(unpruned);
            tree.setConfidenceFactor((float) confidenceFactor);
            tree.setMinNumObj(minNumObj);
            tree.setUseLaplace(useLaplace);
            tree.setReducedErrorPruning(reducedErrorPruning);
            tree.buildClassifier(training);

            // Populate insights
            j48Insights.put("model", "J48 (Weka)");
            j48Insights.put("target", "Clasificación diaria de demanda: BAJA, MEDIA, ALTA");
            j48Insights.put("lowThreshold", BigDecimal.valueOf(lowThreshold).setScale(0, RoundingMode.HALF_UP));
            j48Insights.put("highThreshold", BigDecimal.valueOf(highThreshold).setScale(0, RoundingMode.HALF_UP));
            j48Insights.put("trainingDays", training.numInstances());
            j48Insights.put("numLeaves", (int) tree.measureNumLeaves());
            j48Insights.put("treeSize", (int) tree.measureTreeSize());
            j48Insights.put("rawTree", tree.toString());
            j48Insights.put("visualTree", parseJ48Tree(tree.toString()));

            // Hyperparameters info
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("unpruned", unpruned);
            params.put("confidenceFactor", confidenceFactor);
            params.put("minNumObj", minNumObj);
            params.put("useLaplace", useLaplace);
            params.put("reducedErrorPruning", reducedErrorPruning);
            params.put("cvFolds", cvFolds);
            j48Insights.put("parameters", params);

            // Attributes metadata
            List<Map<String, Object>> attributeList = new ArrayList<>();
            for (int k = 0; k < training.numAttributes(); k++) {
                Attribute attr = training.attribute(k);
                Map<String, Object> attrMap = new LinkedHashMap<>();
                attrMap.put("name", attr.name());
                attrMap.put("type", attr.isNumeric() ? "Numérico" : "Nominal");
                if (attr.isNominal()) {
                    List<String> valList = new ArrayList<>();
                    for (int v = 0; v < attr.numValues(); v++) {
                        valList.add(attr.value(v));
                    }
                    attrMap.put("values", valList);
                } else {
                    attrMap.put("values", null);
                }
                attributeList.add(attrMap);
            }
            j48Insights.put("attributes", attributeList);

            // Model Evaluations
            Evaluation trainEval = new Evaluation(training);
            trainEval.evaluateModel(tree, training);

            Evaluation cvEval = null;
            boolean hasCv = false;
            int actualFolds = cvFolds;
            if (training.numInstances() >= 2) {
                actualFolds = Math.max(2, Math.min(cvFolds, training.numInstances()));
                cvEval = new Evaluation(training);
                cvEval.crossValidateModel(tree, training, actualFolds, new java.util.Random(1));
                hasCv = true;
            }

            Map<String, Object> evalMap = new LinkedHashMap<>();
            evalMap.put("trainingAccuracy", trainEval.pctCorrect());
            evalMap.put("trainingCorrect", trainEval.correct());
            evalMap.put("trainingIncorrect", trainEval.incorrect());
            evalMap.put("trainingTotal", training.numInstances());
            evalMap.put("trainingKappa", trainEval.kappa());
            evalMap.put("trainingMae", trainEval.meanAbsoluteError());
            evalMap.put("trainingRmse", trainEval.rootMeanSquaredError());

            List<String> classNames = new ArrayList<>();
            for (int k = 0; k < training.numClasses(); k++) {
                classNames.add(training.classAttribute().value(k));
            }
            evalMap.put("classNames", classNames);

            if (hasCv && cvEval != null) {
                evalMap.put("hasCv", true);
                evalMap.put("cvAccuracy", cvEval.pctCorrect());
                evalMap.put("cvCorrect", cvEval.correct());
                evalMap.put("cvIncorrect", cvEval.incorrect());
                evalMap.put("cvKappa", cvEval.kappa());
                evalMap.put("cvMae", cvEval.meanAbsoluteError());
                evalMap.put("cvRmse", cvEval.rootMeanSquaredError());
                evalMap.put("cvFolds", actualFolds);
                evalMap.put("confusionMatrix", cvEval.confusionMatrix());

                List<Map<String, Object>> classDetails = new ArrayList<>();
                for (int k = 0; k < training.numClasses(); k++) {
                    Map<String, Object> classMap = new LinkedHashMap<>();
                    classMap.put("className", training.classAttribute().value(k));
                    classMap.put("precision", cvEval.precision(k));
                    classMap.put("recall", cvEval.recall(k));
                    classMap.put("fMeasure", cvEval.fMeasure(k));
                    classMap.put("truePositiveRate", cvEval.truePositiveRate(k));
                    classMap.put("falsePositiveRate", cvEval.falsePositiveRate(k));
                    classDetails.add(classMap);
                }
                evalMap.put("classDetails", classDetails);
            } else {
                evalMap.put("hasCv", false);
                evalMap.put("confusionMatrix", trainEval.confusionMatrix());

                List<Map<String, Object>> classDetails = new ArrayList<>();
                for (int k = 0; k < training.numClasses(); k++) {
                    Map<String, Object> classMap = new LinkedHashMap<>();
                    classMap.put("className", training.classAttribute().value(k));
                    classMap.put("precision", trainEval.precision(k));
                    classMap.put("recall", trainEval.recall(k));
                    classMap.put("fMeasure", trainEval.fMeasure(k));
                    classMap.put("truePositiveRate", trainEval.truePositiveRate(k));
                    classMap.put("falsePositiveRate", trainEval.falsePositiveRate(k));
                    classDetails.add(classMap);
                }
                evalMap.put("classDetails", classDetails);
            }
            j48Insights.put("evaluation", evalMap);

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
            j48Insights.put("error", "Error entrenando clasificador J48: " + ex.getMessage() + ". Mostrando proyecciones basadas en media móvil simple.");
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

    private List<Map<String, Object>> buildDynamicRecommendations(
            Map<LocalDate, BigDecimal> history,
            Map<String, BigDecimal> forecast,
            Map<String, Integer> demandaPorProducto,
            Map<String, Integer> demandaPorTipoEvento) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        BigDecimal historicalAvg = averageLast(new ArrayList<>(history.values()), Math.min(30, history.size()), 1);
        BigDecimal forecastAvg = averageLast(new ArrayList<>(forecast.values()), forecast.size(), 1);

        // 1. Recomendación por volumen de demanda
        if (forecastAvg.compareTo(historicalAvg.multiply(BigDecimal.valueOf(1.2))) > 0) {
            double percent = historicalAvg.compareTo(BigDecimal.ZERO) > 0 
                ? (forecastAvg.subtract(historicalAvg)).doubleValue() / historicalAvg.doubleValue() * 100 
                : 100.0;
            recommendations.add(recommendation(
                "Alta demanda prevista (+" + String.format("%.0f", percent) + "%)",
                "El volumen diario proyectado supera considerablemente el promedio histórico reciente. Te sugerimos reforzar personal en logística, revisar disponibilidad de camiones y ajustar las ventanas de entrega.",
                "OPERACION",
                "ALTA",
                "trending-up",
                "Ver Logística",
                "/admin/logistica"
            ));
        } else if (forecastAvg.compareTo(historicalAvg.multiply(BigDecimal.valueOf(0.8))) < 0) {
            recommendations.add(recommendation(
                "Demanda moderada a baja",
                "El modelo prevé un periodo de baja actividad en reservas. Te recomendamos aprovechar esta ventana para realizar mantenimiento al mobiliario dañado o lanzar promociones especiales.",
                "MARKETING",
                "MEDIA",
                "tag",
                "Crear Cupón",
                "/admin/cupones"
            ));
        } else {
            recommendations.add(recommendation(
                "Demanda estable y regular",
                "Los niveles previstos se alinean con tu flujo habitual de operaciones. No se requiere personal extra. Mantén los cronogramas normales y enfócate en el mantenimiento de rutina.",
                "OPERACION",
                "BAJA",
                "check-circle",
                "Ver Reservas",
                "/admin/reservas"
            ));
        }

        // 2. Recomendación por día pico estimado
        forecast.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                        recommendations.add(recommendation(
                            "Día de Carga Máxima",
                            "El " + entry.getKey() + " registrará el pico de demanda más alto con " + entry.getValue().setScale(0, RoundingMode.HALF_UP) + " unidades a entregar. Prepara los despachos el día anterior.",
                            "LOGISTICA",
                            "ALTA",
                            "calendar",
                            "Ver Agenda",
                            "/admin/logistica"
                        ));
                    }
                });

        // 3. Recomendación por producto estrella & posibles alertas de stock
        demandaPorProducto.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    String prodName = entry.getKey();
                    boolean bajoStock = false;
                    try {
                        List<Product> products = productRepository.searchProducts(prodName);
                        if (!products.isEmpty()) {
                            Product p = products.get(0);
                            if (p.isBajoStock()) {
                                bajoStock = true;
                            }
                        }
                    } catch (Exception ignored) {}

                    if (bajoStock) {
                        recommendations.add(recommendation(
                            "Alerta: Stock de Producto Estrella",
                            "El producto '" + prodName + "' es tu mobiliario más demandado, pero actualmente se encuentra por debajo de su stock mínimo de seguridad. Compra o repara unidades con urgencia.",
                            "LOGISTICA",
                            "ALTA",
                            "alert-circle",
                            "Reponer Stock",
                            "/admin/mobiliarios"
                        ));
                    } else {
                        recommendations.add(recommendation(
                            "Mobiliario más demandado",
                            "'" + prodName + "' sigue siendo tu producto estrella (" + entry.getValue() + " uds solicitadas). Asegúrate de realizar limpieza profunda al recibirlo de vuelta.",
                            "OPERACION",
                            "MEDIA",
                            "star",
                            "Ver Catálogo",
                            "/admin/mobiliarios"
                        ));
                    }
                });

        // 4. Recomendación por tipo de evento dominante
        demandaPorTipoEvento.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    recommendations.add(recommendation(
                        "Segmento Dominante: " + entry.getKey(),
                        "Los eventos tipo '" + entry.getKey() + "' concentran la mayor cantidad de piezas reservadas (" + entry.getValue() + " uds). Te sugerimos crear colecciones o combos con precios preferenciales.",
                        "PRECIO",
                        "MEDIA",
                        "package",
                        "Ver Colecciones",
                        "/admin/categorias"
                    ));
                });

        // 5. Recomendación de optimización de tarifas
        List<String> peakDays = forecast.entrySet().stream()
                .filter(e -> e.getValue().compareTo(historicalAvg.multiply(BigDecimal.valueOf(1.3))) > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!peakDays.isEmpty()) {
            recommendations.add(recommendation(
                "Oportunidad de Dynamic Pricing",
                "Se proyecta un exceso de demanda para los días: " + String.join(", ", peakDays.stream().limit(2).collect(Collectors.toList())) + ". Considera deshabilitar cupones de descuento generales en estas fechas.",
                "PRECIO",
                "MEDIA",
                "dollar-sign",
                "Gestionar Cupones",
                "/admin/cupones"
            ));
        }

        // 6. Alerta general de stock mínimo
        try {
            List<Product> stockCritico = productRepository.findAll().stream()
                .filter(Product::isBajoStock)
                .collect(Collectors.toList());
            if (!stockCritico.isEmpty()) {
                String nombres = stockCritico.stream().map(Product::getNombre).limit(3).collect(Collectors.joining(", "));
                int resto = Math.max(0, stockCritico.size() - 3);
                String listado = nombres + (resto > 0 ? " y " + resto + " más" : "");
                recommendations.add(recommendation(
                    "Alerta de Stock Crítico",
                    "Actualmente tienes " + stockCritico.size() + " mobiliarios con existencias críticas: " + listado + ". Planifica la adquisición de inventario.",
                    "LOGISTICA",
                    "ALTA",
                    "alert-triangle",
                    "Revisar Stock",
                    "/admin/mobiliarios"
                ));
            }
        } catch (Exception ignored) {}

        return recommendations;
    }

    private Map<String, Object> recommendation(String title, String detail, String type, String priority, String icon, String actionLabel, String actionUrl) {
        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("title", title);
        recommendation.put("detail", detail);
        recommendation.put("type", type);
        recommendation.put("priority", priority);
        recommendation.put("icon", icon);
        recommendation.put("actionLabel", actionLabel);
        recommendation.put("actionUrl", actionUrl);
        return recommendation;
    }

    // === PARSER DEL ARBOL J48 DE WEKA A JSON ===
    public static class DecisionTreeNode {
        public String condition;
        public boolean isLeaf;
        public String classLabel;
        public String stats;
        public List<DecisionTreeNode> children = new ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("condition", condition);
            map.put("isLeaf", isLeaf);
            map.put("classLabel", classLabel);
            map.put("stats", stats);
            List<Map<String, Object>> childMaps = new ArrayList<>();
            for (DecisionTreeNode child : children) {
                childMaps.add(child.toMap());
            }
            map.put("children", childMaps);
            return map;
        }
    }

    public static List<Map<String, Object>> parseJ48Tree(String treeStr) {
        List<DecisionTreeNode> roots = new ArrayList<>();
        if (treeStr == null || treeStr.isBlank()) return new ArrayList<>();

        String[] lines = treeStr.split("\n");
        List<DecisionTreeNode> stack = new ArrayList<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || 
                trimmedLine.startsWith("J48") || 
                trimmedLine.startsWith("Number of Leaves") || 
                trimmedLine.startsWith("Size of the tree") ||
                trimmedLine.startsWith("===") ||
                trimmedLine.startsWith("-")) {
                continue;
            }

            int depth = 0;
            String temp = line;
            while (temp.startsWith("|   ")) {
                depth++;
                temp = temp.substring(4);
            }
            while (temp.startsWith("|\t")) {
                depth++;
                temp = temp.substring(2);
            }
            temp = temp.trim();
            if (temp.isEmpty()) continue;

            DecisionTreeNode node = new DecisionTreeNode();
            if (temp.contains(":")) {
                node.isLeaf = true;
                String[] parts = temp.split(":", 2);
                node.condition = parts[0].trim();
                String right = parts[1].trim();

                if (right.contains("(")) {
                    int parenIdx = right.indexOf("(");
                    node.classLabel = right.substring(0, parenIdx).trim();
                    node.stats = right.substring(parenIdx).trim();
                } else {
                    node.classLabel = right;
                    node.stats = "";
                }
            } else {
                node.isLeaf = false;
                node.condition = temp;
            }

            if (depth == 0) {
                stack.clear();
                roots.add(node);
                stack.add(node);
            } else {
                while (stack.size() > depth) {
                    stack.remove(stack.size() - 1);
                }
                if (!stack.isEmpty()) {
                    DecisionTreeNode parent = stack.get(stack.size() - 1);
                    parent.children.add(node);
                } else {
                    roots.add(node);
                }
                stack.add(node);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (DecisionTreeNode root : roots) {
            result.add(root.toMap());
        }
        return result;
    }
}


