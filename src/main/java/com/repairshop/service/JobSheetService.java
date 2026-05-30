package com.repairshop.service;

import com.repairshop.dto.DashboardStats;
import com.repairshop.dto.JobSheetRequest;
import com.repairshop.dto.StatusUpdateRequest;
import com.repairshop.model.Customer;
import com.repairshop.model.JobSheet;
import com.repairshop.model.JobStatus;
import com.repairshop.repository.JobSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class JobSheetService {

    private final JobSheetRepository jobSheetRepository;
    private final CustomerService customerService;

    public DashboardStats getStats() {
        long totalJobs = jobSheetRepository.count();
        long received = jobSheetRepository.countByStatus(JobStatus.RECEIVED);
        long diagnosing = jobSheetRepository.countByStatus(JobStatus.DIAGNOSING);
        long awaitingParts = jobSheetRepository.countByStatus(JobStatus.AWAITING_PARTS);
        long inRepair = jobSheetRepository.countByStatus(JobStatus.IN_REPAIR);
        long readyForPickup = jobSheetRepository.countByStatus(JobStatus.READY);
        long delivered = jobSheetRepository.countByStatus(JobStatus.DELIVERED);
        
        long pendingJobs = received + diagnosing + awaitingParts + inRepair;
        long deliveredToday = jobSheetRepository.countDeliveredToday();
        
        BigDecimal totalRevenue = jobSheetRepository.sumTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        List<JobSheet> recentJobs = jobSheetRepository.findTop10ByOrderByCreatedAtDesc();

        return new DashboardStats(
                totalJobs,
                received,
                diagnosing,
                awaitingParts,
                inRepair,
                readyForPickup,
                delivered,
                pendingJobs,
                deliveredToday,
                totalRevenue,
                recentJobs
        );
    }

    public List<JobSheet> getAll() {
        return jobSheetRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<JobSheet> getByStatus(JobStatus status) {
        return jobSheetRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<JobSheet> search(String query) {
        return jobSheetRepository.search(query);
    }

    public JobSheet getById(Long id) {
        return jobSheetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("JobSheet not found: " + id));
    }

    public JobSheet create(JobSheetRequest req) {
        JobSheet j = new JobSheet();
        j.setJobNumber(generateJobNumber());
        return save(j, req);
    }

    public JobSheet update(Long id, JobSheetRequest req) {
        JobSheet j = getById(id);
        // Block technicians from editing delivered jobs
        if (j.getStatus() == JobStatus.DELIVERED) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"))) {
                throw new RuntimeException("Technicians cannot edit delivered jobs");
            }
        }
        return save(j, req);
    }

    private JobSheet save(JobSheet j, JobSheetRequest req) {
        Customer c = customerService.getById(req.getCustomerId());
        j.setCustomer(c);
        
        j.setDeviceType(req.getDeviceType());
        j.setBrand(req.getBrand());
        j.setModel(req.getModel());
        j.setSerialNumber(req.getSerialNumber());
        j.setProblemDescription(req.getProblemDescription());
        j.setAccessories(req.getAccessories());
        j.setTechnician(req.getTechnician());
        j.setEstimatedCost(req.getEstimatedCost());
        j.setFinalCost(req.getFinalCost());
        j.setPaymentStatus(req.getPaymentStatus() != null ? req.getPaymentStatus() : j.getPaymentStatus());
        j.setPaymentMethod(req.getPaymentMethod());
        j.setMaterialUsed(req.getMaterialUsed());
        j.setActionTaken(req.getActionTaken());
        
        if (req.getStatus() != null) {
            j.setStatus(req.getStatus());
            // Auto-set deliveredDate when status changes to DELIVERED
            if (req.getStatus() == JobStatus.DELIVERED && j.getDeliveredDate() == null) {
                j.setDeliveredDate(LocalDate.now());
            } else if (req.getStatus() != JobStatus.DELIVERED) {
                j.setDeliveredDate(null);
            }
        }
        j.setNotes(req.getNotes());
        
        if (req.getReceivedDate() != null) {
            j.setReceivedDate(LocalDate.parse(req.getReceivedDate().toString())); // Ensure correct format or adjust based on DTO
        } else {
             j.setReceivedDate(LocalDate.now());
        }

        if (req.getDeliveryDate() != null && !req.getDeliveryDate().toString().isBlank()) {
            j.setDeliveryDate(LocalDate.parse(req.getDeliveryDate().toString()));
        }

        return jobSheetRepository.save(j);
    }

    public JobSheet updateStatus(Long id, StatusUpdateRequest req) {
        JobSheet j = getById(id);
        j.setStatus(req.getStatus());
        // Auto-set deliveredDate when status changes to DELIVERED
        if (req.getStatus() == JobStatus.DELIVERED) {
            j.setDeliveredDate(LocalDate.now());
        } else {
            j.setDeliveredDate(null);
        }
        if (req.getNotes() != null && !req.getNotes().isBlank()) {
            String existingNotes = j.getNotes() == null ? "" : j.getNotes() + "\n";
            j.setNotes(existingNotes + "[" + LocalDate.now() + "]: " + req.getNotes());
        }
        return jobSheetRepository.save(j);
    }

    public void delete(Long id) {
        jobSheetRepository.deleteById(id);
    }

    private String generateJobNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        int randomNum = new Random().nextInt(9000) + 1000;
        return "REP-" + dateStr + "-" + randomNum;
    }
}