package com.repairshop.service;

import com.repairshop.dto.ExtractedInvoiceResponse;
import com.repairshop.dto.ExtractedInvoiceResponse.ExtractedItem;
import com.repairshop.model.Product;
import com.repairshop.model.Supplier;
import com.repairshop.repository.ProductRepository;
import com.repairshop.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceExtractionService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public ExtractedInvoiceResponse extractInvoice(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return extractInvoice(inputStream);
        } catch (IOException e) {
            log.error("Failed to read uploaded PDF invoice file", e);
            throw new RuntimeException("Failed to read PDF file: " + e.getMessage());
        }
    }

    public ExtractedInvoiceResponse extractInvoice(InputStream pdfStream) {
        String rawText = "";
        try (PDDocument document = PDDocument.load(pdfStream)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            rawText = stripper.getText(document);
        } catch (IOException e) {
            log.error("Error reading PDF via PDFBox", e);
            throw new RuntimeException("Error reading PDF document: " + e.getMessage());
        }

        if (rawText == null || rawText.trim().isEmpty()) {
            throw new RuntimeException("The uploaded document is either empty or a scanned image. Searchable digital PDF is required.");
        }

        String[] lines = rawText.split("\\r?\\n");

        // 1. Extract Invoice Number
        String invoiceNumber = parseInvoiceNumber(rawText, lines);

        // 2. Extract Invoice Date
        LocalDate invoiceDate = parseInvoiceDate(rawText);

        // 3. Match Supplier
        Supplier matchedSupplier = matchSupplier(lines);
        Long supplierId = matchedSupplier != null ? matchedSupplier.getId() : null;
        String supplierName = matchedSupplier != null ? matchedSupplier.getName() : parseRawSupplierName(lines);

        // 4. Match Items
        List<ExtractedItem> extractedItems = matchInvoiceItems(lines);

        return new ExtractedInvoiceResponse(invoiceNumber, invoiceDate, supplierId, supplierName, extractedItems);
    }

    private String parseInvoiceNumber(String fullText, String[] lines) {
        // Regex patterns for invoice number
        Pattern[] patterns = {
                Pattern.compile("(?i)(?:invoice\\s*(?:no|number|#|id)?|inv\\s*(?:no|number)?)\\s*[:#-]?\\s*([a-zA-Z0-9-/]+)"),
                Pattern.compile("(?i)(?:bill\\s*(?:no|number|#)?)\\s*[:#-]?\\s*([a-zA-Z0-9-/]+)")
        };

        for (Pattern p : patterns) {
            Matcher m = p.matcher(fullText);
            if (m.find()) {
                String match = m.group(1).trim();
                // Avoid capturing general tax names or titles
                if (match.length() > 2 && !match.equalsIgnoreCase("gst")) {
                    return match;
                }
            }
        }

        // Fallback: check first 5 lines for any line containing "No" or "#"
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            String line = lines[i];
            if (line.contains("No:") || line.contains("No.") || line.contains("Number:")) {
                String clean = line.replaceAll("(?i)(invoice|inv|bill|no|number|\\.|:)", "").trim();
                if (clean.length() > 2) {
                    return clean;
                }
            }
        }

        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private LocalDate parseInvoiceDate(String fullText) {
        // Date regex patterns
        Pattern datePattern = Pattern.compile("(?i)(?:date|dated|invoice\\s*date)\\s*[:#-]?\\s*(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}\\s+[a-zA-Z]{3,9}\\s+\\d{4})");
        Matcher m = datePattern.matcher(fullText);
        if (m.find()) {
            String dateStr = m.group(1).trim();
            LocalDate parsed = parseDateString(dateStr);
            if (parsed != null) return parsed;
        }

        // Fallback: search for any date pattern anywhere
        Pattern generalDate = Pattern.compile("(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})");
        Matcher gm = generalDate.matcher(fullText);
        if (gm.find()) {
            LocalDate parsed = parseDateString(gm.group(1).trim());
            if (parsed != null) return parsed;
        }

        return LocalDate.now();
    }

    private LocalDate parseDateString(String dateStr) {
        // Clean characters
        String clean = dateStr.replace("/", "-").replace(".", "-");
        String[] formats = {"d-M-yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "d-MM-yyyy", "dd-M-yyyy", "d-M-yy", "dd-MM-yy"};
        for (String format : formats) {
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                // Keep trying
            }
        }
        return null;
    }

    private Supplier matchSupplier(String[] lines) {
        List<Supplier> dbSuppliers = supplierRepository.findAll();
        if (dbSuppliers.isEmpty()) return null;

        // suppliers are always listed in the first 15 lines of the invoice header
        int headerLines = Math.min(15, lines.length);
        for (int i = 0; i < headerLines; i++) {
            String line = lines[i].toLowerCase();
            for (Supplier supplier : dbSuppliers) {
                String name = supplier.getName().toLowerCase();
                // Fuzzy check: if the invoice line contains the supplier's name (or first 8 chars)
                if (name.length() > 3 && (line.contains(name) || (name.length() > 8 && line.contains(name.substring(0, 8))))) {
                    return supplier;
                }
            }
        }
        return null;
    }

    private String parseRawSupplierName(String[] lines) {
        // Fallback if supplier not in database: capture first non-empty line in header that isn't general invoice headers
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.toLowerCase().contains("invoice") && !line.toLowerCase().contains("tax") && line.length() > 3) {
                return line;
            }
        }
        return "Unknown Vendor";
    }

    private List<ExtractedItem> matchInvoiceItems(String[] lines) {
        List<Product> products = productRepository.findAll();
        List<ExtractedItem> extractedItems = new ArrayList<>();

        if (products.isEmpty()) return extractedItems;

        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            for (Product product : products) {
                String name = product.getName().toLowerCase();
                // If a line contains the product name
                if (name.length() > 4 && lowerLine.contains(name)) {
                    // Extract numbers from the line (e.g. quantity and rate)
                    List<Double> numbers = extractNumbers(line);
                    
                    int qty = 1;
                    BigDecimal rate = product.getPurchasePrice() != null ? product.getPurchasePrice() : product.getRate();

                    // If we found numbers, let's map them smartly
                    if (!numbers.isEmpty()) {
                        // Usually, quantity is a small integer (like 1 to 50), and rate is a larger number
                        Optional<Double> qtyOpt = numbers.stream()
                                .filter(n -> n == Math.floor(n) && n >= 1 && n <= 100)
                                .findFirst();
                        if (qtyOpt.isPresent()) {
                            qty = qtyOpt.get().intValue();
                            numbers.remove(qtyOpt.get());
                        }

                        // Rate is usually the largest remaining decimal number, or if none, fallback to product rate
                        Optional<Double> rateOpt = numbers.stream()
                                .filter(n -> n > 10)
                                .max(Double::compare);
                        if (rateOpt.isPresent()) {
                            rate = BigDecimal.valueOf(rateOpt.get());
                        }
                    }

                    extractedItems.add(new ExtractedItem(product.getId(), product.getName(), qty, rate));
                    break; // Move to next line once a product is matched to avoid duplicate matches on same line
                }
            }
        }

        // Fallback: If no products matched, check if there's any generic line item formatting
        if (extractedItems.isEmpty()) {
            // Find lines that have a description followed by quantity and decimals
            for (String line : lines) {
                if (line.matches(".*\\b\\d+\\b.*\\b\\d+\\.\\d{2}\\b.*")) {
                    List<Double> numbers = extractNumbers(line);
                    if (numbers.size() >= 2) {
                        String desc = line.replaceAll("[0-9.,₹$]", "").trim();
                        if (desc.length() > 3 && !desc.toLowerCase().contains("total") && !desc.toLowerCase().contains("subtotal")) {
                            int qty = numbers.get(0).intValue();
                            BigDecimal rate = BigDecimal.valueOf(numbers.get(numbers.size() - 1));
                            extractedItems.add(new ExtractedItem(null, desc, qty, rate));
                        }
                    }
                }
            }
        }

        return extractedItems;
    }

    private List<Double> extractNumbers(String line) {
        List<Double> numbers = new ArrayList<>();
        // Match numbers including decimals, ignoring currency symbols
        Pattern p = Pattern.compile("(\\b\\d+\\.\\d{2}\\b|\\b\\d+\\b)");
        Matcher m = p.matcher(line.replace(",", "")); // remove commas to parse decimals cleanly
        while (m.find()) {
            try {
                numbers.add(Double.parseDouble(m.group(1)));
            } catch (NumberFormatException e) {
                // skip
            }
        }
        return numbers;
    }
}
