package com.loan.closure.entity;

import java.math.BigDecimal;
import java.util.List;

public class FinancialSummary {

    private BigDecimal monthlyIncome;

    private BigDecimal totalExpenses;

    private BigDecimal totalLoanEmi;

    private BigDecimal monthlyEmergencyContribution;

    private BigDecimal disposableIncome;

    private List<PerLoanSummary> loans;

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getTotalLoanEmi() {
        return totalLoanEmi;
    }

    public void setTotalLoanEmi(BigDecimal totalLoanEmi) {
        this.totalLoanEmi = totalLoanEmi;
    }

    public BigDecimal getMonthlyEmergencyContribution() {
        return monthlyEmergencyContribution;
    }

    public void setMonthlyEmergencyContribution(BigDecimal monthlyEmergencyContribution) {
        this.monthlyEmergencyContribution = monthlyEmergencyContribution;
    }

    public BigDecimal getDisposableIncome() {
        return disposableIncome;
    }

    public void setDisposableIncome(BigDecimal disposableIncome) {
        this.disposableIncome = disposableIncome;
    }

    public List<PerLoanSummary> getLoans() {
        return loans;
    }

    public void setLoans(List<PerLoanSummary> loans) {
        this.loans = loans;
    }

    public static class PerLoanSummary {

        private String loanName;

        private BigDecimal loanAmount;

        private String sanctionDate;

        private Integer monthsSinceSanction;

        private Integer remainingTenureMonths;

        public String getLoanName() {
            return loanName;
        }

        public void setLoanName(String loanName) {
            this.loanName = loanName;
        }

        public BigDecimal getLoanAmount() {
            return loanAmount;
        }

        public void setLoanAmount(BigDecimal loanAmount) {
            this.loanAmount = loanAmount;
        }

        public String getSanctionDate() {
            return sanctionDate;
        }

        public void setSanctionDate(String sanctionDate) {
            this.sanctionDate = sanctionDate;
        }

        public Integer getMonthsSinceSanction() {
            return monthsSinceSanction;
        }

        public void setMonthsSinceSanction(Integer monthsSinceSanction) {
            this.monthsSinceSanction = monthsSinceSanction;
        }

        public Integer getRemainingTenureMonths() {
            return remainingTenureMonths;
        }

        public void setRemainingTenureMonths(Integer remainingTenureMonths) {
            this.remainingTenureMonths = remainingTenureMonths;
        }
    }

}

