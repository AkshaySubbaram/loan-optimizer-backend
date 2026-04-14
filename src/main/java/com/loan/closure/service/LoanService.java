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

    // ✅ Internal rounding (2 decimals)
    private BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // ✅ Final output rounding (₹ style)
    private BigDecimal round0(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    // 1️⃣ EMI Calculation
    public BigDecimal calculateEMI(BigDecimal principal,
                                   BigDecimal annualRate,
                                   Integer tenureMonths) {

        log.debug("Calculating EMI: principal={}, rate={}, tenure={}",
                principal, annualRate, tenureMonths);

        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return round2(principal.divide(
                    BigDecimal.valueOf(tenureMonths),
                    2,
                    RoundingMode.HALF_UP
            ));
        }

        BigDecimal monthlyRate = annualRate.divide(HUNDRED, MC).divide(TWELVE, MC);

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(tenureMonths, MC);

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);

        BigDecimal emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);

        log.debug("EMI calculated={}", emi);
        return emi;
    }

    // 2️⃣ Loan Simulation (FIXED)
    public SimulationResult simulateLoanStrategy(
            BigDecimal principal,
            BigDecimal emi,
            BigDecimal extraEmi,
            List<BigDecimal> partPayments,
            List<Integer> partPaymentMonths,
            BigDecimal annualRate,
            List<AmortizationEntry> amortization
    ) {

        BigDecimal monthlyRate = annualRate.divide(HUNDRED, MC).divide(TWELVE, MC);

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
        }

        int maxMonths = 1000;

        while (principal.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal interest = round2(principal.multiply(monthlyRate, MC));
            BigDecimal totalDue = round2(principal.add(interest));

            BigDecimal payment = emi.add(extraEmi);

            if (partPaymentMap.containsKey(months + 1)) {
                payment = payment.add(partPaymentMap.get(months + 1));
            }

            if (payment.compareTo(totalDue) > 0) {
                payment = totalDue;
            }

            payment = round2(payment);

            BigDecimal principalPaid = round2(payment.subtract(interest));
            principal = round2(totalDue.subtract(payment));

            totalPaid = round2(totalPaid.add(payment));
            months++;

            if (amortization != null) {
                amortization.add(new AmortizationEntry(
                        months,
                        principalPaid,
                        interest,
                        principal
                ));
            }

            if (months > maxMonths) {
                log.error("Exceeded safe limit");
                throw new IllegalStateException("Loan simulation exceeded limit");
            }
        }

        log.debug("Simulation done: months={}, totalPaid={}", months, totalPaid);

        return new SimulationResult(months, totalPaid);
    }

    // 3️⃣ Strategy Calculation
    @Cacheable(
            value = "loanStrategies",
            key = "#req.loanAmount + '-' + #req.interestRate + '-' + #req.tenureMonths + '-' + #req.extraEmi"
    )
    public List<LoanResponse> calculateAllStrategies(LoanRequest req, boolean includeAmortization) {

        log.info("Loan calculation started: amount={}, rate={}, tenure={}, extraEmi={}",
                req.getLoanAmount(), req.getInterestRate(),
                req.getTenureMonths(), req.getExtraEmi());

        req.validatePartPayments();

        List<LoanResponse> strategies = new ArrayList<>();

        BigDecimal emi = calculateEMI(
                req.getLoanAmount(),
                req.getInterestRate(),
                req.getTenureMonths()
        );

        // ✅ NORMAL CASE FIX (simulation-based)
        SimulationResult normalSim = simulateLoanStrategy(
                req.getLoanAmount(),
                emi,
                BigDecimal.ZERO,
                null,
                null,
                req.getInterestRate(),
                null
        );

        BigDecimal normalInterest =
                round2(normalSim.getTotalPaid().subtract(req.getLoanAmount()));

        // Strategy 1: Extra EMI
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

        log.info("Loan calculation completed");

        return strategies;
    }

    // 4️⃣ Response Builder
    private LoanResponse buildResponse(
            SimulationResult result,
            LoanRequest req,
            BigDecimal emi,
            BigDecimal normalInterest,
            String strategyName,
            List<AmortizationEntry> amortization
    ) {

        BigDecimal interestWithStrategy =
                round2(result.getTotalPaid().subtract(req.getLoanAmount()));

        LoanResponse response = new LoanResponse();

        response.setStrategy(strategyName);

        // ✅ FINAL OUTPUT (INTEGER ONLY)
        response.setEmi(round0(emi));
        response.setTotalInterestNormal(round0(normalInterest));
        response.setTotalInterestWithExtra(round0(interestWithStrategy));
        response.setInterestSaved(
                round0(normalInterest.subtract(interestWithStrategy))
        );

        response.setTenureReducedMonths(
                req.getTenureMonths() - result.getMonths()
        );

        response.setAmortization(amortization);

        log.debug("Response built: {}", response);

        return response;
    }

}