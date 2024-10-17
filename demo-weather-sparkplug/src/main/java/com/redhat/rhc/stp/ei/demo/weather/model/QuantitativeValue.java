package com.redhat.rhc.stp.ei.demo.weather.model;

import java.math.BigDecimal;

public class QuantitativeValue {

    public BigDecimal value;
    public BigDecimal maxValue;
    public BigDecimal minValue;
    public String unitCode;
    public String qualityControl;

    @Override
    public String toString() {
        return "QuantitativeValue [value=" + value + ", maxValue=" + maxValue + ", minValue=" + minValue
                + ", unitCode=" + unitCode + ", qualityControl=" + qualityControl + "]";
    }
}
