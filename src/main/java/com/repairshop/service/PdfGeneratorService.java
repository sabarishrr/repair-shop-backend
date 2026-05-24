package com.repairshop.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.repairshop.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DeviceRgb PRIMARY_COLOR   = new DeviceRgb(37, 99, 235);   // Blue
    private static final DeviceRgb HEADER_BG       = new DeviceRgb(239, 246, 255); // Light blue
    private static final DeviceRgb TABLE_HEADER_BG = new DeviceRgb(30, 64, 175);   // Dark blue
    private static final DeviceRgb ALT_ROW_BG      = new DeviceRgb(248, 250, 252); // Light grey
    private static final DeviceRgb BORDER_COLOR     = new DeviceRgb(203, 213, 225);
    private static final DeviceRgb TEXT_MUTED       = new DeviceRgb(100, 116, 139);
    private static final float[] COL_WIDTHS_INVOICE = {3f, 20f, 5f, 4f, 5f, 5f, 5f, 6f};

    // ─── Sales Invoice ────────────────────────────────────────────────────────

    public byte[] buildInvoicePdf(SalesInvoice inv, CompanyDetails company) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        PdfFont bold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // Header
        addDocumentHeader(doc, bold, regular, company, "TAX INVOICE", inv.getStatus() != null ? inv.getStatus().name() : "");

        // Invoice meta table
        Table metaTable = new Table(UnitValue.createPercentArray(new float[]{1f, 1f})).useAllAvailableWidth();
        metaTable.setMarginBottom(8);
        addMetaCell(metaTable, "Invoice No.", inv.getInvoiceNumber(), bold, regular);
        addMetaCell(metaTable, "Date", inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DATE_FMT) : "-", bold, regular);
        addMetaCell(metaTable, "Sales Type", nvl(inv.getSalesType(), "CASH"), bold, regular);
        addMetaCell(metaTable, "Payment Method", nvl(inv.getPaymentMethod(), "CASH"), bold, regular);
        doc.add(metaTable);

        // Bill-to block
        addPartyBlock(doc, bold, regular, "Bill To", inv.getCustomer());

        // Items table
        Table items = new Table(UnitValue.createPercentArray(COL_WIDTHS_INVOICE)).useAllAvailableWidth();
        items.setMarginTop(10).setMarginBottom(10);
        String[] headers = {"#", "Description", "HSN", "Qty", "Rate", "Disc.", "GST%", "Amount"};
        addTableHeaders(items, headers, bold);

        int row = 0;
        for (SalesInvoiceItem item : inv.getItems()) {
            DeviceRgb rowBg = (row % 2 == 0) ? null : ALT_ROW_BG;
            addItemCell(items, String.valueOf(++row), regular, TextAlignment.CENTER, rowBg);
            addItemCell(items, nvl(item.getDescription(), "-"), regular, TextAlignment.LEFT, rowBg);
            addItemCell(items, nvl(item.getHsn(), "-"), regular, TextAlignment.CENTER, rowBg);
            addItemCell(items, String.valueOf(item.getQuantity()), regular, TextAlignment.CENTER, rowBg);
            addItemCell(items, fmt(item.getUnitPrice()), regular, TextAlignment.RIGHT, rowBg);
            addItemCell(items, fmt(item.getDiscount()), regular, TextAlignment.RIGHT, rowBg);
            addItemCell(items, item.getGstPercentage() != null ? item.getGstPercentage() + "%" : "0%", regular, TextAlignment.CENTER, rowBg);
            addItemCell(items, "₹ " + fmt(item.getTaxableValue()), bold, TextAlignment.RIGHT, rowBg);
        }
        doc.add(items);

        // Totals
        Table totals = new Table(UnitValue.createPercentArray(new float[]{3f, 1f})).useAllAvailableWidth();
        addTotalRow(totals, "Taxable Value", fmt(inv.getTotalTaxableValue()), regular, bold, false);
        if (inv.getTotalCgst() != null && inv.getTotalCgst().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totals, "CGST", fmt(inv.getTotalCgst()), regular, bold, false);
            addTotalRow(totals, "SGST", fmt(inv.getTotalSgst()), regular, bold, false);
        }
        if (inv.getTotalIgst() != null && inv.getTotalIgst().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totals, "IGST", fmt(inv.getTotalIgst()), regular, bold, false);
        }
        addTotalRow(totals, "Grand Total", "₹ " + fmt(inv.getGrandTotal()), bold, bold, true);
        if (inv.getReceivedAmount() != null && inv.getReceivedAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totals, "Amount Received", "₹ " + fmt(inv.getReceivedAmount()), regular, bold, false);
            BigDecimal balance = inv.getGrandTotal().subtract(inv.getReceivedAmount());
            addTotalRow(totals, "Balance Due", "₹ " + fmt(balance), bold, bold, true);
        }
        doc.add(totals);

        addFooter(doc, regular, company);
        doc.close();
        return baos.toByteArray();
    }

    // ─── Quotation ────────────────────────────────────────────────────────────

    public byte[] buildQuotationPdf(Quotation quot, CompanyDetails company) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(new PdfDocument(new PdfWriter(baos)), PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        PdfFont bold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        addDocumentHeader(doc, bold, regular, company, "QUOTATION", "");

        Table meta = new Table(UnitValue.createPercentArray(new float[]{1f, 1f})).useAllAvailableWidth().setMarginBottom(8);
        addMetaCell(meta, "Quotation No.", "QT-" + quot.getId(), bold, regular);
        addMetaCell(meta, "Date", quot.getCreatedAt() != null ? quot.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")) : "-", bold, regular);
        addMetaCell(meta, "Validity", nvl(quot.getValidityTerms(), "-"), bold, regular);
        addMetaCell(meta, "Payment Terms", nvl(quot.getPaymentTerms(), "-"), bold, regular);
        doc.add(meta);

        addPartyBlock(doc, bold, regular, "To", quot.getCustomer());

        // Items
        Table items = new Table(UnitValue.createPercentArray(new float[]{3f, 18f, 5f, 4f, 6f, 5f, 6f})).useAllAvailableWidth();
        items.setMarginTop(10).setMarginBottom(10);
        addTableHeaders(items, new String[]{"#", "Product", "HSN", "Qty", "Rate", "GST%", "Amount"}, bold);

        int row = 0;
        for (QuotationItem item : quot.getItems()) {
            DeviceRgb rowBg = (row % 2 == 0) ? null : ALT_ROW_BG;
            addItemCell(items, String.valueOf(++row), regular, TextAlignment.CENTER, rowBg);
            String desc = item.getProduct() != null ? item.getProduct().getName() : "-";
            addItemCell(items, desc, regular, TextAlignment.LEFT, rowBg);
            String hsn = item.getProduct() != null ? nvl(item.getProduct().getHsn(), "-") : "-";
            addItemCell(items, hsn, regular, TextAlignment.CENTER, rowBg);
            addItemCell(items, String.valueOf(item.getQuantity()), regular, TextAlignment.CENTER, rowBg);
            addItemCell(items, "₹ " + fmt(item.getRate()), regular, TextAlignment.RIGHT, rowBg);
            addItemCell(items, item.getGstPercentage() != null ? item.getGstPercentage() + "%" : "0%", regular, TextAlignment.CENTER, rowBg);
            addItemCell(items, "₹ " + fmt(item.getTotalAmount()), bold, TextAlignment.RIGHT, rowBg);
        }
        doc.add(items);

        Table totals = new Table(UnitValue.createPercentArray(new float[]{3f, 1f})).useAllAvailableWidth();
        addTotalRow(totals, "Subtotal", fmt(quot.getSubTotal()), regular, bold, false);
        addTotalRow(totals, "GST Total", fmt(quot.getTaxTotal()), regular, bold, false);
        addTotalRow(totals, "Grand Total", "₹ " + fmt(quot.getGrandTotal()), bold, bold, true);
        doc.add(totals);

        // Terms
        if (quot.getSpecificTerms() != null && !quot.getSpecificTerms().isBlank()) {
            doc.add(new Paragraph("\nTerms & Conditions")
                .setFont(bold).setFontSize(9).setFontColor(PRIMARY_COLOR).setMarginTop(10));
            doc.add(new Paragraph(quot.getSpecificTerms()).setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED));
        }

        addFooter(doc, regular, company);
        doc.close();
        return baos.toByteArray();
    }

    // ─── Receipt ──────────────────────────────────────────────────────────────

    public byte[] buildReceiptPdf(Receipt receipt, CompanyDetails company) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(new PdfDocument(new PdfWriter(baos)), PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        PdfFont bold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        addDocumentHeader(doc, bold, regular, company, "PAYMENT RECEIPT", "PAID");

        Table meta = new Table(UnitValue.createPercentArray(new float[]{1f, 1f})).useAllAvailableWidth().setMarginBottom(8);
        addMetaCell(meta, "Receipt No.", receipt.getReceiptNumber(), bold, regular);
        addMetaCell(meta, "Date", receipt.getReceiptDate() != null ? receipt.getReceiptDate().format(DATE_FMT) : "-", bold, regular);
        addMetaCell(meta, "Payment Method", nvl(receipt.getPaymentMethod(), "-"), bold, regular);
        addMetaCell(meta, "Invoice Ref.", receipt.getSalesInvoice() != null ? receipt.getSalesInvoice().getInvoiceNumber() : "-", bold, regular);
        doc.add(meta);

        addPartyBlock(doc, bold, regular, "Received From", receipt.getCustomer());

        // Amount block
        Table amtBlock = new Table(UnitValue.createPercentArray(new float[]{1f})).useAllAvailableWidth();
        amtBlock.setMarginTop(16);
        Cell amtCell = new Cell().setPadding(16)
            .setBackgroundColor(HEADER_BG)
            .setBorder(new SolidBorder(PRIMARY_COLOR, 1.5f));
        amtCell.add(new Paragraph("Amount Received").setFont(regular).setFontSize(10).setFontColor(TEXT_MUTED));
        amtCell.add(new Paragraph("₹ " + fmt(receipt.getAmount())).setFont(bold).setFontSize(24).setFontColor(PRIMARY_COLOR));
        amtBlock.addCell(amtCell);
        doc.add(amtBlock);

        if (receipt.getNotes() != null && !receipt.getNotes().isBlank()) {
            doc.add(new Paragraph("\nNotes: " + receipt.getNotes()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED).setMarginTop(10));
        }

        addFooter(doc, regular, company);
        doc.close();
        return baos.toByteArray();
    }

    // ─── Payment (to Supplier) ────────────────────────────────────────────────

    public byte[] buildPaymentPdf(Payment payment, CompanyDetails company) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(new PdfDocument(new PdfWriter(baos)), PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        PdfFont bold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        addDocumentHeader(doc, bold, regular, company, "PAYMENT VOUCHER", "");

        Table meta = new Table(UnitValue.createPercentArray(new float[]{1f, 1f})).useAllAvailableWidth().setMarginBottom(8);
        addMetaCell(meta, "Payment No.", payment.getPaymentNumber(), bold, regular);
        addMetaCell(meta, "Date", payment.getPaymentDate() != null ? payment.getPaymentDate().format(DATE_FMT) : "-", bold, regular);
        addMetaCell(meta, "Payment Method", nvl(payment.getPaymentMethod(), "-"), bold, regular);
        addMetaCell(meta, "Purchase Invoice Ref.", payment.getPurchaseInvoice() != null ? payment.getPurchaseInvoice().getInvoiceNumber() : "-", bold, regular);
        doc.add(meta);

        // Supplier block
        if (payment.getSupplier() != null) {
            Supplier s = payment.getSupplier();
            doc.add(new Paragraph("Paid To").setFont(bold).setFontSize(9).setFontColor(TEXT_MUTED).setMarginTop(10));
            doc.add(new Paragraph(s.getName()).setFont(bold).setFontSize(13).setMarginBottom(2));
            if (s.getAddress() != null) doc.add(new Paragraph(s.getAddress()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED));
            if (s.getPhone() != null) doc.add(new Paragraph("Phone: " + s.getPhone()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED));
            if (s.getGstin() != null) doc.add(new Paragraph("GSTIN: " + s.getGstin()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED));
        }

        // Amount block
        Table amtBlock = new Table(UnitValue.createPercentArray(new float[]{1f})).useAllAvailableWidth().setMarginTop(16);
        Cell amtCell = new Cell().setPadding(16).setBackgroundColor(HEADER_BG).setBorder(new SolidBorder(PRIMARY_COLOR, 1.5f));
        amtCell.add(new Paragraph("Amount Paid").setFont(regular).setFontSize(10).setFontColor(TEXT_MUTED));
        amtCell.add(new Paragraph("₹ " + fmt(payment.getAmount())).setFont(bold).setFontSize(24).setFontColor(PRIMARY_COLOR));
        amtBlock.addCell(amtCell);
        doc.add(amtBlock);

        if (payment.getNotes() != null && !payment.getNotes().isBlank()) {
            doc.add(new Paragraph("\nNotes: " + payment.getNotes()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED).setMarginTop(10));
        }

        addFooter(doc, regular, company);
        doc.close();
        return baos.toByteArray();
    }

    // ─── Job Sheet ────────────────────────────────────────────────────────────

    public byte[] buildJobSheetPdf(JobSheet job, CompanyDetails company) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(new PdfDocument(new PdfWriter(baos)), PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        PdfFont bold = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        String docTitle = "DELIVERED".equals(job.getStatus() != null ? job.getStatus().name() : "") ? "CASH BILL" : "JOB SHEET";
        addDocumentHeader(doc, bold, regular, company, docTitle, job.getStatus() != null ? job.getStatus().name() : "");

        Table meta = new Table(UnitValue.createPercentArray(new float[]{1f, 1f})).useAllAvailableWidth().setMarginBottom(8);
        addMetaCell(meta, "Job No.", "JOB-" + job.getId(), bold, regular);
        addMetaCell(meta, "Received Date", job.getReceivedDate() != null ? job.getReceivedDate().format(DATE_FMT) : "-", bold, regular);
        addMetaCell(meta, "Status", job.getStatus() != null ? job.getStatus().name() : "-", bold, regular);
        addMetaCell(meta, "Technician", nvl(job.getTechnician(), "Unassigned"), bold, regular);
        if (job.getDeliveryDate() != null) addMetaCell(meta, "Expected Delivery", job.getDeliveryDate().format(DATE_FMT), bold, regular);
        if (job.getDeliveredDate() != null) addMetaCell(meta, "Delivered On", job.getDeliveredDate().format(DATE_FMT), bold, regular);
        doc.add(meta);

        addPartyBlock(doc, bold, regular, "Customer", job.getCustomer());

        // Device info table
        Table device = new Table(UnitValue.createPercentArray(new float[]{1f, 2f})).useAllAvailableWidth();
        device.setMarginTop(10).setMarginBottom(10);
        addTableHeaders(device, new String[]{"Field", "Details"}, bold);
        addDeviceRow(device, "Device Type", nvl(job.getDeviceType(), "-"), regular, 0);
        addDeviceRow(device, "Brand & Model", nvl(job.getBrand(), "-") + " " + nvl(job.getModel(), ""), regular, 1);
        addDeviceRow(device, "Serial Number", nvl(job.getSerialNumber(), "-"), regular, 0);
        addDeviceRow(device, "Reported Issue", nvl(job.getProblemDescription(), "-"), regular, 1);
        addDeviceRow(device, "Accessories", nvl(job.getAccessories(), "-"), regular, 0);
        if (job.getActionTaken() != null && !job.getActionTaken().isBlank())
            addDeviceRow(device, "Action Taken", job.getActionTaken(), regular, 1);
        if (job.getMaterialUsed() != null && !job.getMaterialUsed().isBlank())
            addDeviceRow(device, "Material Used", job.getMaterialUsed(), regular, 0);
        doc.add(device);

        Table totals = new Table(UnitValue.createPercentArray(new float[]{3f, 1f})).useAllAvailableWidth();
        if (job.getEstimatedCost() != null) addTotalRow(totals, "Estimated Cost", "₹ " + fmt(job.getEstimatedCost()), regular, bold, false);
        if (job.getFinalCost() != null) addTotalRow(totals, "Final Cost", "₹ " + fmt(job.getFinalCost()), bold, bold, true);
        doc.add(totals);

        addFooter(doc, regular, company);
        doc.close();
        return baos.toByteArray();
    }

    // ─── Shared Helpers ───────────────────────────────────────────────────────

    private void addDocumentHeader(Document doc, PdfFont bold, PdfFont regular,
                                   CompanyDetails company, String docType, String badge) throws IOException {
        Table header = new Table(UnitValue.createPercentArray(new float[]{2f, 1f})).useAllAvailableWidth();
        header.setMarginBottom(12);

        // Company info cell
        Cell companyCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        String companyName = company != null ? nvl(company.getCompanyName(), "Company") : "Company";
        companyCell.add(new Paragraph(companyName).setFont(bold).setFontSize(16).setFontColor(PRIMARY_COLOR));
        if (company != null && company.getAddress() != null)
            companyCell.add(new Paragraph(company.getAddress()).setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED));
        if (company != null && company.getPhone() != null)
            companyCell.add(new Paragraph("Phone: " + company.getPhone()).setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED));
        if (company != null && company.getEmail() != null)
            companyCell.add(new Paragraph("Email: " + company.getEmail()).setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED));
        if (company != null && company.getGstNumber() != null)
            companyCell.add(new Paragraph("GSTIN: " + company.getGstNumber()).setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED));
        header.addCell(companyCell);

        // Doc type cell
        Cell typeCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setTextAlignment(TextAlignment.RIGHT);
        typeCell.add(new Paragraph(docType).setFont(bold).setFontSize(18).setFontColor(PRIMARY_COLOR));
        if (badge != null && !badge.isBlank()) {
            typeCell.add(new Paragraph(badge).setFont(bold).setFontSize(9)
                .setFontColor("PAID".equals(badge) ? new DeviceRgb(16, 185, 129) : new DeviceRgb(245, 158, 11)));
        }
        header.addCell(typeCell);
        doc.add(header);

        // Divider line
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1.5f))
            .setStrokeColor(PRIMARY_COLOR).setMarginBottom(10));
    }

    private void addPartyBlock(Document doc, PdfFont bold, PdfFont regular, String label, Customer customer) {
        if (customer == null) return;
        doc.add(new Paragraph(label).setFont(bold).setFontSize(9).setFontColor(TEXT_MUTED).setMarginTop(8));
        doc.add(new Paragraph(nvl(customer.getCompanyName(), customer.getName())).setFont(bold).setFontSize(13).setMarginBottom(2));
        if (customer.getAddress() != null)
            doc.add(new Paragraph(customer.getAddress()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED));
        if (customer.getPhone() != null)
            doc.add(new Paragraph("Phone: " + customer.getPhone()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED));
        if (customer.getEmail() != null)
            doc.add(new Paragraph("Email: " + customer.getEmail()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED));
        if (customer.getGstin() != null)
            doc.add(new Paragraph("GSTIN: " + customer.getGstin()).setFont(regular).setFontSize(9).setFontColor(TEXT_MUTED));
    }

    private void addTableHeaders(Table table, String[] headers, PdfFont bold) {
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                .setBackgroundColor(TABLE_HEADER_BG)
                .setBorder(new SolidBorder(TABLE_HEADER_BG, 0.5f))
                .add(new Paragraph(h).setFont(bold).setFontSize(9).setFontColor(ColorConstants.WHITE)));
        }
    }

    private void addItemCell(Table table, String text, PdfFont font, TextAlignment align, DeviceRgb bg) {
        Cell c = new Cell()
            .setBorder(new SolidBorder(BORDER_COLOR, 0.5f))
            .add(new Paragraph(nvl(text, "-")).setFont(font).setFontSize(9).setTextAlignment(align));
        if (bg != null) c.setBackgroundColor(bg);
        table.addCell(c);
    }

    private void addMetaCell(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        Cell c = new Cell().setPadding(6).setBorder(new SolidBorder(BORDER_COLOR, 0.5f)).setBackgroundColor(HEADER_BG);
        c.add(new Paragraph(label).setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED));
        c.add(new Paragraph(nvl(value, "-")).setFont(bold).setFontSize(10));
        table.addCell(c);
    }

    private void addTotalRow(Table table, String label, String value, PdfFont labelFont, PdfFont valueFont, boolean highlight) {
        DeviceRgb bg = highlight ? HEADER_BG : null;
        Cell lc = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(4).setPaddingBottom(4);
        if (bg != null) lc.setBackgroundColor(bg);
        lc.add(new Paragraph(label).setFont(labelFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

        Cell vc = new Cell().setBorder(Border.NO_BORDER).setPaddingTop(4).setPaddingBottom(4);
        if (bg != null) vc.setBackgroundColor(bg);
        vc.add(new Paragraph(nvl(value, "0.00")).setFont(valueFont).setFontSize(10)
            .setFontColor(highlight ? PRIMARY_COLOR : ColorConstants.BLACK).setTextAlignment(TextAlignment.RIGHT));

        table.addCell(lc);
        table.addCell(vc);
    }

    private void addDeviceRow(Table table, String field, String value, PdfFont regular, int idx) {
        DeviceRgb bg = (idx % 2 != 0) ? ALT_ROW_BG : null;
        Cell fc = new Cell().setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
        if (bg != null) fc.setBackgroundColor(bg);
        fc.add(new Paragraph(field).setFont(regular).setFontSize(9).setBold().setFontColor(TEXT_MUTED));

        Cell vc = new Cell().setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
        if (bg != null) vc.setBackgroundColor(bg);
        vc.add(new Paragraph(value).setFont(regular).setFontSize(9));

        table.addCell(fc);
        table.addCell(vc);
    }

    private void addFooter(Document doc, PdfFont regular, CompanyDetails company) {
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
            .setStrokeColor(BORDER_COLOR).setMarginTop(20).setMarginBottom(8));
        String companyName = company != null ? nvl(company.getCompanyName(), "Company") : "Company";
        doc.add(new Paragraph("Thank you for your business! — " + companyName)
            .setFont(regular).setFontSize(8).setFontColor(TEXT_MUTED)
            .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("This is a computer-generated document.")
            .setFont(regular).setFontSize(7).setFontColor(TEXT_MUTED)
            .setTextAlignment(TextAlignment.CENTER));
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "0.00";
        return String.format("%,.2f", val);
    }

    private String nvl(String val, String defaultVal) {
        return (val == null || val.isBlank()) ? defaultVal : val;
    }
}
