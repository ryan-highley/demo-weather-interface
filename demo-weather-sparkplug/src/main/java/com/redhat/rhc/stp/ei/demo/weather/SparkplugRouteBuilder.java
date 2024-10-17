package com.redhat.rhc.stp.ei.demo.weather;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.kafka;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.dsl.LogEndpointBuilderFactory.LogEndpointBuilder;
import org.apache.camel.component.kafka.serde.KafkaHeaderDeserializer;
import org.eclipse.tahu.SparkplugInvalidTypeException;
import org.eclipse.tahu.message.model.Metric;
import org.eclipse.tahu.message.model.MetricDataType;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.message.model.SparkplugBPayload.SparkplugBPayloadBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.tahu.message.model.SparkplugBPayloadMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.rhc.stp.ei.demo.weather.model.Observation;
import com.redhat.rhc.stp.ei.demo.weather.model.QuantitativeValue;

import jakarta.enterprise.inject.Produces;

public class SparkplugRouteBuilder extends RouteBuilder {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(SparkplugRouteBuilder.class);

    private static final String WEATHER_LATEST_OBSERVATIONS_TOPIC_NAME = "weather-latest-observations";

    @BindToRegistry
    WeatherHeaderDeserializer weatherHeaderDeserializer = new WeatherHeaderDeserializer();

    @Override
    public void configure() throws Exception {

        String routeId = "weather-latest-sparkplug-route";

        LogEndpointBuilder logEndpoint =
            log(String.format("%s.%s", SparkplugRouteBuilder.class.getName(), routeId))
            .showAll(true)
            .multiline(true)
            .level("INFO");

        LogEndpointBuilder errorLogEndpoint =
            log(logEndpoint.getUri())
            .level("ERROR");

        from(kafka(WEATHER_LATEST_OBSERVATIONS_TOPIC_NAME).headerDeserializer("#weatherHeaderDeserializer"))
            .routeId(routeId)
            .onException(Exception.class)
                .handled(true)
                .to(errorLogEndpoint)
            .end()
            .transform().method(SparkplugRouteBuilder.class, "convertLatestObservation")
            // .to(logEndpoint)
            .toD("""
                tahu-edge://WX/${headers[weatherStation]}
                ?deviceIds=
                &clientId=DemoWX-${headers[weatherStation]}
                &servers=localhivemq:tcp://localhost:1883
                &metricDataTypePayloadMap=#latestObservationsPayloadMap
            """)
            ;
    }

    private static class WeatherHeaderDeserializer implements KafkaHeaderDeserializer {

        @Override
        public Object deserialize(String key, byte[] value) {
            return new String(value, StandardCharsets.UTF_8);
        }

    }

    public static SparkplugBPayload convertLatestObservation(String latestObservationResponse) throws JsonProcessingException, IOException {
        SparkplugBPayloadBuilder latestObservationsBuilder = new SparkplugBPayload.SparkplugBPayloadBuilder();

        ObjectMapper mapper = new ObjectMapper();
        
        JsonNode latestObservationProps = mapper.readTree(latestObservationResponse).get("properties");

        Observation latestObservation = mapper.readerFor(Observation.class)
            .readValue(latestObservationProps);

        latestObservationsBuilder.setBody(latestObservationProps.get("rawMessage").asText().getBytes());

        latestObservationsMetrics.entrySet().stream()
            .map(e -> createEmptyMetric(e.getKey(), e.getValue()))
            .peek(m -> {
                try {
                    Object value = latestObservationsFields.get(m.getName()).get(latestObservation);
                    if (value instanceof QuantitativeValue) {
                        MetricDataType metricDataType = m.getDataType();

                        QuantitativeValue qValue = (QuantitativeValue) value;

                        value = convertNumber(qValue.value, metricDataType);

                        metricDataType.checkType(value);
                    }

                    m.setValue(value);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            })
            .filter(m -> !m.getIsNull())
            .forEach(latestObservationsBuilder::addMetric);
            ;

        return latestObservationsBuilder.createPayload();
    }

    private static Number convertNumber(BigDecimal value, MetricDataType metricDataType) {
        if (value == null) {
            return null;
        } else if (metricDataType.equals(MetricDataType.Float)) {
            return value.floatValue();
        } else if (metricDataType.equals(MetricDataType.Double)) {
            return value.doubleValue();
        }

        BigInteger intValue = value.toBigInteger();
        Class<?> intClass = metricDataType.getClazz();
        if (intClass.isAssignableFrom(BigInteger.class)) {
            return intValue;
        } else if (intClass.isAssignableFrom(Long.class)) {
            return intValue.longValue();
        } else if (intClass.isAssignableFrom(Integer.class)) {
            return intValue.intValue();
        } else if (intClass.isAssignableFrom(Short.class)) {
            return intValue.shortValue();
        }

        return intValue.byteValue();
    }

    private static final Map<String, MetricDataType> latestObservationsMetrics = Map.ofEntries(
        Map.entry("station", MetricDataType.String),
        Map.entry("timestamp", MetricDataType.DateTime),
        Map.entry("rawMessage", MetricDataType.String),
        Map.entry("textDescription", MetricDataType.String),
        Map.entry("temperature", MetricDataType.Float),
        Map.entry("dewpoint", MetricDataType.Float),
        Map.entry("windDirection", MetricDataType.UInt8),
        Map.entry("windSpeed", MetricDataType.UInt8),
        Map.entry("windGust", MetricDataType.UInt8),
        Map.entry("barometricPressure", MetricDataType.UInt32),
        Map.entry("seaLevelPressure", MetricDataType.UInt32),
        Map.entry("visibility", MetricDataType.UInt16),
        Map.entry("maxTemperatureLast24Hours", MetricDataType.Float),
        Map.entry("minTemperatureLast24Hours", MetricDataType.Float),
        Map.entry("precipitationLastHour", MetricDataType.UInt16),
        Map.entry("precipitationLast3Hours", MetricDataType.UInt16),
        Map.entry("precipitationLast6Hours", MetricDataType.UInt16),
        Map.entry("relativeHumidity", MetricDataType.Double),
        Map.entry("windChill", MetricDataType.Float),
        Map.entry("heatIndex", MetricDataType.Float)
    );

    @SuppressWarnings("unchecked")
    private static final Map<String, Field> latestObservationsFields = Map.ofEntries(
        latestObservationsMetrics.entrySet().stream()
        .map(e -> {
            try {
                return Map.entry(e.getKey(), Observation.class.getDeclaredField(e.getKey()));
            } catch (NoSuchFieldException nsfe) {
                throw new IllegalStateException(nsfe);
            }
        })
        .toList()
        .toArray(new Map.Entry[] { Map.entry("", Observation.class.getFields()[0]) })
    );

    @Produces
    @BindToRegistry
    public SparkplugBPayloadMap latestObservationsPayloadMap() {
            var latestObservationsPayloadMapBuilder = new SparkplugBPayloadMap.SparkplugBPayloadMapBuilder();

            latestObservationsMetrics.entrySet().stream()
                .map(e -> createEmptyMetric(e.getKey(), e.getValue()))
                .forEach(latestObservationsPayloadMapBuilder::addMetric);

            return latestObservationsPayloadMapBuilder.createPayload();
    }

    private static Metric createEmptyMetric(String name, MetricDataType type) {
        try {
            return new Metric.MetricBuilder(name, type, null).createMetric();
        } catch (SparkplugInvalidTypeException site) {
            // Since the value is null, this should never happen
            throw new IllegalStateException(site);
        }
    }
}
