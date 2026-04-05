package com.loan.closure.integration;

import com.loan.closure.entity.*;
import com.loan.closure.service.StrategyFacadeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StrategyIntegrationTest {

    @Autowired
    private StrategyFacadeService strategyFacadeService;

    @Test
    @DisplayName("End-to-End: Income strategy should return best recommendation")
    void shouldCalculateStrategy_EndToEnd_IncomeMode() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);
        request.setExpenseRequest(buildExpenseRequest());

        StrategyResult result =
                strategyFacadeService.calculateStrategy(request);

        assertAll(
                () -> assertNotNull(result),
                () -> assertNotNull(result.getRecommendedStrategy()),
                () -> assertNotNull(result.getAllStrategies()),
                () -> assertFalse(result.getAllStrategies().isEmpty()),
                () -> assertNotNull(result.getAdvice()),
                () -> assertNotNull(result.getReason()),
                () -> assertNotNull(result.getLoanPriority())
        );
    }


    @Test
    @DisplayName("End-to-End: Direct loan strategy should return strategies")
    void shouldCalculateStrategy_DirectLoanMode() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(false);
        request.setLoanRequest(buildLoanRequest());

        StrategyResult result =
                strategyFacadeService.calculateStrategy(request);

        assertAll(
                () -> assertNotNull(result),
                () -> assertNotNull(result.getAllStrategies()),
                () -> assertTrue(result.getAllStrategies().size() >= 1)
        );
    }


    @Test
    @DisplayName("Should fail when disposable income is negative")
    void shouldFail_WhenDisposableIncomeNegative() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);

        ExpenseRequest exp = buildExpenseRequest();
        exp.setMonthlyIncome(BigDecimal.valueOf(1000)); // force failure

        request.setExpenseRequest(exp);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                strategyFacadeService.calculateStrategy(request)
        );

        assertTrue(ex.getMessage().contains("Expenses"));
    }

    @Test
    @DisplayName("Should handle multiple loans and return best strategy")
    void shouldHandleMultipleLoans() {

        ExpenseRequest req = buildExpenseRequest();

        LoanInput loan2 = new LoanInput();
        loan2.setLoanName("Car Loan");
        loan2.setLoanAmount(BigDecimal.valueOf(200000));
        loan2.setInterestRate(BigDecimal.valueOf(12));
        loan2.setTenureMonths(36);

        req.setLoans(List.of(req.getLoans().get(0), loan2));

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);
        request.setExpenseRequest(req);

        StrategyResult result =
                strategyFacadeService.calculateStrategy(request);

        assertNotNull(result.getRecommendedStrategy());
        assertTrue(result.getAllStrategies().size() >= 2);
    }

    private ExpenseRequest buildExpenseRequest() {

        ExpenseItem rent = new ExpenseItem();
        rent.setName("Rent");
        rent.setAmount(BigDecimal.valueOf(20000));

        ExpenseItem food = new ExpenseItem();
        food.setName("Food");
        food.setAmount(BigDecimal.valueOf(10000));

        LoanInput loan = new LoanInput();
        loan.setLoanName("Home Loan");
        loan.setLoanAmount(BigDecimal.valueOf(500000));
        loan.setInterestRate(BigDecimal.valueOf(10));
        loan.setTenureMonths(60);

        ExpenseRequest req = new ExpenseRequest();
        req.setMonthlyIncome(BigDecimal.valueOf(80000));
        req.setExpenses(List.of(rent, food));
        req.setLoans(List.of(loan));
        req.setGoal("SAVE_INTEREST");
        req.setRiskProfile("MEDIUM");
        req.setEmergencyFund(BigDecimal.valueOf(50000));
        req.setEmergencyFundTarget(BigDecimal.valueOf(200000));
        req.setEmergencyFundMonths(12);

        return req;
    }

    private LoanRequest buildLoanRequest() {

        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(500000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(60);
        req.setExtraEmi(BigDecimal.valueOf(2000));

        return req;
    }

}
