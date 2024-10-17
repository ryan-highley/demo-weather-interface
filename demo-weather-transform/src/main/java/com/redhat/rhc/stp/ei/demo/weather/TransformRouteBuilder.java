package com.redhat.rhc.stp.ei.demo.weather;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.kafka;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.log;

import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;

import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.dsl.LogEndpointBuilderFactory.LogEndpointBuilder;
import org.apache.camel.component.kafka.serde.KafkaHeaderDeserializer;

public class TransformRouteBuilder extends RouteBuilder {

    private static final String WEATHER_LATEST_OBSERVATIONS_TOPIC_NAME = "weather-latest-observations";

    @Inject
    ConnectionFactory connectionFactory;

    @BindToRegistry
    WeatherHeaderDeserializer weatherHeaderDeserializer = new WeatherHeaderDeserializer();

    @Override
    public void configure() throws Exception {

        String routeId = "weather-latest-transform-route";

        LogEndpointBuilder logEndpoint =
            log(String.format("%s.%s", TransformRouteBuilder.class.getName(), routeId))
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
            .transform().jsonpath("$.properties.rawMessage")
            .to(logEndpoint)
            .toD("amqp:topic:weather.latest.${headers[weatherStation]}")
            ;
    }

    private static class WeatherHeaderDeserializer implements KafkaHeaderDeserializer {

        @Override
        public Object deserialize(String key, byte[] value) {
            return new String(value, StandardCharsets.UTF_8);
        }

    }
}
