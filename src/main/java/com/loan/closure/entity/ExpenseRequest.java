package com.loan.closure.entity;

import java.util.List;

public class ExpenseRequest {

    private Double monthlyIncome;

    private List<ExpenseItem> expenses;

    private List<LoanInput> loans;

    private Double emergencyFund = 0.0;

    private String riskProfile = "MEDIUM";

    private String goal = "BALANCE";

    public Double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(Double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public List<ExpenseItem> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<ExpenseItem> expenses) {
        this.expenses = expenses;
    }

    public Double getEmergencyFund() {
        return emergencyFund;
    }

    public void setEmergencyFund(Double emergencyFund) {
        this.emergencyFund = emergencyFund;
    }

    public String getRiskProfile() {
        return riskProfile;
    }

    public void setRiskProfile(String riskProfile) {
        this.riskProfile = riskProfile;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public List<LoanInput> getLoans() {
        return loans;
    }

    public void setLoans(List<LoanInput> loans) {
        this.loans = loans;
    }

}
