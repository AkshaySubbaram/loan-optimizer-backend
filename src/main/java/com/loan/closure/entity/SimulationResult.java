package com.loan.closure.entity;

public class SimulationResult {

    private int months;

    private double totalPaid;

    public SimulationResult(int months, double totalPaid) {
        this.months = months;
        this.totalPaid = totalPaid;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(double totalPaid) {
        this.totalPaid = totalPaid;
    }

}
