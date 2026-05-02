package com.repairshop.controller;

import com.repairshop.model.State;
import com.repairshop.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/states")
@RequiredArgsConstructor
public class StateController {

    private final StateRepository repository;

    @GetMapping
    public List<State> getAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }
}
