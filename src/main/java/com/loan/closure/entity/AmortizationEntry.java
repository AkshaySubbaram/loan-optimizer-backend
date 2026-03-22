package com.loan.closure.entity;

public class AmortizationEntry {

    private int month;

    private double principalPaid;

    private double interestPaid;

    private double balance;

    public AmortizationEntry(int month, double principalPaid, double interestPaid, double balance) {
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

    public double getPrincipalPaid() {
        return principalPaid;
    }

    public void setPrincipalPaid(double principalPaid) {
        this.principalPaid = principalPaid;
    }

    public double getInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(double interestPaid) {
        this.interestPaid = interestPaid;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

}
