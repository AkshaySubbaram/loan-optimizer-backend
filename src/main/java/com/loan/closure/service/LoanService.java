package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoanService {

    // 1️⃣ EMI Calculation
    public double calculateEMI(double principal, double annualRate, int tenureMonths) {
        double r = annualRate / 12 / 100;
        return (principal * r * Math.pow(1 + r, tenureMonths)) /
                (Math.pow(1 + r, tenureMonths) - 1);
    }

    // 2️⃣ Total Interest
    public double calculateTotalInterest(double emi, int months, double principal) {
        return (emi * months) - principal;
    }

    // 3️⃣ Simulation for a strategy
    public SimulationResult simulateLoanStrategy(
            double principal,
            double emi,
            double extraEmi,
            List<Double> partPayments,
            List<Integer> partPaymentMonths,
            double annualRate,
            List<AmortizationEntry> amortization
    ) {
        double r = annualRate / 12 / 100;
        int months = 0;
        double totalPaid = 0;

        Map<Integer, Double> partPaymentMap = new HashMap<>();

        if (partPayments != null && partPaymentMonths != null) {
            int size = Math.min(partPayments.size(), partPaymentMonths.size());

            for (int i = 0; i < size; i++) {
                int month = partPaymentMonths.get(i);
                double amount = partPayments.get(i);

                partPaymentMap.merge(month, amount, Double::sum);
            }
        }

        while (principal > 0) {
            double interest = principal * r;
            double payment = emi + extraEmi;

            // ✅ Optimized lookup
            if (partPaymentMap.containsKey(months + 1)) {
                payment += partPaymentMap.get(months + 1);
            }

            // Last payment adjustment
            if (payment > principal + interest) {
                payment = principal + interest;
            }

            principal = principal + interest - payment;
            totalPaid += payment;
            months++;

            if (amortization != null) {
                amortization.add(new AmortizationEntry(
                        months,
                        round(payment - interest),
                        round(interest),
                        round(principal)
                ));
            }

            if (months > 1000) break;
        }

        return new SimulationResult(months, totalPaid);
    }

    // 4️⃣ Calculate all strategies
    @Cacheable(value = "loanStrategies", key = "#request.hashCode()")
    public List<LoanResponse> calculateAllStrategies(LoanRequest req, boolean includeAmortization) {

        List<LoanResponse> strategies = new ArrayList<>();
        double emi = calculateEMI(req.getLoanAmount(), req.getInterestRate(), req.getTenureMonths());
        double normalInterest = calculateTotalInterest(emi, req.getTenureMonths(), req.getLoanAmount());

        // Strategy 1: Extra EMI Monthly
        List<AmortizationEntry> amortization1 = includeAmortization ? new ArrayList<>() : null;
        SimulationResult r1 = simulateLoanStrategy(
                req.getLoanAmount(),
                emi,
                req.getExtraEmi(),
                null,
                null,
                req.getInterestRate(),
                amortization1
        );
        LoanResponse response1 = buildResponse(r1, req, emi, normalInterest, "Extra EMI Monthly");
        response1.setAmortization(amortization1);
        strategies.add(response1);

        // Strategy 2: Part Payments
        if (req.getPartPayments() != null && !req.getPartPayments().isEmpty()) {
            List<AmortizationEntry> amortization2 = includeAmortization ? new ArrayList<>() : null;
            SimulationResult r2 = simulateLoanStrategy(
                    req.getLoanAmount(),
                    emi,
                    0,
                    req.getPartPayments(),
                    req.getPartPaymentMonths(),
                    req.getInterestRate(),
                    amortization2
            );
            LoanResponse response2 = buildResponse(r2, req, emi, normalInterest, "Part Payments");
            response2.setAmortization(amortization2);
            strategies.add(response2);
        }

        // Strategy 3: Extra EMI + Part Payments
        if (req.getExtraEmi() > 0 && req.getPartPayments() != null && !req.getPartPayments().isEmpty()) {
            List<AmortizationEntry> amortization3 = includeAmortization ? new ArrayList<>() : null;
            SimulationResult r3 = simulateLoanStrategy(
                    req.getLoanAmount(),
                    emi,
                    req.getExtraEmi(),
                    req.getPartPayments(),
                    req.getPartPaymentMonths(),
                    req.getInterestRate(),
                    amortization3
            );
            LoanResponse response3 = buildResponse(r3, req, emi, normalInterest, "Extra EMI + Part Payments");
            response3.setAmortization(amortization3);
            strategies.add(response3);
        }

        return strategies;
    }

    // 5️⃣ Helper to build LoanResponse
    private LoanResponse buildResponse(
            SimulationResult result,
            LoanRequest req,
            double emi,
            double normalInterest,
            String strategyName
    ) {
        double interestWithStrategy = result.getTotalPaid() - req.getLoanAmount();

        LoanResponse response = new LoanResponse();
        response.setStrategy(strategyName);
        response.setEmi(round(emi));
        response.setTotalInterestNormal(round(normalInterest));
        response.setTotalInterestWithExtra(round(interestWithStrategy));
        response.setInterestSaved(round(normalInterest - interestWithStrategy));
        response.setTenureReducedMonths(req.getTenureMonths() - result.getMonths());

        return response;
    }

    // 6️⃣ Utility to round values
    private double round(double value) {
        return Math.round(value);
    }
}