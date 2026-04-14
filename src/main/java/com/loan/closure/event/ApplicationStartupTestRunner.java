package com.loan.closure.event;

import com.loan.closure.entity.*;
import com.loan.closure.service.StrategyFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * ApplicationStartupTestRunner
 *
 * Automatically runs integration tests when the application starts.
 * This verifies that all services are working correctly before accepting requests.
 *
 * Controlled by properties:
 *   app.startup-tests.enabled=true        - Enable/disable tests
 *   app.startup-tests.fail-on-error=true  - Fail app if tests fail
 */
@Component
public class ApplicationStartupTestRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupTestRunner.class);
    private static final String SEPARATOR = "═══════════════════════════════════════════════════════════════════════════";

    private final StrategyFacadeService strategyFacadeService;

    @Value("${app.startup-tests.enabled:true}")
    private boolean testsEnabled;

    @Value("${app.startup-tests.fail-on-error:true}")
    private boolean failOnError;

    public ApplicationStartupTestRunner(StrategyFacadeService strategyFacadeService) {
        this.strategyFacadeService = strategyFacadeService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runIntegrationTests() {
        // Check if tests are enabled
        if (!testsEnabled) {
            log.info("⏭️  Application startup integration tests are DISABLED (app.startup-tests.enabled=false)");
            return;
        }

        log.info(SEPARATOR);
        log.info("🧪 STARTING APPLICATION INTEGRATION TESTS");
        log.info(SEPARATOR + "\n");

        try {
            // Test 1: Income-based Strategy
            testIncomeBasedStrategy();

            // Test 2: Direct Loan Mode
            testDirectLoanMode();

            // Test 3: Error Handling
            testErrorHandling();

            // Test 4: Multiple Loans
            testMultipleLoans();

            log.info("\n" + SEPARATOR);
            log.info("✅ ALL INTEGRATION TESTS PASSED");
            log.info(SEPARATOR);
            log.info("Application is ready to accept requests\n");

        } catch (Exception e) {
            log.error("❌ INTEGRATION TEST FAILED");
            log.error(SEPARATOR);
            log.error("Error: ", e);
            log.error(SEPARATOR);

            // Check if we should fail on error
            if (failOnError) {
                log.error("Application startup failed (app.startup-tests.fail-on-error=true)");
                throw new RuntimeException("Integration tests failed during startup", e);
            } else {
                log.warn("⚠️  Integration tests failed but continuing (app.startup-tests.fail-on-error=false)");
            }
        }
    }

    /**
     * Test 1: Income-based Strategy Calculation
     */
    private void testIncomeBasedStrategy() {
        log.info("📋 Test 1: Income-Based Strategy Calculation...");

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);
        request.setExpenseRequest(buildExpenseRequest());

        StrategyResult result = strategyFacadeService.calculateStrategy(request);

        assert result != null : "Result should not be null";
        assert result.getRecommendedStrategy() != null : "Recommended strategy should not be null";
        assert result.getAllStrategies() != null : "All strategies should not be null";
        assert !result.getAllStrategies().isEmpty() : "Strategies list should not be empty";
        assert result.getAdvice() != null : "Advice should not be null";
        assert result.getReason() != null : "Reason should not be null";
        assert result.getLoanPriority() != null : "Loan priority should not be null";

        log.info("   ✅ Income-based strategy test PASSED");
        log.info("      - Recommended Strategy: {}", result.getRecommendedStrategy().getStrategy());
        log.info("      - Interest Saved: ₹{}", result.getRecommendedStrategy().getInterestSaved());
        log.info("      - Number of Strategies: {}\n", result.getAllStrategies().size());
    }

    /**
     * Test 2: Direct Loan Mode Calculation
     */
    private void testDirectLoanMode() {
        log.info("📋 Test 2: Direct Loan Mode Calculation...");

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(false);
        request.setLoanRequest(buildLoanRequest());

        StrategyResult result = strategyFacadeService.calculateStrategy(request);

        assert result != null : "Result should not be null";
        assert result.getAllStrategies() != null : "All strategies should not be null";
        assert result.getAllStrategies().size() >= 1 : "At least one strategy should be returned";

        log.info("   ✅ Direct loan mode test PASSED");
        log.info("      - Number of Strategies: {}\n", result.getAllStrategies().size());
    }

    /**
     * Test 3: Error Handling (Negative Disposable Income)
     */
    private void testErrorHandling() {
        log.info("📋 Test 3: Error Handling (Negative Disposable Income)...");

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);

        ExpenseRequest exp = buildExpenseRequest();
        exp.setMonthlyIncome(BigDecimal.valueOf(1000)); // Force negative disposable

        request.setExpenseRequest(exp);

        try {
            strategyFacadeService.calculateStrategy(request);
            assert false : "Should have thrown RuntimeException for negative disposable income";
        } catch (RuntimeException e) {
            assert e.getMessage().contains("Expenses") : "Error message should mention expenses";
            log.info("   ✅ Error handling test PASSED");
            log.info("      - Expected exception caught: {}\n", e.getMessage());
        }
    }

    /**
     * Test 4: Multiple Loans Handling
     */
    private void testMultipleLoans() {
        log.info("📋 Test 4: Multiple Loans Handling...");

        ExpenseRequest req = buildExpenseRequest();

        // Add second loan
        LoanInput loan2 = new LoanInput();
        loan2.setLoanName("Car Loan");
        loan2.setLoanAmount(BigDecimal.valueOf(200000));
        loan2.setInterestRate(BigDecimal.valueOf(12));
        loan2.setTenureMonths(36);

        req.setLoans(List.of(req.getLoans().get(0), loan2));

        StrategyRequest request = new StrategyRequest();
        request.setUseIncomeStrategy(true);
        request.setExpenseRequest(req);

        StrategyResult result = strategyFacadeService.calculateStrategy(request);

        assert result != null : "Result should not be null";
        assert result.getRecommendedStrategy() != null : "Recommended strategy should not be null";
        assert result.getLoanPriority() != null : "Loan priority should not be null";
        assert result.getLoanPriority().size() >= 1 : "Loan priority should have at least one entry";

        log.info("   ✅ Multiple loans test PASSED");
        log.info("      - Number of Loans: 2");
        log.info("      - Loan Priority Count: {}\n", result.getLoanPriority().size());
    }

    /**
     * Helper: Build Expense Request for Testing
     */
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

    /**
     * Helper: Build Loan Request for Testing
     */
    private LoanRequest buildLoanRequest() {
        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(500000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(60);
        req.setExtraEmi(BigDecimal.valueOf(2000));
        return req;
    }
}

