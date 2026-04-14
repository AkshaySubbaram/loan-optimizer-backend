package com.loan.closure.download;

import com.loan.closure.entity.*;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
public class TextFileService {

    private static final String SEPARATOR = "═══════════════════════════════════════════════════════════════════════════";
    private static final String SUB_SEPARATOR = "───────────────────────────────────────────────────────────────────────────";
    private static final String TABLE_SEPARATOR = "─────────┼─────────────────┼─────────────────┼─────────────────┼─────────────────";

    public byte[] generateLoanTextFile(List<LoanResponse> strategies, LoanRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            writeHeader(writer);
            writeLoanSummary(writer, request);
            writeStrategyComparison(writer, strategies);
            writeDetailedStrategyAnalysis(writer, strategies);
            writeAmortizationSchedules(writer, strategies);
            writeFooter(writer);

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating text file: " + e.getMessage(), e);
        }
    }

    private void writeHeader(OutputStreamWriter writer) throws Exception {
        writer.write("\n");
        writer.write(SEPARATOR + "\n");
        writer.write("                          DETAILED LOAN CLOSURE ANALYSIS REPORT\n");
        writer.write(SEPARATOR + "\n");
        writer.write("Generated Date: " + LocalDate.now() + "\n\n");
    }

    private void writeLoanSummary(OutputStreamWriter writer, LoanRequest request) throws Exception {
        writer.write("📋 LOAN INPUT SUMMARY\n");
        writer.write(SUB_SEPARATOR + "\n");
        writer.write(String.format("  Loan Amount:                    ₹ %,d%n",
                request.getLoanAmount().setScale(0, RoundingMode.HALF_UP).longValue()));
        writer.write(String.format("  Annual Interest Rate:           %.2f%% p.a.%n", request.getInterestRate()));
        writer.write(String.format("  Loan Tenure:                    %d months (%d years, %d months)%n",
                request.getTenureMonths(),
                request.getTenureMonths() / 12,
                request.getTenureMonths() % 12));
        writer.write(String.format("  Extra EMI (Monthly):            ₹ %,d%n",
                request.getExtraEmi().setScale(0, RoundingMode.HALF_UP).longValue()));

        if (request.getPartPayments() != null && !request.getPartPayments().isEmpty()) {
            writer.write(String.format("  Planned Part Payments:          %d payments planned%n",
                    request.getPartPayments().size()));
        }
        writer.write("\n");
    }

    private void writeStrategyComparison(OutputStreamWriter writer, List<LoanResponse> strategies) throws Exception {
        writer.write("📊 STRATEGY COMPARISON\n");
        writer.write(SUB_SEPARATOR + "\n");

        if (strategies == null || strategies.isEmpty()) {
            writer.write("  No strategies available.\n\n");
            return;
        }

        writer.write(String.format("%-30s | %15s | %18s | %18s | %15s%n",
                "Strategy", "EMI (₹)", "Interest Saved (₹)", "Total Interest (₹)", "Tenure Reduction"));
        writer.write(SUB_SEPARATOR + "\n");

        for (LoanResponse strategy : strategies) {
            String strategyName = truncate(strategy.getStrategy(), 28);
            long emi = strategy.getEmi().setScale(0, RoundingMode.HALF_UP).longValue();
            long interestSaved = strategy.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue();
            long totalInterest = strategy.getTotalInterestWithExtra().setScale(0, RoundingMode.HALF_UP).longValue();
            int tenureReduced = strategy.getTenureReducedMonths();

            writer.write(String.format("%-30s | %,15d | %,18d | %,18d | %3d months%n",
                    strategyName, emi, interestSaved, totalInterest, tenureReduced));
        }
        writer.write("\n");
    }

    private void writeDetailedStrategyAnalysis(OutputStreamWriter writer, List<LoanResponse> strategies) throws Exception {
        writer.write("💡 DETAILED STRATEGY ANALYSIS\n");
        writer.write(SUB_SEPARATOR + "\n\n");

        for (int i = 0; i < strategies.size(); i++) {
            LoanResponse strategy = strategies.get(i);

            writer.write(String.format("Strategy %d: %s%n", i + 1, strategy.getStrategy()));
            writer.write(SUB_SEPARATOR + "\n");

            writer.write("Financial Impact:\n");
            writer.write(String.format("  ✓ Monthly EMI:                  ₹ %,d%n",
                    strategy.getEmi().setScale(0, RoundingMode.HALF_UP).longValue()));
            writer.write(String.format("  ✓ Total Interest (Normal):      ₹ %,d%n",
                    strategy.getTotalInterestNormal().setScale(0, RoundingMode.HALF_UP).longValue()));
            writer.write(String.format("  ✓ Total Interest (With Extra):  ₹ %,d%n",
                    strategy.getTotalInterestWithExtra().setScale(0, RoundingMode.HALF_UP).longValue()));
            writer.write(String.format("  ✓ Interest Saved:               ₹ %,d%n",
                    strategy.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue()));

            writer.write("\nTenure Impact:\n");
            writer.write(String.format("  ✓ Original Tenure:              %d months%n",
                    strategy.getTenureReducedMonths() > 0 ?
                    calculateOriginalTenure(strategy) : 0));
            writer.write(String.format("  ✓ Tenure Reduction:             %d months (%d years)%n",
                    strategy.getTenureReducedMonths(),
                    strategy.getTenureReducedMonths() / 12));

            writer.write("\n");
        }
    }

    private void writeAmortizationSchedules(OutputStreamWriter writer, List<LoanResponse> strategies) throws Exception {
        writer.write("📅 AMORTIZATION SCHEDULES\n");
        writer.write(SUB_SEPARATOR + "\n\n");

        for (int i = 0; i < strategies.size(); i++) {
            LoanResponse strategy = strategies.get(i);

            if (strategy.getAmortization() == null || strategy.getAmortization().isEmpty()) {
                continue;
            }

            writer.write(String.format("Strategy %d: %s - Monthly Breakdown%n", i + 1, strategy.getStrategy()));
            writer.write(SUB_SEPARATOR + "\n");
            writer.write(String.format("%-7s | %15s | %15s | %15s | %15s%n",
                    "Month", "Principal (₹)", "Interest (₹)", "Payment (₹)", "Balance (₹)"));
            writer.write(TABLE_SEPARATOR + "\n");

            List<AmortizationEntry> amortization = strategy.getAmortization();
            int totalMonths = amortization.size();

            for (int j = 0; j < Math.min(12, totalMonths); j++) {
                AmortizationEntry entry = amortization.get(j);
                long principal = entry.getPrincipalPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long interest = entry.getInterestPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long payment = principal + interest;
                long balance = entry.getBalance().setScale(0, RoundingMode.HALF_UP).longValue();

                writer.write(String.format("%7d | %,15d | %,15d | %,15d | %,15d%n",
                        entry.getMonth(), principal, interest, payment, balance));
            }

            if (totalMonths > 12) {
                writer.write(TABLE_SEPARATOR + "\n");
                writer.write(String.format("... [%d months omitted for brevity] ...%n", totalMonths - 12));
                writer.write(TABLE_SEPARATOR + "\n");

                AmortizationEntry lastEntry = amortization.get(totalMonths - 1);
                long principal = lastEntry.getPrincipalPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long interest = lastEntry.getInterestPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long payment = principal + interest;
                long balance = lastEntry.getBalance().setScale(0, RoundingMode.HALF_UP).longValue();

                writer.write(String.format("%7d | %,15d | %,15d | %,15d | %,15d%n",
                        lastEntry.getMonth(), principal, interest, payment, balance));
            }

            writer.write("\n\n");
        }
    }

    private void writeFooter(OutputStreamWriter writer) throws Exception {
        writer.write(SEPARATOR + "\n");
        writer.write("⚠️  IMPORTANT DISCLAIMERS\n");
        writer.write(SUB_SEPARATOR + "\n");
        writer.write("• This report is generated based on the input parameters provided and assumes\n");
        writer.write("  consistent interest rates and payment schedules throughout the loan tenure.\n");
        writer.write("• Actual loan repayment may vary due to changes in interest rates, bank policies,\n");
        writer.write("  additional fees, or prepayment charges.\n");
        writer.write("• The figures are approximations and should be verified with your lender before\n");
        writer.write("  making financial decisions.\n");
        writer.write("• Interest calculations assume simple compounding and monthly payments as specified.\n");
        writer.write("\n");
        writer.write(SEPARATOR + "\n");
        writer.write("For more information or updates, please contact your financial advisor.\n");
        writer.write("Report Generated: " + java.time.LocalDateTime.now() + "\n");
    }

    private int calculateOriginalTenure(LoanResponse strategy) {
        return strategy.getTenureReducedMonths() > 0 ?
               strategy.getTenureReducedMonths() + (strategy.getTenureReducedMonths() / 5) : 0;
    }

    private String truncate(String str, int length) {
        if (str == null) {
            return "N/A";
        }
        return str.length() > length ? str.substring(0, length - 3) + "..." : str;
    }

    public byte[] generateStrategyReport(StrategyResult strategyResult) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            writer.write("\n");
            writer.write(SEPARATOR + "\n");
            writer.write("                     PERSONALIZED LOAN CLOSURE STRATEGY REPORT\n");
            writer.write(SEPARATOR + "\n");
            writer.write("Generated Date: " + LocalDate.now() + "\n");
            writer.write("Report Type: Income-Based Strategy Analysis\n\n");

            writeRecommendationSection(writer, strategyResult);
            writeReasoningSection(writer, strategyResult);
            writeAdviceSection(writer, strategyResult);
            writeLoanPrioritySection(writer, strategyResult);
            writeAllStrategiesComparison(writer, strategyResult);
            writeFooter(writer);

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating strategy report: " + e.getMessage(), e);
        }
    }

    private void writeRecommendationSection(OutputStreamWriter writer, StrategyResult result) throws Exception {
        writer.write("🎯 RECOMMENDED STRATEGY\n");
        writer.write(SUB_SEPARATOR + "\n\n");

        if (result.getRecommendedStrategy() != null) {
            LoanResponse best = result.getRecommendedStrategy();

            writer.write(String.format("Strategy: %s%n%n", best.getStrategy()));

            writer.write("Key Metrics:\n");
            writer.write(String.format("  ✓ Monthly EMI:                  ₹ %,d%n",
                    best.getEmi().setScale(0, RoundingMode.HALF_UP).longValue()));
            writer.write(String.format("  ✓ Total Interest (Normal):      ₹ %,d%n",
                    best.getTotalInterestNormal().setScale(0, RoundingMode.HALF_UP).longValue()));
            writer.write(String.format("  ✓ Total Interest (With Extra):  ₹ %,d%n",
                    best.getTotalInterestWithExtra().setScale(0, RoundingMode.HALF_UP).longValue()));
            writer.write(String.format("  ✓ Interest Saved:               ₹ %,d (Significant Savings!)%n",
                    best.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue()));
            writer.write(String.format("  ✓ Tenure Reduction:             %d months (%d years, %d months)%n",
                    best.getTenureReducedMonths(),
                    best.getTenureReducedMonths() / 12,
                    best.getTenureReducedMonths() % 12));

            writer.write("\n");
        } else {
            writer.write("No strategy recommendation available at this time.\n\n");
        }
    }

    private void writeReasoningSection(OutputStreamWriter writer, StrategyResult result) throws Exception {
        writer.write("📊 ANALYSIS & REASONING\n");
        writer.write(SUB_SEPARATOR + "\n");

        if (result.getReason() != null && !result.getReason().isEmpty()) {
            writer.write(result.getReason() + "\n");
        } else {
            writer.write("This recommendation is based on your financial profile and goals.\n");
        }

        writer.write("\n");
    }

    private void writeAdviceSection(OutputStreamWriter writer, StrategyResult result) throws Exception {
        writer.write("💡 EXPERT ADVICE\n");
        writer.write(SUB_SEPARATOR + "\n");

        if (result.getAdvice() != null) {
            StrategyAdvice advice = result.getAdvice();

            if (advice.getExtraEmiRecommended() != null) {
                writer.write(String.format("Recommended Extra EMI: ₹ %,d per month%n",
                        advice.getExtraEmiRecommended().setScale(0, RoundingMode.HALF_UP).longValue()));
                writer.write("  This additional amount will accelerate your loan closure and reduce interest significantly.\n\n");
            }

            if (advice.getPartPaymentPlan() != null && !advice.getPartPaymentPlan().isEmpty()) {
                writer.write("Part Payment Plan:\n");
                writer.write("  " + advice.getPartPaymentPlan() + "\n\n");
            }

            if (advice.getSummary() != null && !advice.getSummary().isEmpty()) {
                writer.write("Summary:\n");
                writer.write("  " + advice.getSummary() + "\n\n");
            }
        }

        writer.write("General Tips for Loan Closure:\n");
        writer.write("  • Try to maintain the recommended extra EMI consistently\n");
        writer.write("  • Make part payments whenever you have surplus funds\n");
        writer.write("  • Avoid taking additional debt during this period\n");
        writer.write("  • Monitor your loan account regularly for prepayment charges\n");
        writer.write("\n");
    }

    private void writeLoanPrioritySection(OutputStreamWriter writer, StrategyResult result) throws Exception {
        if (result.getLoanPriority() != null && !result.getLoanPriority().isEmpty()) {
            writer.write("📌 LOAN PRIORITY ORDER\n");
            writer.write(SUB_SEPARATOR + "\n");
            writer.write("Focus on closing loans in this order to maximize interest savings:\n\n");

            for (int i = 0; i < result.getLoanPriority().size(); i++) {
                writer.write(String.format("  %d. %s%n", i + 1, result.getLoanPriority().get(i)));
            }
            writer.write("\n");
        }
    }

    private void writeAllStrategiesComparison(OutputStreamWriter writer, StrategyResult result) throws Exception {
        if (result.getAllStrategies() != null && !result.getAllStrategies().isEmpty()) {
            writer.write("📈 ALL AVAILABLE STRATEGIES\n");
            writer.write(SUB_SEPARATOR + "\n");

            writer.write(String.format("%-35s | %15s | %15s | %15s%n",
                    "Strategy", "EMI (₹)", "Interest Saved (₹)", "Tenure Reduced"));
            writer.write(SUB_SEPARATOR + "\n");

            for (LoanResponse strategy : result.getAllStrategies()) {
                String strategyName = truncate(strategy.getStrategy(), 33);
                long emi = strategy.getEmi().setScale(0, RoundingMode.HALF_UP).longValue();
                long interestSaved = strategy.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue();
                int tenureReduced = strategy.getTenureReducedMonths();

                writer.write(String.format("%-35s | %,15d | %,15d | %d months%n",
                        strategyName, emi, interestSaved, tenureReduced));
            }
            writer.write("\n");
        }
    }

}