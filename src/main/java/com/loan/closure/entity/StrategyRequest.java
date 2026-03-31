package com.loan.closure.entity;

public class StrategyRequest {

    private boolean useIncomeStrategy;

    private LoanRequest loanRequest;

    private ExpenseRequest expenseRequest;

    public boolean isUseIncomeStrategy() {
        return useIncomeStrategy;
    }

    public void setUseIncomeStrategy(boolean useIncomeStrategy) {
        this.useIncomeStrategy = useIncomeStrategy;
    }

    public LoanRequest getLoanRequest() {
        return loanRequest;
    }

    public void setLoanRequest(LoanRequest loanRequest) {
        this.loanRequest = loanRequest;
    }

    public ExpenseRequest getExpenseRequest() {
        return expenseRequest;
    }

    public void setExpenseRequest(ExpenseRequest expenseRequest) {
        this.expenseRequest = expenseRequest;
    }

}
