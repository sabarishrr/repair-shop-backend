package com.repairshop.dto;

import lombok.Data;

@Data
public class EmailRequest {
    private String toEmail;
    private String subject;
    private String message;        // Short cover note shown in email body
    private String documentType;   // INVOICE | QUOTATION | RECEIPT | PAYMENT | JOBSHEET
    private Long documentId;
}
