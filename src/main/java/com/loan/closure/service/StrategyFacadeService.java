package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class StrategyFacadeService {

    private static final Logger log = LoggerFactory.getLogger(StrategyFacadeService.class);

    private final LoanService loanService;
    private final ExpenseStrategyService expenseService;

    public StrategyFacadeService(LoanService loanService, ExpenseStrategyService expenseService) {
        this.loanService = loanService;
        this.expenseService = expenseService;
    }

    public StrategyResult calculateStrategy(StrategyRequest request) {

        log.info("Strategy calculation started. Mode={}",
                request.isUseIncomeStrategy() ? "INCOME_BASED" : "DIRECT_LOAN");

        if (request.isUseIncomeStrategy()) {

            ExpenseRequest expReq = request.getExpenseRequest();

            log.info("User Profile: Goal={}, Risk={}", expReq.getGoal(), expReq.getRiskProfile());

            BigDecimal totalExpenses = expReq.getExpenses().stream()
                    .map(e -> safe(e.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalLoanEMI = expReq.getLoans().stream()
                    .map(loan -> loanService.calculateEMI(
                            loan.getLoanAmount(),
                            loan.getInterestRate(),
                            loan.getTenureMonths()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthlyEmergencyContribution = getEmergencyContribution(expReq);

            BigDecimal disposable = safe(expReq.getMonthlyIncome())
                    .subtract(totalExpenses)
                    .subtract(totalLoanEMI)
                    .subtract(monthlyEmergencyContribution);

            log.info("Financial Summary: Income={}, Expenses={}, EMI={}, Emergency={}, Disposable={}",
                    expReq.getMonthlyIncome(), totalExpenses, totalLoanEMI,
                    monthlyEmergencyContribution, disposable);

            if (disposable.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Disposable income is negative. Strategy cannot proceed.");
                throw new RuntimeException("Expenses + EMIs exceed income.");
            }

            List<LoanRequest> loanRequests =
                    expenseService.buildLoanRequestsFromExpense(expReq);

            log.info("Generated {} loan requests from expense strategy", loanRequests.size());

            List<LoanResponse> allStrategies = new ArrayList<>();
            LoanResponse bestOverall = null;

            for (LoanRequest loanReq : loanRequests) {

                log.debug("Processing loan: amount={}, rate={}, tenure={}",
                        loanReq.getLoanAmount(), loanReq.getInterestRate(), loanReq.getTenureMonths());

                List<LoanResponse> strategies =
                        loanService.calculateAllStrategies(loanReq, false);

                List<LoanResponse> filtered = strategies.stream()
                        .filter(s -> isAffordable(s, loanReq, disposable, expReq))
                        .toList();

                List<LoanResponse> finalList =
                        filtered.isEmpty() ? strategies : filtered;

                LoanResponse best =
                        pickBestStrategy(finalList, expReq, loanReq);

                log.debug("Best strategy for loan: {} | InterestSaved={}",
                        best.getStrategy(), best.getInterestSaved());

                allStrategies.addAll(strategies);

                if (bestOverall == null ||
                        best.getInterestSaved().compareTo(bestOverall.getInterestSaved()) > 0) {
                    bestOverall = best;
                }
            }

            log.info("Final best strategy selected: {} | InterestSaved={}",
                    bestOverall != null ? bestOverall.getStrategy() : "NONE",
                    bestOverall != null ? bestOverall.getInterestSaved() : "0");

            StrategyResult result = new StrategyResult();
            result.setRecommendedStrategy(bestOverall);
            result.setAllStrategies(allStrategies);
            result.setReason(buildReason(expReq, totalExpenses, totalLoanEMI, monthlyEmergencyContribution));
            result.setAdvice(buildAdvice(expReq, totalExpenses, totalLoanEMI, monthlyEmergencyContribution));
            result.setLoanPriority(buildLoanPriority(expReq));

            log.info("Strategy calculation completed successfully");

            return result;
        }

        // Direct Loan Mode
        log.info("Direct loan strategy mode");

        List<LoanResponse> strategies =
                loanService.calculateAllStrategies(request.getLoanRequest(), false);

        log.info("Generated {} strategies for direct loan request", strategies.size());

        StrategyResult result = new StrategyResult();
        result.setAllStrategies(strategies);

        return result;
    }

    private BigDecimal getEmergencyContribution(ExpenseRequest expReq) {
        int months = (expReq.getEmergencyFundMonths() != null && expReq.getEmergencyFundMonths() > 0)
                ? expReq.getEmergencyFundMonths()
                : 12;

        BigDecimal remainingEmergency = safe(expReq.getEmergencyFundTarget())
                .subtract(safe(expReq.getEmergencyFund()))
                .max(BigDecimal.ZERO);

        return remainingEmergency.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    private LoanResponse pickBestStrategy(List<LoanResponse> strategies,
                                          ExpenseRequest req,
                                          LoanRequest loanReq) {

        String goal = req.getGoal() != null ? req.getGoal().toUpperCase() : "BALANCE";

        LoanResponse selected;

        switch (goal) {
            case "CLOSE_FAST":
                selected = strategies.stream()
                        .max(Comparator.comparing(LoanResponse::getTenureReducedMonths))
                        .orElse(null);
                break;

            case "SAVE_INTEREST":
                selected = strategies.stream()
                        .max(Comparator.comparing(LoanResponse::getInterestSaved))
                        .orElse(null);
                break;

            case "LOW_EMI":
                selected = strategies.stream()
                        .min(Comparator.comparing(
                                s -> s.getEmi().add(loanReq.getExtraEmi())
                        ))
                        .orElse(null);
                break;

            default:
                selected = strategies.stream()
                        .max(Comparator.comparing(
                                r -> r.getInterestSaved().multiply(new BigDecimal("0.6"))
                                        .add(BigDecimal.valueOf(r.getTenureReducedMonths()).multiply(new BigDecimal("0.4")))
                        ))
                        .orElse(null);
        }

        log.debug("Strategy picked for goal {}: {}", goal,
                selected != null ? selected.getStrategy() : "NONE");

        return selected;
    }

    private String buildReason(ExpenseRequest req,
                               BigDecimal totalExpenses,
                               BigDecimal totalLoanEMI,
                               BigDecimal monthlyEmergency) {

        BigDecimal disposable = safe(req.getMonthlyIncome())
                .subtract(totalExpenses)
                .subtract(totalLoanEMI)
                .subtract(monthlyEmergency);

        return "Based on your disposable income of ₹" + disposable.setScale(0, RoundingMode.HALF_UP) +
                ", risk profile " + req.getRiskProfile() +
                ", emergency fund ₹" + monthlyEmergency.setScale(0, RoundingMode.HALF_UP) +
                "/month, strategy optimized for goal: " + req.getGoal();
    }

    private StrategyAdvice buildAdvice(ExpenseRequest req,
                                       BigDecimal totalExpenses,
                                       BigDecimal totalLoanEMI,
                                       BigDecimal monthlyEmergency) {

        BigDecimal disposable = safe(req.getMonthlyIncome())
                .subtract(totalExpenses)
                .subtract(totalLoanEMI)
                .subtract(monthlyEmergency);

        String goal = req.getGoal() != null ? req.getGoal().toUpperCase() : "BALANCE";
        String risk = req.getRiskProfile() != null ? req.getRiskProfile().toUpperCase() : "MEDIUM";

        BigDecimal extraEmi;
        BigDecimal savings;
        String summary;

        switch (goal) {
            case "CLOSE_FAST":
                extraEmi = disposable.multiply("HIGH".equals(risk) ? new BigDecimal("0.7") : new BigDecimal("0.6"));
                savings = disposable.subtract(extraEmi);
                summary = "Aggressive repayment to close loan faster.";
                break;

            case "LOW_EMI":
                extraEmi = disposable.multiply("LOW".equals(risk) ? new BigDecimal("0.1") : new BigDecimal("0.2"));
                savings = disposable.subtract(extraEmi);
                summary = "Lower EMI, higher savings.";
                break;

            case "SAVE_INTEREST":
                extraEmi = disposable.multiply(new BigDecimal("0.5"));
                savings = disposable.subtract(extraEmi);
                summary = "Optimized for interest reduction.";
                break;

            default:
                extraEmi = disposable.multiply(new BigDecimal("0.4"));
                savings = disposable.subtract(extraEmi);
                summary = "Balanced strategy.";
        }

        StrategyAdvice advice = new StrategyAdvice();
        advice.setExtraEmiRecommended(BigDecimal.valueOf(extraEmi.setScale(0, RoundingMode.HALF_UP).longValue()));
        advice.setPartPaymentPlan(
                "Save ₹" + savings.setScale(0, RoundingMode.HALF_UP) +
                        "/month → ₹" + savings.multiply(BigDecimal.valueOf(6)).setScale(0, RoundingMode.HALF_UP)
                        + " every 6 months"
        );
        advice.setSummary(summary);

        return advice;
    }

    private boolean isAffordable(LoanResponse strategy,
                                 LoanRequest loanReq,
                                 BigDecimal disposable,
                                 ExpenseRequest expReq) {

        BigDecimal totalOutflow =
                strategy.getEmi().add(loanReq.getExtraEmi());

        String goal = expReq.getGoal() != null ? expReq.getGoal().toUpperCase() : "BALANCE";

        if ("LOW_EMI".equals(goal))
            return totalOutflow.compareTo(disposable.multiply(new BigDecimal("0.6"))) <= 0;

        if ("BALANCE".equals(goal))
            return totalOutflow.compareTo(disposable.multiply(new BigDecimal("0.8"))) <= 0;

        return totalOutflow.compareTo(disposable) <= 0;
    }

    private List<String> buildLoanPriority(ExpenseRequest req) {

        if (req.getLoans() == null || req.getLoans().isEmpty()) {
            return List.of("No loans provided");
        }

        if (req.getLoans().size() == 1) {
            return List.of(req.getLoans().get(0).getLoanName() + " - Only loan");
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
                    .sorted((l1, l2) -> l2.getInterestRate().compareTo(l1.getInterestRate()))
                    .toList();
        }

        List<String> result = new ArrayList<>();
        int rank = 1;

        for (LoanInput loan : sortedLoans) {
            result.add(rank++ + ". " + loan.getLoanName() +
                    " (" + loan.getInterestRate() + "%)");
        }

        return result;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}