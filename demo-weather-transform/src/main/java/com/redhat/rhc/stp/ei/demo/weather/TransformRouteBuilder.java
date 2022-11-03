package com.redhat.rhc.stp.ei.demo.weather;

import org.apache.camel.builder.RouteBuilder;

public class TransformRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("kafka:weather-latest-observations-kdfw")
            .routeId("kdfw-transform-route")
            .transform().jsonpath("$.properties.rawMessage")
            .to("log:kdfw-transform?showAll=true&multiline=true")
            .to("amqp:topic:weather.latest.kdfw")
            ;
    }
    
}
