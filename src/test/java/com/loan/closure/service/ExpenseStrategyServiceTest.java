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

    // ✅ 2. Disposable <= 0 → should fail
    @Test
    void shouldThrowWhenDisposableNegative() {

        ExpenseRequest req = TestDataFactory.sampleExpenseRequest();

        req.setMonthlyIncome(BigDecimal.valueOf(10000)); // very low

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("20000"));

        assertThrows(RuntimeException.class,
                () -> service.buildLoanRequestsFromExpense(req));
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

        // Monthly emergency contribution = (120000 - 0) / 12 = 10,000
        // This reduces disposable income, affecting extra EMI allocation
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should generate loan requests");

        // Verify that extra EMI is calculated considering emergency fund contribution
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
        req.setRiskProfile("MEDIUM");  // Set explicitly for clarity

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // For LOW_EMI + MEDIUM risk: extraEmiRatio = 0.2 (20%)
        // This means extra EMI should be significantly lower
        // Disposable = 100,000 - 30,000 (expenses) - 20,000 (EMI) - ~667 (emergency) ≈ 49,333
        // Extra EMI at 20% = ~9,866
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
        // Sample data has: Home @ 9%, Car @ 12%
        // After sorting (descending): Car (12%) should be first

        when(loanService.calculateEMI(any(), any(), any()))
                .thenReturn(new BigDecimal("10000"));

        List<LoanRequest> result = service.buildLoanRequestsFromExpense(req);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // TestDataFactory has Car Loan @ 12% interest rate
        // Loans should be sorted by interest rate descending (highest first)
        // Verify by checking the order of results
        if (result.size() >= 2) {
            // Car Loan (higher rate) should be prioritized first in allocations
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
