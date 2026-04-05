package com.loan.closure.service;

import com.loan.closure.entity.LoanRequest;
import com.loan.closure.entity.LoanResponse;
import com.loan.closure.entity.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoanServiceTest {

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService();
    }

    @Test
    void shouldCalculateCorrectEMI() {

        BigDecimal emi = loanService.calculateEMI(
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10),
                60
        );

        assertNotNull(emi);
        assertTrue(emi.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldHandleZeroInterest() {

        BigDecimal emi = loanService.calculateEMI(
                BigDecimal.valueOf(100000),
                BigDecimal.ZERO,
                12
        );

        assertEquals(new BigDecimal("8333.33"), emi);
    }

    @Test
    void shouldHandleLargeLoan() {

        BigDecimal emi = loanService.calculateEMI(
                new BigDecimal("100000000"),
                new BigDecimal("8.5"),
                240
        );

        assertNotNull(emi);
    }

    @Test
    void shouldCalculateTotalInterest() {

        BigDecimal interest = loanService.calculateTotalInterest(
                BigDecimal.valueOf(10000),
                60,
                BigDecimal.valueOf(500000)
        );

        assertTrue(interest.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldSimulateLoanSuccessfully() {

        SimulationResult result = loanService.simulateLoanStrategy(
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(2000),
                null,
                null,
                BigDecimal.valueOf(10),
                new ArrayList<>()
        );

        assertNotNull(result);
        assertTrue(result.getMonths() > 0);
    }

    @Test
    void shouldApplyPartPayments() {

        List<BigDecimal> payments = List.of(BigDecimal.valueOf(50000));
        List<Integer> months = List.of(6);

        SimulationResult result = loanService.simulateLoanStrategy(
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                payments,
                months,
                BigDecimal.valueOf(10),
                null
        );

        assertNotNull(result);
    }

    @Test
    void extraEmiShouldReduceTenure() {

        SimulationResult normal = loanService.simulateLoanStrategy(
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                BigDecimal.ZERO,
                null,
                null,
                BigDecimal.valueOf(10),
                null
        );

        SimulationResult faster = loanService.simulateLoanStrategy(
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(2000),
                null,
                null,
                BigDecimal.valueOf(10),
                null
        );

        assertTrue(faster.getMonths() < normal.getMonths());
    }

    @Test
    void shouldFailIfSimulationTooLong() {

        assertThrows(IllegalStateException.class, () ->
                loanService.simulateLoanStrategy(
                        BigDecimal.valueOf(1),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        BigDecimal.ZERO,
                        null
                )
        );
    }

    @Test
    void shouldReturnAllStrategies() {

        LoanRequest req = sampleLoanRequest();

        List<LoanResponse> result =
                loanService.calculateAllStrategies(req, false);

        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    void shouldGenerateAmortizationWhenEnabled() {

        LoanRequest req = sampleLoanRequest();

        List<LoanResponse> result =
                loanService.calculateAllStrategies(req, true);

        assertNotNull(result.get(0).getAmortization());
        assertFalse(result.get(0).getAmortization().isEmpty());
    }

    @Test
    void shouldFailForMismatchedPartPayments() {

        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(500000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(60);
        req.setExtraEmi(BigDecimal.ZERO);

        req.setPartPayments(List.of(BigDecimal.valueOf(10000)));
        req.setPartPaymentMonths(List.of(1, 2)); // mismatch

        assertThrows(IllegalArgumentException.class, () ->
                loanService.calculateAllStrategies(req, false)
        );
    }

    // ✅ Simple Test Data (avoid external dependency)
    private LoanRequest sampleLoanRequest() {
        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(500000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(60);
        req.setExtraEmi(BigDecimal.valueOf(2000));
        return req;
    }
}