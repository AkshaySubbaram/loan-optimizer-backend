package com.loan.closure.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanResponse {

    private BigDecimal emi;

    private BigDecimal totalInterestNormal;

    private BigDecimal totalInterestWithExtra;

    private BigDecimal interestSaved;

    private int tenureReducedMonths;

    private String strategy;

    private List<AmortizationEntry> amortization;

    public BigDecimal getEmi() {
        return emi;
    }

    public void setEmi(BigDecimal emi) {
        this.emi = emi;
    }

    public BigDecimal getTotalInterestNormal() {
        return totalInterestNormal;
    }

    public void setTotalInterestNormal(BigDecimal totalInterestNormal) {
        this.totalInterestNormal = totalInterestNormal;
    }

    public BigDecimal getTotalInterestWithExtra() {
        return totalInterestWithExtra;
    }

    public void setTotalInterestWithExtra(BigDecimal totalInterestWithExtra) {
        this.totalInterestWithExtra = totalInterestWithExtra;
    }

    public BigDecimal getInterestSaved() {
        return interestSaved;
    }

    public void setInterestSaved(BigDecimal interestSaved) {
        this.interestSaved = interestSaved;
    }

    public int getTenureReducedMonths() {
        return tenureReducedMonths;
    }

    public void setTenureReducedMonths(int tenureReducedMonths) {
        this.tenureReducedMonths = tenureReducedMonths;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<AmortizationEntry> getAmortization() {
        return amortization;
    }

    public void setAmortization(List<AmortizationEntry> amortization) {
        this.amortization = amortization;
    }

}
