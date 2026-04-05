package com.loan.closure.entity;

import java.math.BigDecimal;
import java.util.List;

public class ExpenseRequest {

    private BigDecimal monthlyIncome;

    private List<ExpenseItem> expenses;

    private List<LoanInput> loans;

    private BigDecimal emergencyFund = BigDecimal.ZERO;

    private BigDecimal emergencyFundTarget = BigDecimal.ZERO;

    private Integer emergencyFundMonths;

    private String riskProfile = "MEDIUM";

    private String goal = "BALANCE";

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public List<ExpenseItem> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<ExpenseItem> expenses) {
        this.expenses = expenses;
    }

    public List<LoanInput> getLoans() {
        return loans;
    }

    public void setLoans(List<LoanInput> loans) {
        this.loans = loans;
    }

    public BigDecimal getEmergencyFund() {
        return emergencyFund;
    }

    public void setEmergencyFund(BigDecimal emergencyFund) {
        this.emergencyFund = emergencyFund;
    }

    public BigDecimal getEmergencyFundTarget() {
        return emergencyFundTarget;
    }

    public void setEmergencyFundTarget(BigDecimal emergencyFundTarget) {
        this.emergencyFundTarget = emergencyFundTarget;
    }

    public Integer getEmergencyFundMonths() {
        return emergencyFundMonths;
    }

    public void setEmergencyFundMonths(Integer emergencyFundMonths) {
        this.emergencyFundMonths = emergencyFundMonths;
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
}
