package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StrategyFacadeService {

    private final LoanService loanService;

    private final ExpenseStrategyService expenseService;

    public StrategyFacadeService(LoanService loanService, ExpenseStrategyService expenseService) {
        this.loanService = loanService;
        this.expenseService = expenseService;
    }

    public StrategyResult calculateStrategy(StrategyRequest request) {

        if (request.isUseIncomeStrategy()) {

            ExpenseRequest expReq = request.getExpenseRequest();

            double totalExpenses = expReq.getExpenses().stream().mapToDouble(e -> e.getAmount()).sum();

            double disposable = expReq.getMonthlyIncome() - totalExpenses;

            List<LoanRequest> loanRequests = expenseService.buildLoanRequestsFromExpense(expReq);

            List<LoanResponse> allStrategies = new ArrayList<>();
            LoanResponse bestOverall = null;

            for (LoanRequest loanReq : loanRequests) {

                List<LoanResponse> strategies =
                        loanService.calculateAllStrategies(loanReq, false);

                List<LoanResponse> filtered = strategies.stream()
                        .filter(s -> isAffordable(s, loanReq, disposable, expReq))
                        .toList();

                List<LoanResponse> finalList = filtered.isEmpty() ? strategies : filtered;

                LoanResponse best = pickBestStrategy(finalList, expReq, loanReq);

                allStrategies.addAll(strategies);

                if (bestOverall == null ||
                        best.getInterestSaved() > bestOverall.getInterestSaved()) {
                    bestOverall = best;
                }
            }

            StrategyResult result = new StrategyResult();
            result.setRecommendedStrategy(bestOverall);
            result.setAllStrategies(allStrategies);
            result.setReason(buildReason(expReq));
            result.setAdvice(buildAdvice(expReq));
            result.setLoanPriority(buildLoanPriority(expReq));

            return result;
        }

        StrategyResult result = new StrategyResult();
        result.setAllStrategies(
                loanService.calculateAllStrategies(request.getLoanRequest(), false)
        );

        return result;
    }

    private LoanResponse pickBestStrategy(List<LoanResponse> strategies,
                                          ExpenseRequest req,
                                          LoanRequest loanReq) {

        String goal = req.getGoal() != null ? req.getGoal().toUpperCase() : "BALANCE";

        switch (goal) {

            case "CLOSE_FAST":
                return strategies.stream()
                        .max(Comparator.comparing(LoanResponse::getTenureReducedMonths))
                        .orElse(null);

            case "SAVE_INTEREST":
                return strategies.stream()
                        .max(Comparator.comparing(LoanResponse::getInterestSaved))
                        .orElse(null);

            case "LOW_EMI":
                return strategies.stream()
                        .min(Comparator.comparing(
                                s -> s.getEmi() + loanReq.getExtraEmi()   // 🔥 FIX
                        ))
                        .orElse(null);

            default:
                return strategies.stream()
                        .max(Comparator.comparing(
                                r -> r.getInterestSaved() * 0.6 +
                                        r.getTenureReducedMonths() * 0.4
                        ))
                        .orElse(null);
        }
    }

    private String buildReason(ExpenseRequest req) {

        double totalExpenses = req.getExpenses().stream()
                .mapToDouble(e -> e.getAmount())
                .sum();

        double disposable = req.getMonthlyIncome() - totalExpenses;

        return "Based on your disposable income of " + disposable +
                " and risk profile " + req.getRiskProfile() +
                ", this strategy is optimal for your goal: " + req.getGoal();
    }

    private StrategyAdvice buildAdvice(ExpenseRequest req) {

        double totalExpenses = req.getExpenses().stream()
                .mapToDouble(e -> e.getAmount())
                .sum();

        double disposable = req.getMonthlyIncome() - totalExpenses;

        String goal = req.getGoal() != null ? req.getGoal().toUpperCase() : "BALANCE";
        String risk = req.getRiskProfile() != null ? req.getRiskProfile().toUpperCase() : "MEDIUM";

        double extraEmi;
        double savings;
        String summary;

        if ("CLOSE_FAST".equals(goal)) {

            if ("HIGH".equals(risk)) {
                extraEmi = disposable * 0.7;
                savings = disposable * 0.3;
            } else if ("LOW".equals(risk)) {
                extraEmi = disposable * 0.5;
                savings = disposable * 0.5;
            } else {
                extraEmi = disposable * 0.6;
                savings = disposable * 0.4;
            }

            summary = "Aggressive repayment plan to close your loan faster.";

        } else if ("LOW_EMI".equals(goal)) {

            if ("HIGH".equals(risk)) {
                extraEmi = disposable * 0.3;
                savings = disposable * 0.7;
            } else if ("LOW".equals(risk)) {
                extraEmi = disposable * 0.1;
                savings = disposable * 0.9;
            } else {
                extraEmi = disposable * 0.2;
                savings = disposable * 0.8;
            }

            summary = "Lower EMI burden with higher savings for part payments.";

        } else if ("SAVE_INTEREST".equals(goal)) {

            if ("HIGH".equals(risk)) {
                extraEmi = disposable * 0.6;
                savings = disposable * 0.4;
            } else {
                extraEmi = disposable * 0.5;
                savings = disposable * 0.5;
            }

            summary = "Optimized plan to reduce total interest.";

        } else {

            if ("HIGH".equals(risk)) {
                extraEmi = disposable * 0.5;
                savings = disposable * 0.5;
            } else if ("LOW".equals(risk)) {
                extraEmi = disposable * 0.3;
                savings = disposable * 0.7;
            } else {
                extraEmi = disposable * 0.4;
                savings = disposable * 0.6;
            }

            summary = "Balanced approach between EMI and savings.";
        }

        StrategyAdvice advice = new StrategyAdvice();

        advice.setExtraEmiRecommended(Math.round(extraEmi));

        advice.setPartPaymentPlan(
                "Save ₹" + Math.round(savings) +
                        "/month and make part payment of ₹" +
                        Math.round(savings * 6) + " every 6 months"
        );

        advice.setSummary(summary);

        return advice;
    }

    private boolean isAffordable(LoanResponse strategy,
                                 LoanRequest loanReq,
                                 double disposable,
                                 ExpenseRequest expReq) {

        double totalOutflow = strategy.getEmi() + loanReq.getExtraEmi();

        String goal = expReq.getGoal().toUpperCase();

        if ("LOW_EMI".equals(goal)) {
            return totalOutflow <= disposable * 0.6; // relaxed but controlled
        }

        if ("BALANCE".equals(goal)) {
            return totalOutflow <= disposable * 0.8;
        }

        return totalOutflow <= disposable;
    }

    private List<String> buildLoanPriority(ExpenseRequest req) {

        if (req.getLoans() == null || req.getLoans().isEmpty()) {
            return List.of("No loans provided");
        }

        if (req.getLoans().size() == 1) {
            return List.of(
                    req.getLoans().get(0).getLoanName() +
                            " - Only loan, focus here"
            );
        }

        boolean hasUserPriority = req.getLoans().stream()
                .anyMatch(l -> l.getPriority() != null);

        List<LoanInput> sortedLoans;

        if (hasUserPriority) {
            sortedLoans = req.getLoans().stream()
                    .sorted(Comparator.comparing(LoanInput::getPriority))
                    .toList();
        } else {
            sortedLoans = req.getLoans().stream()
                    .sorted((l1, l2) -> Double.compare(
                            l2.getInterestRate(), l1.getInterestRate()))
                    .toList();
        }

        int rank = 1;

        List<String> result = new ArrayList<>();

        for (LoanInput loan : sortedLoans) {

            String reason;

            if (hasUserPriority) {
                reason = "User defined priority";
            } else {
                reason = "Higher interest rate";
            }

            result.add(
                    rank++ + ". " +
                            loan.getLoanName() + " → PRIORITY (" +
                            loan.getInterestRate() + "%) - " + reason
            );
        }

        return result;
    }

}
