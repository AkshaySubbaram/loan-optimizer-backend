package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.List;

@Service
public class ExpenseStrategyService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseStrategyService.class);

    private final LoanService loanService;

    public ExpenseStrategyService(LoanService loanService) {
        this.loanService = loanService;
    }

    public List<LoanRequest> buildLoanRequestsFromExpense(ExpenseRequest req) {

        log.info("Starting expense-based loan strategy calculation. Goal={}, Risk={}",
                req.getGoal(), req.getRiskProfile());

        BigDecimal totalExpenses = calculateTotalExpenses(req.getExpenses());

        BigDecimal totalLoanEMI = req.getLoans().stream()
                .map(loan -> loanService.calculateEMI(
                        loan.getLoanAmount(),
                        loan.getInterestRate(),
                        computeRemainingTenureMonths(loan)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal disposable = getDisposable(req, totalExpenses, totalLoanEMI);

        log.info("Financial summary: Income={}, Expenses={}, EMI={}, Disposable={}",
                req.getMonthlyIncome(), totalExpenses, totalLoanEMI, disposable);

        String goal = req.getGoal() != null ? req.getGoal().toUpperCase() : "BALANCE";
        String risk = req.getRiskProfile() != null ? req.getRiskProfile().toUpperCase() : "MEDIUM";

        BigDecimal extraEmiRatio;
        BigDecimal savingsRatio;

        switch (goal) {
            case "CLOSE_FAST":
                if ("HIGH".equals(risk)) {
                    extraEmiRatio = new BigDecimal("0.7");
                    savingsRatio = new BigDecimal("0.3");
                } else if ("LOW".equals(risk)) {
                    extraEmiRatio = new BigDecimal("0.5");
                    savingsRatio = new BigDecimal("0.5");
                } else {
                    extraEmiRatio = new BigDecimal("0.6");
                    savingsRatio = new BigDecimal("0.4");
                }
                break;

            case "LOW_EMI":
                if ("HIGH".equals(risk)) {
                    extraEmiRatio = new BigDecimal("0.3");
                    savingsRatio = new BigDecimal("0.7");
                } else if ("LOW".equals(risk)) {
                    extraEmiRatio = new BigDecimal("0.1");
                    savingsRatio = new BigDecimal("0.9");
                } else {
                    extraEmiRatio = new BigDecimal("0.2");
                    savingsRatio = new BigDecimal("0.8");
                }
                break;

            case "SAVE_INTEREST":
                extraEmiRatio = "HIGH".equals(risk)
                        ? new BigDecimal("0.6")
                        : new BigDecimal("0.5");
                savingsRatio = BigDecimal.ONE.subtract(extraEmiRatio);
                break;

            default:
                if ("HIGH".equals(risk)) {
                    extraEmiRatio = new BigDecimal("0.5");
                    savingsRatio = new BigDecimal("0.5");
                } else if ("LOW".equals(risk)) {
                    extraEmiRatio = new BigDecimal("0.3");
                    savingsRatio = new BigDecimal("0.7");
                } else {
                    extraEmiRatio = new BigDecimal("0.4");
                    savingsRatio = new BigDecimal("0.6");
                }
        }

        BigDecimal totalExtraEmi = disposable.multiply(extraEmiRatio);
        BigDecimal monthlySavings = disposable.multiply(savingsRatio);

        log.debug("Strategy split: ExtraEMI={}, Savings={}", totalExtraEmi, monthlySavings);

        List<LoanInput> sortedLoans = req.getLoans().stream()
                .sorted((l1, l2) -> l2.getInterestRate().compareTo(l1.getInterestRate()))
                .toList();

        List<LoanRequest> result = getLoanRequests(sortedLoans, totalExtraEmi, monthlySavings);

        log.info("Generated {} loan strategies from expense plan", result.size());

        return result;
    }

    private BigDecimal getDisposable(ExpenseRequest req, BigDecimal totalExpenses, BigDecimal totalLoanEMI) {

        int months = (req.getEmergencyFundMonths() != null && req.getEmergencyFundMonths() > 0)
                ? req.getEmergencyFundMonths()
                : 12;

        BigDecimal remainingEmergency = safe(req.getEmergencyFundTarget())
                .subtract(safe(req.getEmergencyFund()))
                .max(BigDecimal.ZERO);

        BigDecimal monthlyEmergencyContribution =
                remainingEmergency.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        BigDecimal disposable = safe(req.getMonthlyIncome())
                .subtract(totalExpenses)
                .subtract(totalLoanEMI)
                .subtract(monthlyEmergencyContribution);

        if (disposable.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Disposable income is negative. Income={}, Expenses={}, EMI={}, Emergency={}",
                    req.getMonthlyIncome(), totalExpenses, totalLoanEMI, monthlyEmergencyContribution);

            log.warn("Disposable income is non-positive ({}). Setting disposable to zero and proceeding.", disposable);
            return BigDecimal.ZERO;
        }

        return disposable;
    }

    private List<LoanRequest> getLoanRequests(
            List<LoanInput> sortedLoans,
            BigDecimal totalExtraEmi,
            BigDecimal monthlySavings) {

        List<LoanRequest> loanRequests = new ArrayList<>();
        int loanCount = sortedLoans.size();

        Map<String, BigDecimal> loanEmiMap = new HashMap<>();
        for (LoanInput loan : sortedLoans) {
            int remainingTenure = computeRemainingTenureMonths(loan);
            if (remainingTenure > 0) {
                BigDecimal remainingPrincipal = calculateRemainingPrincipal(loan);
                BigDecimal emi = loanService.calculateEMI(remainingPrincipal, loan.getInterestRate(), remainingTenure);
                loanEmiMap.put(loan.getLoanName(), emi);
            }
        }

        for (int i = 0; i < loanCount; i++) {

            LoanInput loan = sortedLoans.get(i);
            BigDecimal loanExtraEmi;

            if (loanEmiMap.size() > 1) {
                String highestEmiLoanName = loanEmiMap.entrySet().stream()
                        .max(Comparator.comparing(Map.Entry::getValue))
                        .map(Map.Entry::getKey)
                        .orElse(null);

                if (loan.getLoanName().equals(highestEmiLoanName)) {
                    loanExtraEmi = totalExtraEmi.multiply(new BigDecimal("0.7"));
                } else {
                    loanExtraEmi = totalExtraEmi.multiply(new BigDecimal("0.3"))
                            .divide(BigDecimal.valueOf((long) loanCount - 1), 2, RoundingMode.HALF_UP);
                }
            } else {
                loanExtraEmi = totalExtraEmi;
            }

             List<BigDecimal> partPayments = new ArrayList<>();
             List<Integer> partMonths = new ArrayList<>();

            BigDecimal accumulated = BigDecimal.ZERO;

            int remainingTenure = computeRemainingTenureMonths(loan);

            if (remainingTenure <= 0) {
                log.info("Skipping loan {} as completed or no remaining tenure", loan.getLoanName());
                continue;
            }

            for (int m = 1; m <= remainingTenure; m++) {

                accumulated = accumulated.add(
                        monthlySavings.divide(BigDecimal.valueOf(loanCount), 2, RoundingMode.HALF_UP)
                );

                if (m % 6 == 0 || accumulated.compareTo(new BigDecimal("50000")) >= 0) {
                    partMonths.add(m);
                    partPayments.add(accumulated.setScale(2, RoundingMode.HALF_UP));
                    accumulated = BigDecimal.ZERO;
                }
            }

            log.debug("Loan [{}]: ExtraEMI={}, PartPaymentsCount={}",
                    loan.getLoanName(), loanExtraEmi, partPayments.size());

            BigDecimal remainingPrincipal = calculateRemainingPrincipal(loan);

            LoanRequest lr = new LoanRequest();
            lr.setLoanAmount(remainingPrincipal);
            lr.setInterestRate(loan.getInterestRate());
            lr.setTenureMonths(remainingTenure);
            lr.setExtraEmi(loanExtraEmi.setScale(2, RoundingMode.HALF_UP));
            lr.setPartPayments(partPayments);
            lr.setPartPaymentMonths(partMonths);
            lr.setLoanName(loan.getLoanName());

            loanRequests.add(lr);
        }

        return loanRequests;
    }

    private BigDecimal calculateRemainingPrincipal(LoanInput loan) {
        if (loan.getSanctionDate() == null) {
            return loan.getLoanAmount();
        }

        LocalDate sanction = loan.getSanctionDate();
        LocalDate now = LocalDate.now();

        if (now.isBefore(sanction)) {
            return loan.getLoanAmount();
        }

        Period p = Period.between(sanction, now);
        int monthsElapsed = p.getYears() * 12 + p.getMonths();
        int originalTenure = loan.getTenureMonths();

        if (monthsElapsed <= 0) {
            return loan.getLoanAmount();
        }

        BigDecimal originalEmi = loanService.calculateEMI(loan.getLoanAmount(), loan.getInterestRate(), originalTenure);
        BigDecimal monthlyRate = loan.getInterestRate().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = loan.getLoanAmount();

        for (int month = 0; month < monthsElapsed; month++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPaid = originalEmi.subtract(interest).setScale(2, RoundingMode.HALF_UP);
            remainingPrincipal = remainingPrincipal.subtract(principalPaid).setScale(2, RoundingMode.HALF_UP);

            if (remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
                remainingPrincipal = BigDecimal.ZERO;
                break;
            }
        }

        log.debug("Loan [{}]: Original Principal={}, Months Elapsed={}, Remaining Principal={}",
                loan.getLoanName(), loan.getLoanAmount(), monthsElapsed, remainingPrincipal);

        return remainingPrincipal.max(BigDecimal.ZERO);
    }

    private int computeRemainingTenureMonths(LoanInput loan) {
        if (loan.getSanctionDate() == null) return loan.getTenureMonths();

        LocalDate sanction = loan.getSanctionDate();
        LocalDate now = LocalDate.now();

        if (now.isBefore(sanction)) {
            return loan.getTenureMonths();
        }

        Period p = Period.between(sanction, now);
        int monthsElapsed = p.getYears() * 12 + p.getMonths();

        int remaining = loan.getTenureMonths() - monthsElapsed;
        return Math.max(remaining, 0);
    }

    private BigDecimal calculateTotalExpenses(List<ExpenseItem> expenses) {
        if (expenses == null) return BigDecimal.ZERO;

        return expenses.stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}