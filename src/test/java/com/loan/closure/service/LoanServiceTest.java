package com.loan.closure.service;

import com.loan.closure.entity.LoanRequest;
import com.loan.closure.entity.LoanResponse;
import com.loan.closure.exception.LoanCompletedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoanServiceTest {

    private final LoanService svc = new LoanService();

    @Test
    void shouldThrowWhenRemainingTenureNegative() {
        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(100000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(12);
        // sanction way in the past to make remaining negative
        req.setSanctionDate(LocalDate.of(2000, 1, 1));

        assertThrows(LoanCompletedException.class, () -> svc.calculateAllStrategies(req, false));
    }

    @Test
    void shouldReturnFinalEmiWhenOneMonthLeft() {
        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(10000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(1);
        req.setSanctionDate(LocalDate.now().minusMonths(0)); // started this month, 1 month remains

        List<LoanResponse> strategies = svc.calculateAllStrategies(req, false);

        assertNotNull(strategies);
        assertEquals(1, strategies.size());
        assertEquals("Final EMI", strategies.get(0).getStrategy());
    }

    @Test
    void shouldReturnStrategiesWhenRemainingMoreThanOne() {
        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(100000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(24);
        req.setSanctionDate(LocalDate.now());

        List<LoanResponse> strategies = svc.calculateAllStrategies(req, false);

        assertNotNull(strategies);
        assertTrue(strategies.size() >= 1);
    }
}

