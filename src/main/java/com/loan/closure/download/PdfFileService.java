package com.loan.closure.download;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.loan.closure.entity.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PdfFileService {

    private static final float FONT_SIZE_HEADER = 20f;
    private static final float FONT_SIZE_SUBTITLE = 12f;
    private static final float FONT_SIZE_NORMAL = 10f;
    private static final float FONT_SIZE_SMALL = 9f;

    public byte[] generateLoanPdfReport(List<LoanResponse> strategies, LoanRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Header
            addHeader(document);

            // Loan Summary
            addLoanSummary(document, request);

            // Strategy Comparison Table
            addStrategyComparison(document, strategies);

            // Detailed Strategy Analysis
            addDetailedStrategyAnalysis(document, strategies);

            // Amortization Schedules
            addAmortizationSchedules(document, strategies);

            // Footer with Disclaimer
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF file: " + e.getMessage(), e);
        }
    }

    public byte[] generateStrategyPdfReport(StrategyResult strategyResult) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Header
            addPdfHeader(document, "PERSONALIZED LOAN CLOSURE STRATEGY REPORT");

            // Recommendation Section
            addRecommendationSection(document, strategyResult);

            // Reasoning Section
            addReasoningSection(document, strategyResult);

            // Advice Section
            addAdviceSection(document, strategyResult);

            // Loan Priority Section
            addLoanPrioritySection(document, strategyResult);

            // All Strategies Comparison
            addAllStrategiesComparison(document, strategyResult);

            // Footer
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating strategy PDF report: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document) throws IOException {
        addPdfHeader(document, "DETAILED LOAN CLOSURE ANALYSIS REPORT");
        addParagraph(document, "Generated Date: " + LocalDate.now(), FONT_SIZE_SMALL, TextAlignment.CENTER);
        document.add(new Paragraph("\n"));
    }

    private void addPdfHeader(Document document, String title) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        Paragraph heading = new Paragraph(title)
                .setFont(font)
                .setFontSize(FONT_SIZE_HEADER)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(heading);

        document.add(new Paragraph("\n"));
    }

    private void addLoanSummary(Document document, LoanRequest request) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addSectionTitle(document, "📋 LOAN INPUT SUMMARY", font);

        Table table = new Table(2);
        table.setWidth(UnitValue.createPercentValue(100)).setHorizontalAlignment(HorizontalAlignment.LEFT);

        addTableRow(table, "Loan Amount:", "₹ " + String.format("%,d",
                request.getLoanAmount().setScale(0, RoundingMode.HALF_UP).longValue()), font);

        addTableRow(table, "Annual Interest Rate:", String.format("%.2f%% p.a.", request.getInterestRate()), font);

        addTableRow(table, "Loan Tenure:", String.format("%d months (%d years, %d months)",
                request.getTenureMonths(),
                request.getTenureMonths() / 12,
                request.getTenureMonths() % 12), font);

        addTableRow(table, "Extra EMI (Monthly):", "₹ " + String.format("%,d",
                request.getExtraEmi().setScale(0, RoundingMode.HALF_UP).longValue()), font);

        if (request.getPartPayments() != null && !request.getPartPayments().isEmpty()) {
            addTableRow(table, "Planned Part Payments:", String.format("%d payments planned",
                    request.getPartPayments().size()), font);
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addStrategyComparison(Document document, List<LoanResponse> strategies) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addSectionTitle(document, "📊 STRATEGY COMPARISON", font);

        if (strategies == null || strategies.isEmpty()) {
            addParagraph(document, "No strategies available.", FONT_SIZE_NORMAL, TextAlignment.LEFT);
            document.add(new Paragraph("\n"));
            return;
        }

        Table table = new Table(5);
        table.setWidth(UnitValue.createPercentValue(100));

        // Header row
        addHeaderCell(table, "Strategy", font);
        addHeaderCell(table, "EMI (₹)", font);
        addHeaderCell(table, "Interest Saved (₹)", font);
        addHeaderCell(table, "Total Interest (₹)", font);
        addHeaderCell(table, "Tenure Reduction", font);

        // Data rows
        for (LoanResponse strategy : strategies) {
            String strategyName = truncate(strategy.getStrategy(), 20);
            long emi = strategy.getEmi().setScale(0, RoundingMode.HALF_UP).longValue();
            long interestSaved = strategy.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue();
            long totalInterest = strategy.getTotalInterestWithExtra().setScale(0, RoundingMode.HALF_UP).longValue();
            int tenureReduced = strategy.getTenureReducedMonths();

            addTableCell(table, strategyName, font);
            addTableCell(table, String.format("₹ %,d", emi), font);
            addTableCell(table, String.format("₹ %,d", interestSaved), font);
            addTableCell(table, String.format("₹ %,d", totalInterest), font);
            addTableCell(table, tenureReduced + " months", font);
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addDetailedStrategyAnalysis(Document document, List<LoanResponse> strategies) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addSectionTitle(document, "💡 DETAILED STRATEGY ANALYSIS", font);

        for (int i = 0; i < strategies.size(); i++) {
            LoanResponse strategy = strategies.get(i);

            addSubsectionTitle(document, "Strategy " + (i + 1) + ": " + strategy.getStrategy(), font);

            Table table = new Table(2);
            table.setWidth(UnitValue.createPercentValue(100));

            // Financial Impact
            addSectionHeader(table, "Financial Impact", font);
            addTableRow(table, "Monthly EMI:", "₹ " + String.format("%,d",
                    strategy.getEmi().setScale(0, RoundingMode.HALF_UP).longValue()), font);
            addTableRow(table, "Total Interest (Normal):", "₹ " + String.format("%,d",
                    strategy.getTotalInterestNormal().setScale(0, RoundingMode.HALF_UP).longValue()), font);
            addTableRow(table, "Total Interest (With Extra):", "₹ " + String.format("%,d",
                    strategy.getTotalInterestWithExtra().setScale(0, RoundingMode.HALF_UP).longValue()), font);
            addTableRow(table, "Interest Saved:", "₹ " + String.format("%,d",
                    strategy.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue()), font);

            // Tenure Impact
            addSectionHeader(table, "Tenure Impact", font);
            addTableRow(table, "Tenure Reduction:", String.format("%d months (%d years)",
                    strategy.getTenureReducedMonths(),
                    strategy.getTenureReducedMonths() / 12), font);

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }

    private void addAmortizationSchedules(Document document, List<LoanResponse> strategies) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addSectionTitle(document, "📅 AMORTIZATION SCHEDULES", font);

        for (int i = 0; i < strategies.size(); i++) {
            LoanResponse strategy = strategies.get(i);

            if (strategy.getAmortization() == null || strategy.getAmortization().isEmpty()) {
                continue;
            }

            addSubsectionTitle(document, "Strategy " + (i + 1) + ": " + strategy.getStrategy() + " - Monthly Breakdown", font);

            Table table = new Table(5);
            table.setWidth(UnitValue.createPercentValue(100));

            // Header
            addHeaderCell(table, "Month", font);
            addHeaderCell(table, "Principal (₹)", font);
            addHeaderCell(table, "Interest (₹)", font);
            addHeaderCell(table, "Payment (₹)", font);
            addHeaderCell(table, "Balance (₹)", font);

            List<AmortizationEntry> amortization = strategy.getAmortization();
            int totalMonths = amortization.size();

            // Show first 12 months
            for (int j = 0; j < Math.min(12, totalMonths); j++) {
                AmortizationEntry entry = amortization.get(j);
                long principal = entry.getPrincipalPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long interest = entry.getInterestPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long payment = principal + interest;
                long balance = entry.getBalance().setScale(0, RoundingMode.HALF_UP).longValue();

                addTableCell(table, String.valueOf(entry.getMonth()), font);
                addTableCell(table, String.format("₹ %,d", principal), font);
                addTableCell(table, String.format("₹ %,d", interest), font);
                addTableCell(table, String.format("₹ %,d", payment), font);
                addTableCell(table, String.format("₹ %,d", balance), font);
            }

            // Show summary for remaining months if more than 12
            if (totalMonths > 12) {
                Cell cell = new Cell(1, 5)
                        .add(new Paragraph(String.format("... [%d months omitted for brevity] ...", totalMonths - 12))
                                .setFont(font)
                                .setFontSize(FONT_SIZE_SMALL)
                                .setTextAlignment(TextAlignment.CENTER));
                table.addCell(cell);

                // Show last month
                AmortizationEntry lastEntry = amortization.get(totalMonths - 1);
                long principal = lastEntry.getPrincipalPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long interest = lastEntry.getInterestPaid().setScale(0, RoundingMode.HALF_UP).longValue();
                long payment = principal + interest;
                long balance = lastEntry.getBalance().setScale(0, RoundingMode.HALF_UP).longValue();

                addTableCell(table, String.valueOf(lastEntry.getMonth()), font);
                addTableCell(table, String.format("₹ %,d", principal), font);
                addTableCell(table, String.format("₹ %,d", interest), font);
                addTableCell(table, String.format("₹ %,d", payment), font);
                addTableCell(table, String.format("₹ %,d", balance), font);
            }

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }

    private void addRecommendationSection(Document document, StrategyResult result) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addSectionTitle(document, "🎯 RECOMMENDED STRATEGY", font);

        if (result.getRecommendedStrategy() != null) {
            LoanResponse best = result.getRecommendedStrategy();

            addSubsectionTitle(document, best.getStrategy(), font);

            Table table = new Table(2);
            table.setWidth(UnitValue.createPercentValue(100));

            addTableRow(table, "Monthly EMI:", "₹ " + String.format("%,d",
                    best.getEmi().setScale(0, RoundingMode.HALF_UP).longValue()), font);
            addTableRow(table, "Total Interest (Normal):", "₹ " + String.format("%,d",
                    best.getTotalInterestNormal().setScale(0, RoundingMode.HALF_UP).longValue()), font);
            addTableRow(table, "Total Interest (With Extra):", "₹ " + String.format("%,d",
                    best.getTotalInterestWithExtra().setScale(0, RoundingMode.HALF_UP).longValue()), font);
            addTableRow(table, "Interest Saved:", "₹ " + String.format("%,d",
                    best.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue()), font);
            addTableRow(table, "Tenure Reduction:", String.format("%d months (%d years, %d months)",
                    best.getTenureReducedMonths(),
                    best.getTenureReducedMonths() / 12,
                    best.getTenureReducedMonths() % 12), font);

            document.add(table);
        } else {
            addParagraph(document, "No strategy recommendation available at this time.", FONT_SIZE_NORMAL, TextAlignment.LEFT);
        }

        document.add(new Paragraph("\n"));
    }

    private void addReasoningSection(Document document, StrategyResult result) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addSectionTitle(document, "📊 ANALYSIS & REASONING", font);

        String reason = (result.getReason() != null && !result.getReason().isEmpty())
                ? result.getReason()
                : "This recommendation is based on your financial profile and goals.";

        addParagraph(document, reason, FONT_SIZE_NORMAL, TextAlignment.LEFT);
        document.add(new Paragraph("\n"));
    }

    private void addAdviceSection(Document document, StrategyResult result) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addSectionTitle(document, "💡 EXPERT ADVICE", font);

        if (result.getAdvice() != null) {
            StrategyAdvice advice = result.getAdvice();

            if (advice.getExtraEmiRecommended() != null) {
                addParagraph(document, "Recommended Extra EMI: ₹ " + String.format("%,d",
                        advice.getExtraEmiRecommended().setScale(0, RoundingMode.HALF_UP).longValue()),
                        FONT_SIZE_NORMAL, TextAlignment.LEFT);
                addParagraph(document,
                        "This additional amount will accelerate your loan closure and reduce interest significantly.",
                        FONT_SIZE_SMALL, TextAlignment.LEFT);
                document.add(new Paragraph("\n"));
            }

            if (advice.getPartPaymentPlan() != null && !advice.getPartPaymentPlan().isEmpty()) {
                addParagraph(document, "Part Payment Plan:", FONT_SIZE_NORMAL, TextAlignment.LEFT);
                addParagraph(document, advice.getPartPaymentPlan(), FONT_SIZE_SMALL, TextAlignment.LEFT);
                document.add(new Paragraph("\n"));
            }

            if (advice.getSummary() != null && !advice.getSummary().isEmpty()) {
                addParagraph(document, "Summary:", FONT_SIZE_NORMAL, TextAlignment.LEFT);
                addParagraph(document, advice.getSummary(), FONT_SIZE_SMALL, TextAlignment.LEFT);
                document.add(new Paragraph("\n"));
            }
        }

        addParagraph(document, "General Tips for Loan Closure:", FONT_SIZE_NORMAL, TextAlignment.LEFT);
        List<String> tips = List.of(
                "Try to maintain the recommended extra EMI consistently",
                "Make part payments whenever you have surplus funds",
                "Avoid taking additional debt during this period",
                "Monitor your loan account regularly for prepayment charges"
        );

        for (String tip : tips) {
            addParagraph(document, "• " + tip, FONT_SIZE_SMALL, TextAlignment.LEFT);
        }

        document.add(new Paragraph("\n"));
    }

    private void addLoanPrioritySection(Document document, StrategyResult result) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        if (result.getLoanPriority() != null && !result.getLoanPriority().isEmpty()) {
            addSectionTitle(document, "📌 LOAN PRIORITY ORDER", font);
            addParagraph(document, "Focus on closing loans in this order to maximize interest savings:", FONT_SIZE_NORMAL, TextAlignment.LEFT);
            document.add(new Paragraph("\n"));

            for (int i = 0; i < result.getLoanPriority().size(); i++) {
                addParagraph(document, (i + 1) + ". " + result.getLoanPriority().get(i), FONT_SIZE_SMALL, TextAlignment.LEFT);
            }

            document.add(new Paragraph("\n"));
        }
    }

    private void addAllStrategiesComparison(Document document, StrategyResult result) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        if (result.getAllStrategies() != null && !result.getAllStrategies().isEmpty()) {
            addSectionTitle(document, "📈 ALL AVAILABLE STRATEGIES", font);

            Table table = new Table(4);
            table.setWidth(UnitValue.createPercentValue(100));

            addHeaderCell(table, "Strategy", font);
            addHeaderCell(table, "EMI (₹)", font);
            addHeaderCell(table, "Interest Saved (₹)", font);
            addHeaderCell(table, "Tenure Reduced", font);

            for (LoanResponse strategy : result.getAllStrategies()) {
                String strategyName = truncate(strategy.getStrategy(), 20);
                long emi = strategy.getEmi().setScale(0, RoundingMode.HALF_UP).longValue();
                long interestSaved = strategy.getInterestSaved().setScale(0, RoundingMode.HALF_UP).longValue();
                int tenureReduced = strategy.getTenureReducedMonths();

                addTableCell(table, strategyName, font);
                addTableCell(table, String.format("₹ %,d", emi), font);
                addTableCell(table, String.format("₹ %,d", interestSaved), font);
                addTableCell(table, tenureReduced + " months", font);
            }

            document.add(table);
            document.add(new Paragraph("\n"));
        }
    }

    private void addFooter(Document document) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        document.add(new Paragraph("\n"));
        addSectionTitle(document, "⚠️  IMPORTANT DISCLAIMERS", font);

        List<String> disclaimers = List.of(
                "This report is generated based on the input parameters provided and assumes consistent interest rates and payment schedules throughout the loan tenure.",
                "Actual loan repayment may vary due to changes in interest rates, bank policies, additional fees, or prepayment charges.",
                "The figures are approximations and should be verified with your lender before making financial decisions.",
                "Interest calculations assume simple compounding and monthly payments as specified."
        );

        for (String disclaimer : disclaimers) {
            addParagraph(document, "• " + disclaimer, FONT_SIZE_SMALL, TextAlignment.LEFT);
        }

        document.add(new Paragraph("\n"));
        addParagraph(document, "For more information or updates, please contact your financial advisor.", FONT_SIZE_SMALL, TextAlignment.CENTER);
        addParagraph(document, "Report Generated: " + LocalDateTime.now(), FONT_SIZE_SMALL, TextAlignment.CENTER);
    }

    // Helper methods
    private void addSectionTitle(Document document, String title, PdfFont font) {
        Paragraph para = new Paragraph(title)
                .setFont(font)
                .setFontSize(FONT_SIZE_SUBTITLE)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(5);
        document.add(para);
    }

    private void addSubsectionTitle(Document document, String title, PdfFont font) {
        Paragraph para = new Paragraph(title)
                .setFont(font)
                .setFontSize(FONT_SIZE_NORMAL)
                .setBold()
                .setMarginTop(8)
                .setMarginBottom(4);
        document.add(para);
    }

    private void addParagraph(Document document, String text, float fontSize, TextAlignment alignment) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        Paragraph para = new Paragraph(text)
                .setFont(font)
                .setFontSize(fontSize)
                .setTextAlignment(alignment)
                .setMarginBottom(3);
        document.add(para);
    }

    private void addTableRow(Table table, String label, String value, PdfFont font) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(font).setFontSize(FONT_SIZE_SMALL).setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(5);

        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(FONT_SIZE_SMALL))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSectionHeader(Table table, String header, PdfFont font) {
        Cell cell = new Cell(1, 2)
                .add(new Paragraph(header).setFont(font).setFontSize(FONT_SIZE_SMALL).setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
        table.addCell(cell);
    }

    private void addHeaderCell(Table table, String text, PdfFont font) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(FONT_SIZE_SMALL).setBold())
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceGray(0.9f))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
        table.addCell(cell);
    }

    private void addTableCell(Table table, String text, PdfFont font) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(FONT_SIZE_SMALL))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
        table.addCell(cell);
    }

    private String truncate(String str, int length) {
        if (str == null) {
            return "N/A";
        }
        return str.length() > length ? str.substring(0, length - 3) + "..." : str;
    }
}
