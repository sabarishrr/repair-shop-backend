package com.repairshop.service;

import com.repairshop.dto.EmailRequest;
import com.repairshop.model.*;
import com.repairshop.repository.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final PdfGeneratorService pdfGeneratorService;
    private final CompanyDetailsService companyDetailsService;

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final QuotationRepository quotationRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final JobSheetRepository jobSheetRepository;

    @Value("${app.mail.from}")
    private String fromEmail;

    /**
     * Sends a formatted PDF of the given document as an email attachment.
     */
    public void sendDocumentEmail(EmailRequest req) throws MessagingException, IOException {
        CompanyDetails company = companyDetailsService.get();

        byte[] pdfBytes = generatePdf(req, company);
        String fileName  = buildFileName(req);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(req.getToEmail());
        helper.setSubject(req.getSubject());

        // HTML body
        String htmlBody = buildHtmlBody(req.getMessage(), company);
        helper.setText(htmlBody, true);

        // PDF attachment
        helper.addAttachment(fileName, new ByteArrayResource(pdfBytes), "application/pdf");

        mailSender.send(message);
        log.info("Email sent to {} for {} #{}", req.getToEmail(), req.getDocumentType(), req.getDocumentId());
    }

    // ── PDF dispatch ──────────────────────────────────────────────────────────

    private byte[] generatePdf(EmailRequest req, CompanyDetails company) throws IOException {
        Long id = req.getDocumentId();
        return switch (req.getDocumentType().toUpperCase()) {
            case "INVOICE" -> {
                SalesInvoice inv = salesInvoiceRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
                yield pdfGeneratorService.buildInvoicePdf(inv, company);
            }
            case "QUOTATION" -> {
                Quotation q = quotationRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Quotation not found: " + id));
                yield pdfGeneratorService.buildQuotationPdf(q, company);
            }
            case "RECEIPT" -> {
                Receipt r = receiptRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Receipt not found: " + id));
                yield pdfGeneratorService.buildReceiptPdf(r, company);
            }
            case "PAYMENT" -> {
                Payment p = paymentRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
                yield pdfGeneratorService.buildPaymentPdf(p, company);
            }
            case "JOBSHEET" -> {
                JobSheet j = jobSheetRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("JobSheet not found: " + id));
                yield pdfGeneratorService.buildJobSheetPdf(j, company);
            }
            default -> throw new IllegalArgumentException("Unknown document type: " + req.getDocumentType());
        };
    }

    private String buildFileName(EmailRequest req) {
        return switch (req.getDocumentType().toUpperCase()) {
            case "INVOICE"   -> "Invoice_" + req.getDocumentId() + ".pdf";
            case "QUOTATION" -> "Quotation_QT-" + req.getDocumentId() + ".pdf";
            case "RECEIPT"   -> "Receipt_" + req.getDocumentId() + ".pdf";
            case "PAYMENT"   -> "Payment_" + req.getDocumentId() + ".pdf";
            case "JOBSHEET"  -> "JobSheet_JOB-" + req.getDocumentId() + ".pdf";
            default          -> "Document_" + req.getDocumentId() + ".pdf";
        };
    }

    // ── HTML email body ───────────────────────────────────────────────────────

    private String buildHtmlBody(String userMessage, CompanyDetails company) {
        String companyName = company != null && company.getCompanyName() != null
                ? company.getCompanyName() : "Our Company";
        String safeMessage = userMessage != null ? userMessage.replace("\n", "<br>") : "";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, Helvetica, sans-serif; background: #f1f5f9; margin: 0; padding: 0;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background: #f1f5f9; padding: 32px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.08);">
                        <!-- Header -->
                        <tr>
                          <td style="background: linear-gradient(135deg, #1e40af 0%%, #2563eb 100%%);
                                     padding: 32px 40px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 1px;">%s</h1>
                          </td>
                        </tr>
                        <!-- Body -->
                        <tr>
                          <td style="padding: 32px 40px;">
                            <p style="font-size: 15px; color: #334155; line-height: 1.6; margin: 0 0 16px;">%s</p>
                            <p style="font-size: 14px; color: #64748b; margin: 0;">
                              Please find the attached PDF document for your reference.
                            </p>
                          </td>
                        </tr>
                        <!-- Divider -->
                        <tr><td style="padding: 0 40px;"><hr style="border: none; border-top: 1px solid #e2e8f0; margin: 0;"></td></tr>
                        <!-- Footer -->
                        <tr>
                          <td style="padding: 24px 40px; text-align: center;">
                            <p style="font-size: 12px; color: #94a3b8; margin: 0;">
                              This email was sent by <strong>%s</strong>. Please do not reply to this email.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(companyName, safeMessage, companyName);
    }
}
