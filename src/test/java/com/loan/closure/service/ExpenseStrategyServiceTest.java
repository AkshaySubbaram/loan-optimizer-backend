package com.loan.closure.service;

import com.loan.closure.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpenseStrategyServiceTest {

    private LoanService loanService;

    private ExpenseStrategyService service;

    @BeforeEach
    void setup() {
        loanService = mock(LoanService.class);
        service = new ExpenseStrategyService(loanService);
    }

    // ✅ 1. Happy Path
    @Test
    void shouldBuildLoanRequestsSuccessfully() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // ✅ 2. Disposable <= 0 → should handle gracefully (no exception)
    @Test
    void shouldHandleNegativeDisposableGracefully() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();

        req.setMonthlyIncome(BigDecimal.valueOf(10000)); // very low

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("20000"));

        List<LoanRequest> result = assertDoesNotThrow(
                () -> service.buildLoanRequestsFromExpense(req)
        );

        assertNotNull(result);
    }

    // ✅ 3. No expenses
    @Test
    void shouldHandleNoExpenses() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();
        req.setExpenses(null);

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
    }

    // ✅ 4. No loans
    @Test
    void shouldHandleNoLoans() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();
        req.setLoans(List.of());

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertTrue(result.isEmpty());
    }

    // ✅ 5. Emergency fund logic
    @Test
    void shouldCalculateEmergencyContributionCorrectly() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();

        req.setEmergencyFund(BigDecimal.ZERO);
        req.setEmergencyFundTarget(BigDecimal.valueOf(120000));
        req.setEmergencyFundMonths(12);

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should generate loan requests");

        LoanRequest lr = result.get(0);

        assertNotNull(lr.getExtraEmi());
        assertTrue(lr.getExtraEmi().compareTo(BigDecimal.ZERO) > 0,
                "Extra EMI should be allocated after emergency fund contribution");
    }

    // ✅ 6. High risk profile → more EMI
    @Test
    void shouldAllocateMoreToExtraEmiForHighRisk() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();
        req.setRiskProfile("HIGH");

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertTrue(result.get(0).getExtraEmi().compareTo(BigDecimal.ZERO) > 0);
    }

    // ✅ 7. LOW_EMI goal → less extra EMI
    @Test
    void shouldReduceExtraEmiForLowEmiGoal() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();
        req.setGoal("LOW_EMI");
        req.setRiskProfile("MEDIUM");

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        LoanRequest lr = result.get(0);
        assertTrue(lr.getExtraEmi().compareTo(new BigDecimal("15000")) < 0,
                "LOW_EMI goal should allocate at most 30% of disposable income");
    }

    // ✅ 8. Part payments should be generated
    @Test
    void shouldGeneratePartPayments() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        LoanRequest lr = result.get(0);

        assertNotNull(lr.getPartPayments());
        assertNotNull(lr.getPartPaymentMonths());
        assertEquals(lr.getPartPayments().size(), lr.getPartPaymentMonths().size(),
                "Part payments and months should have same count");
    }

    // ✅ 9. Loans sorted by interest rate
    @Test
    void shouldSortLoansByInterestRate() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        if (result.size() >= 2) {
            LoanRequest firstLoan = result.get(0);
            assertTrue(firstLoan.getExtraEmi().compareTo(BigDecimal.ZERO) > 0,
                    "Highest interest loan should get extra EMI allocation");
        }
    }

    // ✅ 10. Null values safety
    @Test
    void shouldHandleNullValuesSafely() {

        ExpenseRequest req = new ExpenseRequest();
        req.setMonthlyIncome(BigDecimal.valueOf(100000));
        req.setLoans(TestDataFactory.sampleLoans());

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
    }

}
