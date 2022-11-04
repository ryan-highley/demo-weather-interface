package com.redhat.rhc.stp.ei.demo.weather;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;

import org.apache.camel.builder.RouteBuilder;

public class TransformRouteBuilder extends RouteBuilder {

    @Inject
    ConnectionFactory connectionFactory;

    @Override
    public void configure() throws Exception {
        from("kafka:weather-latest-observations-[a-z0-9]+?topicIsPattern=true")
            .routeId("weather-latest-transform-route")
            .transform().jsonpath("$.properties.rawMessage")
            .to("log:weather-latest-transform?showAll=true&multiline=true")
            .toD("amqp:topic:weather.latest.kdfw")
            ;
    }
    
}
