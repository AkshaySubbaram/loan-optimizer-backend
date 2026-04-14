package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

            final BigDecimal disposableForUse = disposable.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO
                    : disposable;

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
                        .filter(s -> isAffordable(s, loanReq, disposableForUse, expReq))
                        .toList();

                List<LoanResponse> finalList =
                        filtered.isEmpty() ? strategies : filtered;

                LoanResponse best =
                        pickBestStrategy(finalList, expReq, loanReq);

                log.debug("Best strategy for loan: {} | InterestSaved={}",
                        best != null ? best.getStrategy() : "NONE",
                        best != null ? best.getInterestSaved() : BigDecimal.ZERO);

                allStrategies.addAll(strategies);

                if (best != null) {
                    if (bestOverall == null ||
                            best.getInterestSaved().compareTo(bestOverall.getInterestSaved()) > 0) {
                        bestOverall = best;
                    } else if (best.getInterestSaved().compareTo(bestOverall.getInterestSaved()) == 0 &&
                               loanReq.getInterestRate() != null && bestOverall.getLoanName() != null) {
                        // If interest saved is equal, pick the one with higher interest rate (priority)
                        final String bestOverallLoanName = bestOverall.getLoanName();
                        LoanInput bestLoan = expReq.getLoans().stream()
                                .filter(l -> l.getLoanName().equals(bestOverallLoanName))
                                .findFirst()
                                .orElse(null);
                        if (bestLoan != null && loanReq.getInterestRate().compareTo(bestLoan.getInterestRate()) > 0) {
                            bestOverall = best;
                        }
                    }
                }
            }

            log.info("Final best strategy selected: {} | InterestSaved={}",
                    bestOverall != null ? bestOverall.getStrategy() : "NONE",
                    bestOverall != null ? bestOverall.getInterestSaved() : "0");

            StrategyResult result = new StrategyResult();

            FinancialSummary fs = new FinancialSummary();
            fs.setMonthlyIncome(safe(expReq.getMonthlyIncome()));
            fs.setTotalExpenses(totalExpenses);
            fs.setTotalLoanEmi(totalLoanEMI);
            fs.setMonthlyEmergencyContribution(monthlyEmergencyContribution);
            fs.setDisposableIncome(disposable);

            List<FinancialSummary.PerLoanSummary> loanSums = new ArrayList<>();
            for (LoanInput li : expReq.getLoans()) {
                FinancialSummary.PerLoanSummary pls = new FinancialSummary.PerLoanSummary();
                pls.setLoanName(li.getLoanName());
                pls.setLoanAmount(safe(li.getLoanAmount()));
                if (li.getSanctionDate() != null) {
                    java.time.LocalDate now = java.time.LocalDate.now();
                    java.time.LocalDate sanction = li.getSanctionDate();
                    int monthsSince = java.time.Period.between(sanction, now).getYears() * 12 + java.time.Period.between(sanction, now).getMonths();
                    pls.setMonthsSinceSanction(Math.max(monthsSince, 0));
                    int remaining = li.getTenureMonths() - Math.max(monthsSince, 0);
                    pls.setRemainingTenureMonths(Math.max(remaining, 0));
                    pls.setSanctionDate(sanction.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                } else {
                    pls.setSanctionDate(null);
                    pls.setMonthsSinceSanction(null);
                    pls.setRemainingTenureMonths(li.getTenureMonths());
                }
                loanSums.add(pls);
            }

            fs.setLoans(loanSums);

            result.setFinancialSummary(fs);
            result.setRecommendedStrategy(bestOverall);
            result.setAllStrategies(allStrategies);
            result.setReason(buildReason(expReq, totalExpenses, totalLoanEMI, monthlyEmergencyContribution));
            result.setAdvice(buildAdvice(expReq, totalExpenses, totalLoanEMI, monthlyEmergencyContribution));
            result.setLoanPriority(buildLoanPriority(expReq));

            log.info("Strategy calculation completed successfully");

            return result;
        }

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
        BigDecimal savingsForPrepayment;
        String summary;

        switch (goal) {
            case "CLOSE_FAST":
                extraEmi = disposable.multiply("HIGH".equals(risk) ? new BigDecimal("0.7") : new BigDecimal("0.6"));
                savingsForPrepayment = disposable.subtract(extraEmi);
                summary = "Aggressive repayment to close loan faster.";
                break;

            case "LOW_EMI":
                extraEmi = disposable.multiply("LOW".equals(risk) ? new BigDecimal("0.1") : new BigDecimal("0.2"));
                savingsForPrepayment = disposable.subtract(extraEmi);
                summary = "Lower EMI, higher savings for prepayment & emergencies.";
                break;

            case "SAVE_INTEREST":
                extraEmi = disposable.multiply(new BigDecimal("0.5"));
                savingsForPrepayment = disposable.subtract(extraEmi);
                summary = "Optimized for interest reduction.";
                break;

            default:
                extraEmi = disposable.multiply(new BigDecimal("0.4"));
                savingsForPrepayment = disposable.subtract(extraEmi);
                summary = "Balanced strategy.";
        }

        StrategyAdvice advice = new StrategyAdvice();
        advice.setExtraEmiRecommended(extraEmi.setScale(2, RoundingMode.HALF_UP));

        advice.setPartPaymentPlan(
                "Save ₹" + savingsForPrepayment.setScale(0, RoundingMode.HALF_UP) +
                        "/month → ₹" + savingsForPrepayment.multiply(BigDecimal.valueOf(6)).setScale(0, RoundingMode.HALF_UP)
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
            LoanInput loan = req.getLoans().get(0);
            int remainingMonths = computeRemainingTenure(loan);
            BigDecimal emi = loanService.calculateEMI(loan.getLoanAmount(), loan.getInterestRate(), remainingMonths);
            return List.of(loan.getLoanName() + " (" + loan.getInterestRate() + "%, " + remainingMonths + " months, EMI ₹" + emi.setScale(0, RoundingMode.HALF_UP) + ")");
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
            int remainingMonths = computeRemainingTenure(loan);
            BigDecimal emi = loanService.calculateEMI(loan.getLoanAmount(), loan.getInterestRate(), remainingMonths);
            result.add(rank++ + ". " + loan.getLoanName() +
                    " (" + loan.getInterestRate() + "%, " + remainingMonths + " months, EMI ₹" + emi.setScale(0, RoundingMode.HALF_UP) + ")");
        }

        return result;
    }

    private int computeRemainingTenure(LoanInput loan) {
        if (loan.getSanctionDate() == null) return loan.getTenureMonths();

        LocalDate sanction = loan.getSanctionDate();
        LocalDate now = LocalDate.now();

        if (now.isBefore(sanction)) {
            return loan.getTenureMonths();
        }

        long monthsElapsed = ChronoUnit.MONTHS.between(sanction, now);

        if (now.getDayOfMonth() < sanction.getDayOfMonth()) {
            monthsElapsed--;
        }

        int remaining = loan.getTenureMonths() - (int) monthsElapsed;
        return Math.max(remaining, 0);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
