package com.redhat.rhc.stp.ei.demo.weather.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Observation {

    public String station;
    public Date timestamp;
    public String rawMessage;
    public String textDescription;
    public QuantitativeValue temperature;
    public QuantitativeValue dewpoint;
    public QuantitativeValue windDirection;
    public QuantitativeValue windSpeed;
    public QuantitativeValue windGust;
    public QuantitativeValue barometricPressure;
    public QuantitativeValue seaLevelPressure;
    public QuantitativeValue visibility;
    public QuantitativeValue maxTemperatureLast24Hours;
    public QuantitativeValue minTemperatureLast24Hours;
    public QuantitativeValue precipitationLastHour;
    public QuantitativeValue precipitationLast3Hours;
    public QuantitativeValue precipitationLast6Hours;
    public QuantitativeValue relativeHumidity;
    public QuantitativeValue windChill;
    public QuantitativeValue heatIndex;

    @Override
    public String toString() {
        return "Observation [station=" + station + ", timestamp=" + timestamp + ", rawMessage=" + rawMessage
                + ", textDescription=" + textDescription + ", temperature=" + temperature + ", dewpoint=" + dewpoint
                + ", windDirection=" + windDirection + ", windSpeed=" + windSpeed + ", windGust=" + windGust
                + ", barometricPressure=" + barometricPressure + ", seaLevelPressure=" + seaLevelPressure
                + ", visibility=" + visibility + ", maxTemperatureLast24Hours=" + maxTemperatureLast24Hours
                + ", minTemperatureLast24Hours=" + minTemperatureLast24Hours + ", precipitationLastHour="
                + precipitationLastHour + ", precipitationLast3Hours=" + precipitationLast3Hours
                + ", precipitationLast6Hours=" + precipitationLast6Hours + ", relativeHumidity=" + relativeHumidity
                + ", windChill=" + windChill + ", heatIndex=" + heatIndex + "]";
    }

    
}
