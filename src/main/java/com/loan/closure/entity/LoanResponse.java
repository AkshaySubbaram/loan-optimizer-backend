package com.loan.closure.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanResponse {

    private double emi;

    private double totalInterestNormal;

    private double totalInterestWithExtra;

    private double interestSaved;

    private int tenureReducedMonths;

    private String strategy;

    private List<AmortizationEntry> amortization;

    public double getEmi() {
        return emi;
    }

    public void setEmi(double emi) {
        this.emi = emi;
    }

    public double getTotalInterestNormal() {
        return totalInterestNormal;
    }

    public void setTotalInterestNormal(double totalInterestNormal) {
        this.totalInterestNormal = totalInterestNormal;
    }

    public double getTotalInterestWithExtra() {
        return totalInterestWithExtra;
    }

    public void setTotalInterestWithExtra(double totalInterestWithExtra) {
        this.totalInterestWithExtra = totalInterestWithExtra;
    }

    public double getInterestSaved() {
        return interestSaved;
    }

    public void setInterestSaved(double interestSaved) {
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
