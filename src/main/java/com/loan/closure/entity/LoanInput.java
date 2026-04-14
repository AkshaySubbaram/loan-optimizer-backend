package com.loan.closure.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

public class LoanInput {

    @NotBlank
    private String loanName;

    @NotNull
    @DecimalMin(value = "1.0", message = "Loan amount must be greater than 0")
    private BigDecimal loanAmount;

    @NotNull
    @DecimalMin(value = "0.1", message = "Interest rate must be greater than 0")
    private BigDecimal interestRate;

    @NotNull
    @Min(value = 1, message = "Tenure must be at least 1 month")
    private int tenureMonths;

    private Integer priority;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate sanctionDate;

    public String getLoanName() {
        return loanName;
    }

    public void setLoanName(String loanName) {
        this.loanName = loanName;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public int getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(int tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDate getSanctionDate() {
        return sanctionDate;
    }

    public void setSanctionDate(LocalDate sanctionDate) {
        this.sanctionDate = sanctionDate;
    }

}
