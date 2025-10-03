package com.paymentchain.customer.controller;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.paymentchain.customer.entities.Customer;
import com.paymentchain.customer.entities.CustomerProduct;
import com.paymentchain.customer.repository.CustomerRepository;

import org.springframework.http.MediaType;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/customer")
public class CustomerRestController {

    @Autowired

    private final CustomerRepository customerRepository;
    private final WebClient.Builder webClientBuilder; // <- usa CamelCase consistente

    public CustomerRestController(CustomerRepository customerRepository, WebClient.Builder webClientBuilder) {
        this.customerRepository = customerRepository;
        this.webClientBuilder = webClientBuilder; // <- asignación correcta
    }

    @GetMapping()
    public List<Customer> list() {
        return customerRepository.findAll();
    }

   @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        return customerRepository.findById(id)
                .map(customer -> new ResponseEntity<>(customer, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable long id, @RequestBody Customer input) {
        return customerRepository.findById(id)
                .map(existing -> {
                    existing.setName(input.getName());
                    existing.setPhone(input.getPhone());
                    existing.setCode(input.getCode());
                    existing.setIban(input.getIban());
                    existing.setSurname(input.getSurname());
                    Customer updated = customerRepository.save(existing);
                    return new ResponseEntity<>(updated, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @PostMapping
    @Transactional
    public ResponseEntity<?> post(@RequestBody Customer input) {
        input.setId(null);

        input.getProducts().forEach(x -> x.setCustomer(input));

        if (input.getProducts() != null) {
            for (CustomerProduct cp : input.getProducts()) {
                cp.setId(null);             // NUEVO
                cp.setCustomer(input);      // back-reference
                // cp.setProductId(...) ya viene en el JSON
            }
        }

        Customer saved = customerRepository.save(input);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(saved);
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (!customerRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        customerRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // 204
    }

    @GetMapping("/full")
    public Mono<ResponseEntity<Customer>> getByCode(@RequestParam String code) {
            return Mono.fromCallable(() -> customerRepository.findByCode(code))   // Optional<Customer>
                .subscribeOn(Schedulers.boundedElastic())                         // offload JPA (bloqueante)
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty))       // Optional -> Mono<Customer>
                .flatMap(customer -> {
                    var products = customer.getProducts();
                    if (products == null || products.isEmpty()) {
                        return Mono.just(customer);                                // no hay nada que enriquecer
                    }
                    return Flux.fromIterable(products)
                        .flatMap(cp -> {
                            Long productId = cp.getProductId();
                            if (productId == null) return Mono.just(cp);           // sin productId, seguimos

                            return getProductName(productId)                       // Mono<String>
                                .onErrorResume(e -> Mono.empty())                  // si falla, tratamos como vacío
                                .switchIfEmpty(Mono.just("N/A"))                   // valor por defecto si vacío
                                .doOnNext(cp::setProductName)                      // setea el nombre en el detalle
                                .thenReturn(cp);                                   // devolvemos el cp para completar el flujo
                        })
                        .then(Mono.just(customer));                                // cuando terminen todos, emitimos customer
                })
                .map(ResponseEntity::ok)                                           // 200 OK + body
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }


    private Mono<String> getProductName(long id) {

        WebClient client = webClientBuilder
                .baseUrl("http://localhost:8083/product")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return client.get()
                .uri("/{id}", id)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> node.path("name").asText(null));
    }
    

}
