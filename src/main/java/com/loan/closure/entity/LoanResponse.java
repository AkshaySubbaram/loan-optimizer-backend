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

    private String loanName;

    private java.math.BigDecimal extraEmi;

    private java.math.BigDecimal suggestedMonthlyWithExtra;

    private java.util.List<java.math.BigDecimal> partPayments;

    private java.util.List<Integer> partPaymentMonths;

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

    public String getLoanName() {
        return loanName;
    }

    public void setLoanName(String loanName) {
        this.loanName = loanName;
    }

    public java.math.BigDecimal getExtraEmi() {
        return extraEmi;
    }

    public void setExtraEmi(java.math.BigDecimal extraEmi) {
        this.extraEmi = extraEmi;
    }

    public java.math.BigDecimal getSuggestedMonthlyWithExtra() {
        return suggestedMonthlyWithExtra;
    }

    public void setSuggestedMonthlyWithExtra(java.math.BigDecimal suggestedMonthlyWithExtra) {
        this.suggestedMonthlyWithExtra = suggestedMonthlyWithExtra;
    }

    public java.util.List<java.math.BigDecimal> getPartPayments() {
        return partPayments;
    }

    public void setPartPayments(java.util.List<java.math.BigDecimal> partPayments) {
        this.partPayments = partPayments;
    }

    public java.util.List<Integer> getPartPaymentMonths() {
        return partPaymentMonths;
    }

    public void setPartPaymentMonths(java.util.List<Integer> partPaymentMonths) {
        this.partPaymentMonths = partPaymentMonths;
    }

}
