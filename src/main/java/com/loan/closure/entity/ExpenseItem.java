package com.loan.closure.entity;

import java.math.BigDecimal;

public class ExpenseItem {

    private String name;

    private BigDecimal amount;

    public ExpenseItem() {
    }

    public ExpenseItem(String name, BigDecimal amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
