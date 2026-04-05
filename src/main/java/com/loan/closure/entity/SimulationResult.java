package com.loan.closure.entity;

import java.math.BigDecimal;

public class SimulationResult {

    private int months;

    private BigDecimal totalPaid;

    public SimulationResult(int months, BigDecimal totalPaid) {
        this.months = months;
        this.totalPaid = totalPaid;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }
}
