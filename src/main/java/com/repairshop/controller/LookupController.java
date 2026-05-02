package com.repairshop.controller;

import com.repairshop.model.Brand;
import com.repairshop.model.CommonIssue;
import com.repairshop.repository.BrandRepository;
import com.repairshop.repository.CommonIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final BrandRepository brandRepository;
    private final CommonIssueRepository commonIssueRepository;

    @GetMapping("/brands")
    public List<Brand> getBrands() {
        return brandRepository.findAll();
    }

    @PostMapping("/brands")
    public Brand createBrand(@RequestBody Brand brand) {
        return brandRepository.save(brand);
    }


    @GetMapping("/issues")
    public List<CommonIssue> getIssues() {
        return commonIssueRepository.findAll();
    }

    @PostMapping("/issues")
    public CommonIssue createIssue(@RequestBody CommonIssue issue) {
        return commonIssueRepository.save(issue);
    }
}
