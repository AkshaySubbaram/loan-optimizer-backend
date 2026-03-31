package com.loan.closure.entity;

public class LoanInput {

    private String loanName;

    private double loanAmount;

    private double interestRate;

    private int tenureMonths;

    private Integer priority; // optional (1 = highest)

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getLoanName() {
        return loanName;
    }

    public void setLoanName(String loanName) {
        this.loanName = loanName;
    }

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

}
