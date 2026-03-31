package com.loan.closure.entity;

import java.util.List;

public class StrategyResult {

    private LoanResponse recommendedStrategy;

    private String reason;

    private List<LoanResponse> allStrategies;

    private StrategyAdvice advice;

    private List<String> loanPriority;

    public List<String> getLoanPriority() {
        return loanPriority;
    }

    public void setLoanPriority(List<String> loanPriority) {
        this.loanPriority = loanPriority;
    }

    public StrategyAdvice getAdvice() {
        return advice;
    }

    public void setAdvice(StrategyAdvice advice) {
        this.advice = advice;
    }

    public LoanResponse getRecommendedStrategy() {
        return recommendedStrategy;
    }

    public void setRecommendedStrategy(LoanResponse recommendedStrategy) {
        this.recommendedStrategy = recommendedStrategy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<LoanResponse> getAllStrategies() {
        return allStrategies;
    }

    public void setAllStrategies(List<LoanResponse> allStrategies) {
        this.allStrategies = allStrategies;
    }

}
