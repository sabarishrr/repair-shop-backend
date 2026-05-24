package com.repairshop.dto;

import com.repairshop.model.DebitNote.DebitNoteReason;
import com.repairshop.model.DebitNote.DebitNoteStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DebitNoteRequest {
    private String noteNumber;
    private LocalDate noteDate;
    private Long purchaseInvoiceId; // Optional link
    private Long supplierId;
    private DebitNoteReason reason;
    private DebitNoteStatus status;
    private String notes;
    private List<DebitNoteItemRequest> items;
}
