package com.loan.closure.service;

import com.loan.closure.entity.*;

import java.math.BigDecimal;
import java.util.List;

public class TestDataFactory {

    // 🔹 Sample LoanRequest
    public static LoanRequest sampleLoanRequest() {
        LoanRequest req = new LoanRequest();
        req.setLoanAmount(BigDecimal.valueOf(500000));
        req.setInterestRate(BigDecimal.valueOf(10));
        req.setTenureMonths(60);
        req.setExtraEmi(BigDecimal.valueOf(2000));
        req.setPartPayments(List.of(BigDecimal.valueOf(50000)));
        req.setPartPaymentMonths(List.of(6));
        return req;
    }

    // 🔹 Sample ExpenseRequest
    public static ExpenseRequest sampleExpenseRequest() {

        ExpenseRequest req = new ExpenseRequest();

        req.setMonthlyIncome(BigDecimal.valueOf(100000));

        req.setExpenses(List.of(
                new ExpenseItem("Rent", BigDecimal.valueOf(20000)),
                new ExpenseItem("Food", BigDecimal.valueOf(10000))
        ));

        req.setLoans(sampleLoans());

        req.setEmergencyFund(BigDecimal.valueOf(20000));
        req.setEmergencyFundTarget(BigDecimal.valueOf(100000));
        req.setEmergencyFundMonths(12);

        req.setRiskProfile("MEDIUM");
        req.setGoal("BALANCE");

        return req;
    }

    // 🔹 Sample Loans List
    public static List<LoanInput> sampleLoans() {

        LoanInput l1 = new LoanInput();
        l1.setLoanName("Home Loan");
        l1.setLoanAmount(BigDecimal.valueOf(500000));
        l1.setInterestRate(BigDecimal.valueOf(9));
        l1.setTenureMonths(120);

        LoanInput l2 = new LoanInput();
        l2.setLoanName("Car Loan");
        l2.setLoanAmount(BigDecimal.valueOf(200000));
        l2.setInterestRate(BigDecimal.valueOf(12));
        l2.setTenureMonths(60);

        return List.of(l1, l2);
    }
}
