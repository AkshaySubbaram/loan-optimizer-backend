package com.loan.closure.service;

import com.loan.closure.entity.ExpenseItem;
import com.loan.closure.entity.ExpenseRequest;
import com.loan.closure.entity.LoanInput;
import com.loan.closure.entity.LoanRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseStrategyService {

    public List<LoanRequest> buildLoanRequestsFromExpense(ExpenseRequest req) {

        double totalExpenses = calculateTotalExpenses(req.getExpenses());
        double disposable = req.getMonthlyIncome() - totalExpenses;

        if (disposable <= 0) {
            throw new RuntimeException("Expenses exceed income.");
        }

        String goal = req.getGoal() != null ? req.getGoal().toUpperCase() : "BALANCE";
        String risk = req.getRiskProfile() != null ? req.getRiskProfile().toUpperCase() : "MEDIUM";
        
        double extraEmiRatio;
        double savingsRatio;

        if ("CLOSE_FAST".equals(goal)) {

            if ("HIGH".equals(risk)) {
                extraEmiRatio = 0.7;
                savingsRatio = 0.3;
            } else if ("LOW".equals(risk)) {
                extraEmiRatio = 0.5;
                savingsRatio = 0.5;
            } else {
                extraEmiRatio = 0.6;
                savingsRatio = 0.4;
            }

        } else if ("LOW_EMI".equals(goal)) {

            if ("HIGH".equals(risk)) {
                extraEmiRatio = 0.3;
                savingsRatio = 0.7;
            } else if ("LOW".equals(risk)) {
                extraEmiRatio = 0.1;
                savingsRatio = 0.9;
            } else {
                extraEmiRatio = 0.2;
                savingsRatio = 0.8;
            }

        } else if ("SAVE_INTEREST".equals(goal)) {

            extraEmiRatio = "HIGH".equals(risk) ? 0.6 : 0.5;
            savingsRatio = 1 - extraEmiRatio;

        } else { 

            if ("HIGH".equals(risk)) {
                extraEmiRatio = 0.5;
                savingsRatio = 0.5;
            } else if ("LOW".equals(risk)) {
                extraEmiRatio = 0.3;
                savingsRatio = 0.7;
            } else {
                extraEmiRatio = 0.4;
                savingsRatio = 0.6;
            }
        }

        double totalExtraEmi = disposable * extraEmiRatio;
        double monthlySavings = disposable * savingsRatio;
        
        List<LoanInput> sortedLoans = req.getLoans().stream()
                .sorted((l1, l2) -> Double.compare(l2.getInterestRate(), l1.getInterestRate()))
                .toList();

        List<LoanRequest> loanRequests = getLoanRequests(sortedLoans, totalExtraEmi, monthlySavings);

        return loanRequests;
    }

    private static List<LoanRequest> getLoanRequests(List<LoanInput> sortedLoans, double totalExtraEmi, double monthlySavings) {
        List<LoanRequest> loanRequests = new ArrayList<>();

        int loanCount = sortedLoans.size();

        for (int i = 0; i < loanCount; i++) {

            LoanInput loan = sortedLoans.get(i);

            double loanExtraEmi;

            if (i == 0) {
                loanExtraEmi = totalExtraEmi * 0.7; // highest priority loan
            } else {
                loanExtraEmi = totalExtraEmi * 0.3 / (loanCount - 1);
            }

            List<Double> partPayments = new ArrayList<>();
            List<Integer> partMonths = new ArrayList<>();

            double accumulated = 0;

            for (int m = 1; m <= loan.getTenureMonths(); m++) {

                accumulated += monthlySavings / loanCount;

                if (m % 6 == 0 || accumulated >= 50000) {
                    partMonths.add(m);
                    partPayments.add((double) Math.round(accumulated));
                    accumulated = 0;
                }
            }

            LoanRequest lr = new LoanRequest();
            lr.setLoanAmount(loan.getLoanAmount());
            lr.setInterestRate(loan.getInterestRate());
            lr.setTenureMonths(loan.getTenureMonths());
            lr.setExtraEmi(Math.round(loanExtraEmi));
            lr.setPartPayments(partPayments);
            lr.setPartPaymentMonths(partMonths);

            loanRequests.add(lr);
        }
        return loanRequests;
    }

    private double calculateTotalExpenses(List<ExpenseItem> expenses) {
        return expenses.stream()
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0)
                .sum();
    }

}