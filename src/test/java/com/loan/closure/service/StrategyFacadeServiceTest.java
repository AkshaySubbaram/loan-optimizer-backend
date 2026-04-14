package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StrategyFacadeServiceTest {

    private LoanService loanService;

    private ExpenseStrategyService expenseService;

    private StrategyFacadeService facade;

    @BeforeEach
    void setup() {
        loanService = mock(LoanService.class);
        expenseService = mock(ExpenseStrategyService.class);
        facade = new StrategyFacadeService(loanService, expenseService);
    }

    // ✅ 1. Income Strategy - SUCCESS FLOW
    @Test
    void shouldExecuteIncomeStrategySuccessfully() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);

        ExpenseRequest exp = sampleExpenseRequest();
        request.setExpenseRequest(exp);

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000));

        LoanRequest loanReq = sampleLoanRequest();

        when(expenseService.buildLoanRequestsFromExpense(any()))
                .thenReturn(List.of(loanReq));

        when(loanService.calculateAllStrategies(any(), eq(false)))
                .thenReturn(sampleLoanResponses());

        StrategyResult result = facade.calculateStrategy(request);

        assertNotNull(result);
        assertNotNull(result.getRecommendedStrategy());
        assertFalse(result.getAllStrategies().isEmpty());
        assertNotNull(result.getAdvice());
        assertNotNull(result.getLoanPriority());
    }

    // ✅ 2. Direct Loan Mode
    @Test
    void shouldExecuteDirectLoanMode() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(false);
        request.setLoanRequest(sampleLoanRequest());

        when(loanService.calculateAllStrategies(any(), eq(false)))
                .thenReturn(sampleLoanResponses());

        StrategyResult result = facade.calculateStrategy(request);

        assertNotNull(result);
        assertEquals(2, result.getAllStrategies().size());
    }

    // ✅ 3. Disposable Negative → Handle Gracefully (no exception)
    @Test
    void shouldHandleNegativeDisposableGracefully() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);

        ExpenseRequest exp = sampleExpenseRequest();
        exp.setMonthlyIncome(BigDecimal.valueOf(1000));
        request.setExpenseRequest(exp);

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(50000));

        StrategyResult result = assertDoesNotThrow(
                () -> facade.calculateStrategy(request)
        );

        assertNotNull(result);
        assertNotNull(result.getFinancialSummary());
        assertTrue(result.getFinancialSummary().getDisposableIncome().compareTo(BigDecimal.ZERO) <= 0);
    }

    // ✅ 4. Best Strategy Selection
    @Test
    void shouldSelectBestStrategyBasedOnInterestSaved() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);
        request.setExpenseRequest(sampleExpenseRequest());

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000));

        LoanRequest loanReq = sampleLoanRequest();

        when(expenseService.buildLoanRequestsFromExpense(any()))
                .thenReturn(List.of(loanReq));

        when(loanService.calculateAllStrategies(any(), eq(false)))
                .thenReturn(sampleLoanResponses());

        StrategyResult result = facade.calculateStrategy(request);

        assertEquals("Extra EMI", result.getRecommendedStrategy().getStrategy());
    }

    // ✅ 5. Loan Priority Logic
    @Test
    void shouldBuildLoanPriorityCorrectly() {

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);
        request.setExpenseRequest(sampleExpenseRequest());

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000));

        when(expenseService.buildLoanRequestsFromExpense(any()))
                .thenReturn(List.of(sampleLoanRequest()));

        when(loanService.calculateAllStrategies(any(), eq(false)))
                .thenReturn(sampleLoanResponses());

        StrategyResult result = facade.calculateStrategy(request);

        assertFalse(result.getLoanPriority().isEmpty());
    }

    private ExpenseRequest sampleExpenseRequest() {

        ExpenseRequest req = new ExpenseRequest();

        req.setMonthlyIncome(BigDecimal.valueOf(100000));

        req.setExpenses(List.of(
                new ExpenseItem("Rent", BigDecimal.valueOf(20000)),
                new ExpenseItem("Food", BigDecimal.valueOf(10000))
        ));

        req.setLoans(List.of(sampleLoanInput()));

        req.setEmergencyFund(BigDecimal.valueOf(20000));
        req.setEmergencyFundTarget(BigDecimal.valueOf(100000));
        req.setEmergencyFundMonths(12);

        req.setGoal("SAVE_INTEREST");
        req.setRiskProfile("MEDIUM");

        return req;
    }

    private LoanInput sampleLoanInput() {
        LoanInput loan = new LoanInput();
        loan.setLoanName("Home Loan");
        loan.setLoanAmount(BigDecimal.valueOf(500000));
        loan.setInterestRate(BigDecimal.valueOf(10));
        loan.setTenureMonths(60);
        return loan;
    }

    private LoanRequest sampleLoanRequest() {
        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(500000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(60);
        req.setExtraEmi(BigDecimal.valueOf(2000));
        return req;
    }

    private List<LoanResponse> sampleLoanResponses() {

        LoanResponse r1 = new LoanResponse();
        r1.setStrategy("Extra EMI");
        r1.setEmi(BigDecimal.valueOf(10000));
        r1.setInterestSaved(BigDecimal.valueOf(50000));
        r1.setTenureReducedMonths(10);

        LoanResponse r2 = new LoanResponse();
        r2.setStrategy("Part Payment");
        r2.setEmi(BigDecimal.valueOf(9000));
        r2.setInterestSaved(BigDecimal.valueOf(30000));
        r2.setTenureReducedMonths(5);

        return List.of(r1, r2);
    }
}
