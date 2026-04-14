package com.loan.closure.download;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.*;
import com.loan.closure.entity.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PdfFileService {

    private static final float HEADER = 20f;

    private static final float SUB = 13f;

    private static final float SMALL = 9f;

    public byte[] generateStrategyPdfReport(StrategyResult result) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            addHeader(document);
            addExecutiveSummary(document, result);
            addFinancialSummary(document, result);
            addRecommendationSection(document, result);

            addComparisonChart(document, result);   // ✅ Chart 1
            addPieChart(document, result); // ✅ Chart 2

            addInsights(document);
            addAdviceSection(document, result);
            addLoanPriority(document, result);
            addAllStrategies(document, result);
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ================= HEADER =================
    private void addHeader(Document doc) throws Exception {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        doc.add(new Paragraph("📊 LOAN CLOSURE REPORT")
                .setFont(font)
                .setFontSize(HEADER)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("Generated: " + LocalDateTime.now())
                .setFontSize(SMALL)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("\n"));
    }

    // ================= SUMMARY =================
    private void addExecutiveSummary(Document doc, StrategyResult result) throws Exception {
        addSection(doc, "🚀 QUICK SUMMARY");

        if (result == null || result.getRecommendedStrategy() == null) return;

        LoanResponse best = result.getRecommendedStrategy();

        String content = String.format(
                "✔ %s (%s)\n\nMonthly EMI: %s\nInterest Saved: %s\nLoan closes %d months earlier",
                safeText(best.getLoanName()),
                safeText(best.getStrategy()),
                safeAmount(best.getEmi()),
                safeAmount(best.getInterestSaved()),
                best.getTenureReducedMonths()
        );

        addHighlightBox(doc, content);
    }

    // ================= FINANCIAL =================
    private void addFinancialSummary(Document doc, StrategyResult result) throws Exception {
        if (result == null || result.getFinancialSummary() == null) return;

        FinancialSummary fs = result.getFinancialSummary();

        addSection(doc, "📋 FINANCIAL SUMMARY");

        Table table = new Table(2).useAllAvailableWidth();
        addRow(table, "Income", safeAmount(fs.getMonthlyIncome()));
        addRow(table, "Expenses", safeAmount(fs.getTotalExpenses()));
        addRow(table, "Total EMI", safeAmount(fs.getTotalLoanEmi()));
        addRow(table, "Disposable", safeAmount(fs.getDisposableIncome()));

        doc.add(table);
    }

    // ================= RECOMMENDATION =================
    private void addRecommendationSection(Document doc, StrategyResult result) throws Exception {
        addSection(doc, "🎯 RECOMMENDED STRATEGY");

        if (result == null || result.getRecommendedStrategy() == null) return;

        LoanResponse best = result.getRecommendedStrategy();

        addHighlightBox(doc,
                "Focus on: " + safeText(best.getLoanName()) +
                        "\nSave: " + safeAmount(best.getInterestSaved()) +
                        "\nTenure Reduced: " + best.getTenureReducedMonths() + " months"
        );
    }

    // ================= CHART 1 =================
    private void addComparisonChart(Document doc, StrategyResult result) throws Exception {

        if (result == null || result.getRecommendedStrategy() == null) return;

        DefaultCategoryDataset dataset = getDefaultCategoryDataset(result);

        JFreeChart chart = ChartFactory.createBarChart(
                "Before vs After Strategy",
                "Scenario",
                "Amount (₹)",
                dataset
        );

        styleChart(chart);

        doc.add(new Paragraph("\n📊 Before vs After Strategy"));

        Image img = chartToImage(chart);
        img.scaleToFit(500, 300);
        doc.add(img);
    }

    private static DefaultCategoryDataset getDefaultCategoryDataset(StrategyResult result) {
        LoanResponse best = result.getRecommendedStrategy();

        double normal = best.getTotalInterestNormal() != null
                ? best.getTotalInterestNormal().doubleValue() : 0;

        double reduced = best.getTotalInterestWithExtra() != null
                ? best.getTotalInterestWithExtra().doubleValue() : 0;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(normal, "Interest", "Before");
        dataset.addValue(reduced, "Interest", "After");
        return dataset;
    }

    // ================= INSIGHTS =================
    private void addInsights(Document doc) throws Exception {
        addSection(doc, "🧠 INSIGHTS");

        List<String> list = List.of(
                "Increase EMI using surplus income",
                "Focus on high-interest loans first",
                "Prepayments reduce long-term burden",
                "Maintain emergency fund"
        );

        for (String s : list) {
            doc.add(new Paragraph("✔ " + s).setFontSize(SMALL));
        }
    }

    // ================= ADVICE =================
    private void addAdviceSection(Document doc, StrategyResult result) throws Exception {
        addSection(doc, "💡 ADVICE");

        if (result == null || result.getAdvice() == null) return;

        StrategyAdvice a = result.getAdvice();

        if (a.getExtraEmiRecommended() != null)
            doc.add(new Paragraph("Extra EMI: " + safeAmount(a.getExtraEmiRecommended())));

        if (a.getPartPaymentPlan() != null)
            doc.add(new Paragraph("Plan: " + a.getPartPaymentPlan()));

        if (a.getSummary() != null)
            doc.add(new Paragraph("Summary: " + a.getSummary()));
    }

    // ================= PRIORITY =================
    private void addLoanPriority(Document doc, StrategyResult result) throws Exception {
        addSection(doc, "📌 LOAN PRIORITY");

        if (result == null || result.getLoanPriority() == null) return;

        for (String s : result.getLoanPriority()) {
            doc.add(new Paragraph("• " + s).setFontSize(SMALL));
        }
    }

    // ================= TABLE =================
    private void addAllStrategies(Document doc, StrategyResult result) throws Exception {
        addSection(doc, "📊 ALL STRATEGIES");

        if (result == null || result.getAllStrategies() == null) return;

        Table t = new Table(new float[]{3, 2, 2}).useAllAvailableWidth();

        addHeaderCell(t, "Loan");
        addHeaderCell(t, "EMI");
        addHeaderCell(t, "Saved");

        for (LoanResponse s : result.getAllStrategies()) {
            addCell(t, safeText(s.getLoanName()));
            addCell(t, safeAmount(s.getEmi()));
            addCell(t, safeAmount(s.getInterestSaved()));
        }

        doc.add(t);
    }

    // ================= FOOTER =================
    private void addFooter(Document doc) {
        doc.add(new Paragraph("\n⚠ Estimated report. Verify with bank.")
                .setFontSize(SMALL)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // ================= HELPERS =================
    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getPlot().setBackgroundPaint(Color.LIGHT_GRAY);
    }

    private Image chartToImage(JFreeChart chart) throws Exception {
        BufferedImage img = chart.createBufferedImage(800, 400);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return new Image(com.itextpdf.io.image.ImageDataFactory.create(baos.toByteArray()));
    }

    private void addSection(Document doc, String title) throws Exception {
        PdfFont f = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        doc.add(new Paragraph(title).setFont(f).setFontSize(SUB).setMarginTop(10));
    }

    private void addRow(Table t, String k, String v) {
        t.addCell(new Cell().add(new Paragraph(k)).setBorder(Border.NO_BORDER));
        t.addCell(new Cell().add(new Paragraph(v)).setBorder(Border.NO_BORDER));
    }

    private void addHeaderCell(Table t, String text) {
        t.addCell(new Cell().add(new Paragraph(text))
                .setBackgroundColor(new DeviceGray(0.85f))
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addCell(Table t, String text) {
        t.addCell(new Cell().add(new Paragraph(text))
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addHighlightBox(Document doc, String text) {
        Table t = new Table(1).useAllAvailableWidth();
        t.addCell(new Cell().add(new Paragraph(text))
                .setBackgroundColor(new DeviceRgb(220, 255, 220))
                .setPadding(10));
        doc.add(t);
    }

    private String safeAmount(Number v) {
        if (v == null) return "N/A";
        return formatAmount(v.longValue());
    }

    private String safeText(String v) {
        return v != null ? v : "N/A";
    }

    private String formatAmount(long a) {
        if (a >= 10000000) return String.format("₹ %.2f Cr", a / 10000000.0);
        if (a >= 100000) return String.format("₹ %.2f L", a / 100000.0);
        return "₹ " + String.format("%,d", a);
    }

    private Image createPieChart(double principal, double interest) throws Exception {

        DefaultPieDataset dataset = new DefaultPieDataset();

        double total = principal + interest;
        if (total == 0) total = 1;

        dataset.setValue(
                "Principal (" + formatAmount((long) principal) + " - " + (int)((principal / total) * 100) + "%)",
                principal
        );

        dataset.setValue(
                "Interest (" + formatAmount((long) interest) + " - " + (int)((interest / total) * 100) + "%)",
                interest
        );

        JFreeChart chart = ChartFactory.createPieChart(
                "Loan Breakdown",
                dataset,
                true,
                true,
                false
        );

        BufferedImage bufferedImage = chart.createBufferedImage(500, 300);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);

        return new Image(com.itextpdf.io.image.ImageDataFactory.create(baos.toByteArray()));
    }

    private void addPieChart(Document doc, StrategyResult result) throws Exception {

        if (result == null || result.getRecommendedStrategy() == null) return;

        LoanResponse best = result.getRecommendedStrategy();

        double interest = best.getTotalInterestWithExtra() != null
                ? best.getTotalInterestWithExtra().doubleValue()
                : 0;

        // ✅ Get correct principal from financial summary
        double principal = 0;

        if (result.getFinancialSummary() != null &&
                result.getFinancialSummary().getLoans() != null) {

            for (FinancialSummary.PerLoanSummary loan : result.getFinancialSummary().getLoans()) {
                if (loan.getLoanName().equals(best.getLoanName())) {
                    principal = loan.getLoanAmount().doubleValue();
                    break;
                }
            }
        }

        Image chartImage = createPieChart(principal, interest);

        doc.add(new Paragraph("\n📊 Loan Breakdown"));
        doc.add(chartImage.setAutoScale(true));
    }

}