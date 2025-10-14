package com.paymentchain.transaction.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.paymentchain.transaction.entities.Transaction;
import com.paymentchain.transaction.repository.TransactionRepository;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    @GetMapping()
    public List<Transaction> list() {
        return transactionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> get(@PathVariable(name = "id") long id){
        return transactionRepository.findById(id)
                .map(transaction -> new ResponseEntity<>(transaction, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/customer/transactions")
    public ResponseEntity<List<Transaction>> get(@RequestParam(name = "accountIban") String accountIban) {
        List<Transaction> transactions = transactionRepository.findByAccountIban(accountIban);

        if (transactions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable(name = "id") long id, @RequestBody Transaction input){
        return transactionRepository.findById(id)
                .map(existing -> {
                    existing.setAmount(input.getAmount());
                    existing.setChannel(input.getChannel());
                    existing.setDate(input.getDate());
                    existing.setDescription(input.getDescription());
                    existing.setFee(input.getFee());
                    existing.setAccountIban(input.getAccountIban());
                    existing.setReference(input.getReference());
                    existing.setStatus(input.getStatus());
                    Transaction updated = transactionRepository.save(existing);
                    return new ResponseEntity<>(updated, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> post(@RequestBody Transaction input){
        input.setId(null);

        Transaction saved = transactionRepository.save(input);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.getId()).toUri();
        
        return ResponseEntity.created(location).body(saved);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id){
        if(!transactionRepository.existsById(id)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        transactionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
