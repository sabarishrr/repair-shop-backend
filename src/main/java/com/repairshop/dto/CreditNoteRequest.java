package com.repairshop.dto;

import com.repairshop.model.CreditNote.CreditNoteReason;
import com.repairshop.model.CreditNote.CreditNoteStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreditNoteRequest {
    private String noteNumber;
    private LocalDate noteDate;
    private Long salesInvoiceId; // Optional link
    private Long customerId;
    private CreditNoteReason reason;
    private CreditNoteStatus status;
    private String notes;
    private List<CreditNoteItemRequest> items;
}
