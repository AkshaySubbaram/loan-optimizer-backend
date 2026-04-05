package com.loan.closure.entity;

import java.math.BigDecimal;

public class AmortizationEntry {

    private int month;

    private BigDecimal principalPaid;

    private BigDecimal interestPaid;

    private BigDecimal balance;

    public AmortizationEntry(int month, BigDecimal principalPaid, BigDecimal interestPaid, BigDecimal balance) {
        this.month = month;
        this.principalPaid = principalPaid;
        this.interestPaid = interestPaid;
        this.balance = balance;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    public void setPrincipalPaid(BigDecimal principalPaid) {
        this.principalPaid = principalPaid;
    }

    public BigDecimal getInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(BigDecimal interestPaid) {
        this.interestPaid = interestPaid;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
