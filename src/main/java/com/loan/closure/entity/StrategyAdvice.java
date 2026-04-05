package com.loan.closure.entity;

import java.math.BigDecimal;

public class StrategyAdvice {

    private BigDecimal extraEmiRecommended;

    private String partPaymentPlan;

    private String summary;

    public BigDecimal getExtraEmiRecommended() {
        return extraEmiRecommended;
    }

    public void setExtraEmiRecommended(BigDecimal extraEmiRecommended) {
        this.extraEmiRecommended = extraEmiRecommended;
    }

    public String getPartPaymentPlan() {
        return partPaymentPlan;
    }

    public void setPartPaymentPlan(String partPaymentPlan) {
        this.partPaymentPlan = partPaymentPlan;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

}
