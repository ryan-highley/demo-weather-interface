package com.redhat.rhc.stp.ei.demo.weather;

import org.apache.camel.ExchangePattern;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.netty.http.NettyHttpOperationFailedException;

public class InboundRouteBuilder extends RouteBuilder {

    @PropertyInject(value = "weather.station", defaultValue = "kdfw")
    String weatherStation;
    
    @Override
    public void configure() throws Exception {

        from("timer:initiator?repeatCount=1&delay=-1")
            .routeId(String.format("%s-latest-observation-route", weatherStation))
            .onException(NettyHttpOperationFailedException.class)
                .handled(true)
                .toD("log:{{ weather.station }}-latest-observation-error?level=ERROR&showAll=true&multiline=true")
            .end()
            .setHeader("weatherStation", constant(weatherStation))
            .setHeader("stationId", constant(weatherStation.toUpperCase()))
            .setHeader(KafkaConstants.KEY, header("stationId"))
            .setHeader(KafkaConstants.HEADERS, constant("weatherStation=" + weatherStation))
            .setHeader("User-Agent", constant("(RHC-weather-demo, rhighley@redhat.com)"))
            .to(ExchangePattern.InOut, "rest-openapi:api.weather.gov/openapi.json#station_observation_latest")
            .to("log:demo-weather-in?showAll=true&multiline=true")
            .toD("kafka:weather-latest-observations-${properties:weather.station:kdfw}")
            ;
    }
}
