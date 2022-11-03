package com.redhat.rhc.stp.ei.demo.weather;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.http.NettyHttpOperationFailedException;

public class InboundRouteBuilder extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {

        from("timer:initiator?repeatCount=1")
            .routeId("kdfw-latest-observation-route")
            .onException(NettyHttpOperationFailedException.class)
                .handled(true)
                .to("log:kdfw-latest-observation-error?level=ERROR&showAll=true&multiline=true")
            .end()
            .setHeader("stationId", constant("KDFW"))
            .setHeader("User-Agent", constant("(RHC-weather-demo, rhighley@redhat.com)"))
            .to(ExchangePattern.InOut, "rest-openapi:api.weather.gov/openapi.json#station_observation_latest")
            .to("log:demo-weather-in?showAll=true&multiline=true")
            .to("kafka:weather-latest-observations-kdfw")
            ;
    }
}
