package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
                        loan.getTenureMonths()))
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

            throw new RuntimeException("Expenses + EMIs + emergency fund exceed income.");
        }

        return disposable;
    }

    private List<LoanRequest> getLoanRequests(
            List<LoanInput> sortedLoans,
            BigDecimal totalExtraEmi,
            BigDecimal monthlySavings) {

        List<LoanRequest> loanRequests = new ArrayList<>();
        int loanCount = sortedLoans.size();

        for (int i = 0; i < loanCount; i++) {

            LoanInput loan = sortedLoans.get(i);
            BigDecimal loanExtraEmi;

            if (i == 0) {
                loanExtraEmi = totalExtraEmi.multiply(new BigDecimal("0.7"));
            } else {
                loanExtraEmi = totalExtraEmi.multiply(new BigDecimal("0.3"))
                        .divide(BigDecimal.valueOf(loanCount - 1), 2, RoundingMode.HALF_UP);
            }

            List<BigDecimal> partPayments = new ArrayList<>();
            List<Integer> partMonths = new ArrayList<>();

            BigDecimal accumulated = BigDecimal.ZERO;

            for (int m = 1; m <= loan.getTenureMonths(); m++) {

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

            LoanRequest lr = new LoanRequest();
            lr.setLoanAmount(loan.getLoanAmount());
            lr.setInterestRate(loan.getInterestRate());
            lr.setTenureMonths(loan.getTenureMonths());
            lr.setExtraEmi(loanExtraEmi.setScale(2, RoundingMode.HALF_UP));
            lr.setPartPayments(partPayments);
            lr.setPartPaymentMonths(partMonths);

            loanRequests.add(lr);
        }

        return loanRequests;
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