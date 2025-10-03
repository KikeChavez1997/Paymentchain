package com.paymentchain.product.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.paymentchain.product.entities.Product;
import com.paymentchain.product.repository.ProductRepository;

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
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/product")
public class ProductRestController {

    @Autowired
    ProductRepository productRepository;

    @GetMapping()
    public List<Product> list() {
        return productRepository.findAll();
    }

   @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        return productRepository.findById(id)
                .map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable long id, @RequestBody Product input) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(input.getName());
                    existing.setCode(input.getCode());
                    Product updated = productRepository.save(existing);
                    return new ResponseEntity<>(updated, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @PostMapping
    public ResponseEntity<?> post(@RequestBody Product input) {
        // No permitas que el cliente defina el id (ni version si la agregas)
        input.setId(null);
        // input.setVersion(null);
        Product saved = productRepository.save(input);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.getId())
            .toUri();

        return ResponseEntity.created(location).body(saved);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id){
        if (!productRepository.existsById(id)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    

}
