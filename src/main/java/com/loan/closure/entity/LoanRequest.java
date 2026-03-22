package com.loan.closure.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.antlr.v4.runtime.misc.NotNull;
import java.util.List;

public class LoanRequest  {

    @NotNull
    private double loanAmount;

    private double interestRate;

    private int tenureMonths;

    private double extraEmi;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Double> partPayments;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Integer> partPaymentMonths;

    private boolean includeAmortization;

    public double getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(double loanAmount) {
        this.loanAmount = loanAmount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public double getExtraEmi() {
        return extraEmi;
    }

    public void setExtraEmi(double extraEmi) {
        this.extraEmi = extraEmi;
    }

    public List<Double> getPartPayments() {
        return partPayments;
    }

    public void setPartPayments(List<Double> partPayments) {
        this.partPayments = partPayments;
    }

    public List<Integer> getPartPaymentMonths() {
        return partPaymentMonths;
    }

    public void setPartPaymentMonths(List<Integer> partPaymentMonths) {
        this.partPaymentMonths = partPaymentMonths;
    }

    public boolean isIncludeAmortization() {
        return includeAmortization;
    }

    public void setIncludeAmortization(boolean includeAmortization) {
        this.includeAmortization = includeAmortization;
    }

}
