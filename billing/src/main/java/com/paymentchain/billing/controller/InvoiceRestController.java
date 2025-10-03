package com.paymentchain.billing.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.paymentchain.billing.entities.Invoice;
import com.paymentchain.billing.repository.InvoiceRepository;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/billing")
public class InvoiceRestController {
    
    @Autowired
    InvoiceRepository billingRepository;
    
    @GetMapping()
    public List<Invoice> list() {
        return billingRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?>  get(@PathVariable long id) {
          return billingRepository.findById(id)
            .map(invoice -> new ResponseEntity<>(invoice, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable String id, @RequestBody Invoice input) {
        return null;
    }
    
    @PostMapping
    public ResponseEntity<?> post(@RequestBody Invoice input) {
        input.setId(null);
        Invoice save = billingRepository.save(input);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(save.getId())
            .toUri();

        return ResponseEntity.created(location).body(save);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return billingRepository.findById(Long.valueOf(id))
                .map(invoice -> {
                    billingRepository.delete(invoice);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    
}
