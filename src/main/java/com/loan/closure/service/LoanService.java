package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    // 1️⃣ EMI Calculation
    public BigDecimal calculateEMI(BigDecimal principal,
                                   BigDecimal annualRate,
                                   Integer tenureMonths) {

        log.debug("Calculating EMI: principal={}, rate={}, tenure={}",
                principal, annualRate, tenureMonths);

        // ✅ 1. NULL VALIDATION
        if (principal == null || annualRate == null || tenureMonths == null) {
            throw new IllegalArgumentException("Principal, rate and tenure must not be null");
        }

        // ✅ 2. VALUE VALIDATION
        if (principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be greater than zero");
        }

        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("Tenure must be greater than zero");
        }

        if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }

        // ✅ 3. ZERO INTEREST HANDLING (CRITICAL FIX)
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal emi = principal.divide(
                    BigDecimal.valueOf(tenureMonths),
                    2,
                    RoundingMode.HALF_UP
            );

            log.debug("Zero interest EMI calculated={}", emi);
            return emi;
        }

        // ✅ 4. NORMAL EMI CALCULATION
        BigDecimal monthlyRate = annualRate.divide(HUNDRED, MC).divide(TWELVE, MC);

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(tenureMonths, MC);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        // ✅ 5. SAFE DIVISION CHECK
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Invalid EMI calculation: denominator is zero");
        }

        // ✅ 6. Bank-grade safe
        if (tenureMonths > 600) {
            throw new IllegalArgumentException("Tenure too large");
        }

        BigDecimal emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        log.debug("EMI calculated={}", emi);

        return emi;
    }

    // 2️⃣ Total Interest
    public BigDecimal calculateTotalInterest(BigDecimal emi, Integer months, BigDecimal principal) {

        BigDecimal interest = emi.multiply(BigDecimal.valueOf(months))
                .subtract(principal)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Total interest calculated={}", interest);

        return interest;
    }

    // 3️⃣ Simulation
    public SimulationResult simulateLoanStrategy(
            BigDecimal principal,
            BigDecimal emi,
            BigDecimal extraEmi,
            List<BigDecimal> partPayments,
            List<Integer> partPaymentMonths,
            BigDecimal annualRate,
            List<AmortizationEntry> amortization
    ) {

        log.info("Starting loan simulation: principal={}, emi={}, extraEmi={}",
                principal, emi, extraEmi);

        BigDecimal monthlyRate = annualRate
                .divide(HUNDRED, MC)
                .divide(TWELVE, MC);

        int months = 0;
        BigDecimal totalPaid = BigDecimal.ZERO;

        Map<Integer, BigDecimal> partPaymentMap = new HashMap<>();

        if (partPayments != null && partPaymentMonths != null) {
            int size = Math.min(partPayments.size(), partPaymentMonths.size());

            for (int i = 0; i < size; i++) {
                partPaymentMap.merge(
                        partPaymentMonths.get(i),
                        partPayments.get(i),
                        BigDecimal::add
                );
            }

            log.debug("Part payments loaded: {}", partPaymentMap.size());
        }

        int maxMonths = 1000;

        while (principal.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal interest = principal.multiply(monthlyRate, MC);
            BigDecimal payment = emi.add(extraEmi);

            if (partPaymentMap.containsKey(months + 1)) {
                payment = payment.add(partPaymentMap.get(months + 1));
            }

            BigDecimal totalDue = principal.add(interest);

            if (payment.compareTo(totalDue) > 0) {
                payment = totalDue;
            }

            principal = totalDue.subtract(payment);
            totalPaid = totalPaid.add(payment);
            months++;

            if (amortization != null) {
                amortization.add(new AmortizationEntry(
                        months,
                        payment.subtract(interest).setScale(2, RoundingMode.HALF_UP),
                        interest.setScale(2, RoundingMode.HALF_UP),
                        principal.setScale(2, RoundingMode.HALF_UP)
                ));
            }

            if (months > maxMonths) {
                log.error("Simulation exceeded max months limit: {}", maxMonths);
                throw new IllegalStateException("Loan simulation exceeded safe limit");
            }
        }

        log.info("Simulation completed: months={}, totalPaid={}", months, totalPaid);

        return new SimulationResult(months, totalPaid.setScale(2, RoundingMode.HALF_UP));
    }

    // 4️⃣ Calculate strategies
    @Cacheable(
            value = "loanStrategies",
            key = "#req.loanAmount + '-' + #req.interestRate + '-' + #req.tenureMonths + '-' + #req.extraEmi"
    )
    public List<LoanResponse> calculateAllStrategies(LoanRequest req, boolean includeAmortization) {

        log.info("Calculating strategies for loan: amount={}, rate={}, tenure={}, extraEmi={}",
                req.getLoanAmount(), req.getInterestRate(), req.getTenureMonths(), req.getExtraEmi());

        req.validatePartPayments();

        List<LoanResponse> strategies = new ArrayList<>();

        BigDecimal emi = calculateEMI(
                req.getLoanAmount(),
                req.getInterestRate(),
                req.getTenureMonths()
        );

        BigDecimal normalInterest = calculateTotalInterest(
                emi,
                req.getTenureMonths(),
                req.getLoanAmount()
        );

        // Strategy 1
        List<AmortizationEntry> a1 = includeAmortization ? new ArrayList<>() : null;

        SimulationResult r1 = simulateLoanStrategy(
                req.getLoanAmount(),
                emi,
                req.getExtraEmi(),
                null,
                null,
                req.getInterestRate(),
                a1
        );

        strategies.add(buildResponse(r1, req, emi, normalInterest, "Extra EMI Monthly", a1));

        // Strategy 2
        if (req.hasPartPayments()) {
            log.debug("Executing Part Payment strategy");

            List<AmortizationEntry> a2 = includeAmortization ? new ArrayList<>() : null;

            SimulationResult r2 = simulateLoanStrategy(
                    req.getLoanAmount(),
                    emi,
                    BigDecimal.ZERO,
                    req.getPartPayments(),
                    req.getPartPaymentMonths(),
                    req.getInterestRate(),
                    a2
            );

            strategies.add(buildResponse(r2, req, emi, normalInterest, "Part Payments", a2));
        }

        // Strategy 3
        if (req.getExtraEmi().compareTo(BigDecimal.ZERO) > 0 && req.hasPartPayments()) {
            log.debug("Executing Combined strategy");

            List<AmortizationEntry> a3 = includeAmortization ? new ArrayList<>() : null;

            SimulationResult r3 = simulateLoanStrategy(
                    req.getLoanAmount(),
                    emi,
                    req.getExtraEmi(),
                    req.getPartPayments(),
                    req.getPartPaymentMonths(),
                    req.getInterestRate(),
                    a3
            );

            strategies.add(buildResponse(
                    r3,
                    req,
                    emi,
                    normalInterest,
                    "Extra EMI + Part Payments",
                    a3
            ));
        }

        log.info("Total strategies generated={}", strategies.size());

        return strategies;
    }

    private LoanResponse buildResponse(
            SimulationResult result,
            LoanRequest req,
            BigDecimal emi,
            BigDecimal normalInterest,
            String strategyName,
            List<AmortizationEntry> amortization
    ) {

        BigDecimal interestWithStrategy =
                result.getTotalPaid().subtract(req.getLoanAmount());

        LoanResponse response = new LoanResponse();
        response.setStrategy(strategyName);
        response.setEmi(emi);
        response.setTotalInterestNormal(normalInterest);
        response.setTotalInterestWithExtra(interestWithStrategy);
        response.setInterestSaved(normalInterest.subtract(interestWithStrategy));
        response.setTenureReducedMonths(req.getTenureMonths() - result.getMonths());
        response.setAmortization(amortization);

        log.debug("Built response for strategy={}, interestSaved={}",
                strategyName, response.getInterestSaved());

        return response;
    }

}