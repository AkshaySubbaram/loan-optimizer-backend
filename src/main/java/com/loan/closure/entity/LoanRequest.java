package com.loan.closure.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class LoanRequest  {

    @NotNull
    @DecimalMin("1.0")
    private BigDecimal loanAmount;

    @NotNull
    @DecimalMin("0.1")
    private BigDecimal interestRate;

    @NotNull
    @Min(1)
    private int tenureMonths;

    @DecimalMin("0.0")
    private BigDecimal extraEmi;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<BigDecimal> partPayments;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Integer> partPaymentMonths;

    private boolean includeAmortization;

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getExtraEmi() {
        return extraEmi;
    }

    public void setExtraEmi(BigDecimal extraEmi) {
        this.extraEmi = extraEmi;
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

    public List<BigDecimal> getPartPayments() {
        return partPayments;
    }

    public void setPartPayments(List<BigDecimal> partPayments) {
        this.partPayments = partPayments;
    }

    public boolean hasPartPayments() {
        return partPayments != null &&
                partPaymentMonths != null &&
                !partPayments.isEmpty() &&
                partPayments.size() == partPaymentMonths.size();
    }

    public void validatePartPayments() {
        if (partPayments != null && partPaymentMonths != null) {

            if (partPayments.size() != partPaymentMonths.size()) {
                throw new IllegalArgumentException("Part payments and months size mismatch");
            }

            for (Integer month : partPaymentMonths) {
                if (month == null || month <= 0) {
                    throw new IllegalArgumentException("Part payment month must be > 0");
                }
            }
        }
    }

}
