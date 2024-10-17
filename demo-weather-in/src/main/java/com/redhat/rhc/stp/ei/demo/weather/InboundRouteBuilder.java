package com.redhat.rhc.stp.ei.demo.weather;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.kafka;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.log;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.restOpenapi;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.timer;

import org.apache.camel.ExchangePattern;
import org.apache.camel.Expression;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.dsl.LogEndpointBuilderFactory.LogEndpointBuilder;
import org.apache.camel.component.kafka.KafkaConstants;

public class InboundRouteBuilder extends RouteBuilder {

    private static final String STATION_OBSERVATION_LATEST_URI = "api.weather.gov/openapi.json#station_observation_latest";
    private static final String WEATHER_LATEST_OBSERVATIONS_TOPIC_NAME = "weather-latest-observations";

    @PropertyInject(value = "weather-station", defaultValue = "kdfw")
    String weatherStation;

    Expression userAgentExpression = constant("(RHC-weather-demo, rhighley@redhat.com)");

    @Override
    public void configure() throws Exception {

        String routeId = String.format("latest-observation-route-%s", weatherStation);
        
        LogEndpointBuilder logEndpoint =
            log(String.format("%s.%s", InboundRouteBuilder.class.getName(), routeId))
            .showAll(true)
            .multiline(true)
            .level("INFO");

        LogEndpointBuilder errorLogEndpoint =
            log(logEndpoint.getUri())
            .level("ERROR");

        from(timer(routeId).repeatCount(1L).delay(-1L))
            .routeId(routeId)
            .onException(Exception.class)
                .handled(true)
                .to(errorLogEndpoint)
            .end()
            .setHeader("stationId", constant(weatherStation.toUpperCase()))
            .setHeader("User-Agent", userAgentExpression)
            .to(ExchangePattern.InOut, restOpenapi(STATION_OBSERVATION_LATEST_URI))
            .setHeader("weatherStation", constant(weatherStation))
            .setHeader(KafkaConstants.KEY, header("weatherStation"))
            .to(logEndpoint)
            .to(kafka(WEATHER_LATEST_OBSERVATIONS_TOPIC_NAME))
            ;
    }
}