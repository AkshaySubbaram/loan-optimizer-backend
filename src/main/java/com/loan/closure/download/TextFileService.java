package com.loan.closure.download;

import com.loan.closure.entity.AmortizationEntry;
import com.loan.closure.entity.LoanRequest;
import com.loan.closure.entity.LoanResponse;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TextFileService {

    public byte[] generateLoanTextFile(List<LoanResponse> strategies, LoanRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            writer.write("=== Loan Report ===\n");
            writer.write("Loan Amount: " + request.getLoanAmount() + "\n");
            writer.write("Interest Rate: " + request.getInterestRate() + "\n");
            writer.write("Tenure (Months): " + request.getTenureMonths() + "\n\n");

            for (LoanResponse strategy : strategies) {
                writer.write("Strategy: " + strategy.getStrategy() + "\n");
                writer.write("EMI: " + strategy.getEmi()
                        + ", Interest Saved: " + strategy.getInterestSaved()
                        + ", Tenure Reduced: " + strategy.getTenureReducedMonths()
                        + "\n");

                if (strategy.getAmortization() != null && !strategy.getAmortization().isEmpty()) {
                    writer.write("Month | Principal Paid | Interest Paid | Balance\n");
                    for (AmortizationEntry entry : strategy.getAmortization()) {
                        writer.write(entry.getMonth() + " | " +
                                entry.getPrincipalPaid() + " | " +
                                entry.getInterestPaid() + " | " +
                                entry.getBalance() + "\n");
                    }
                }
                writer.write("\n");
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating text file: " + e.getMessage(), e);
        }
    }
}