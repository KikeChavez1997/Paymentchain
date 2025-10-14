package com.paymentchain.customer.controller;
import com.fasterxml.jackson.databind.JsonNode;

import com.paymentchain.customer.api.dto.CustomerCreateRequest;

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
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
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
    private final WebClient.Builder webClientBuilder; // usa CamelCase consistente

    public CustomerRestController(CustomerRepository customerRepository, WebClient.Builder webClientBuilder) {
        this.customerRepository = customerRepository;
        this.webClientBuilder = webClientBuilder; // asignación correcta
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
    public ResponseEntity<?> post(@Valid @RequestBody CustomerCreateRequest input) {

        // Mapear DTO con Entidad
        Customer entity = new Customer();
        entity.setId(null);
        entity.setName(input.getName());
        entity.setPhone(input.getPhone());
        entity.setCode(input.getCode());
        entity.setIban(input.getIban());
        entity.setSurname(input.getSurname());
        entity.setAddress(input.getAddress());

        if (input.getProducts() != null) {
            List<CustomerProduct> products = input.getProducts().stream().map(dto -> {
                CustomerProduct cp = new CustomerProduct();
                cp.setId(null);
                cp.setProductId(dto.getProductId());
                cp.setCustomer(entity);                 
                return cp;
            }).toList();
            entity.setProducts(products);
        } else {
            entity.setProducts(List.of());
        }

        Customer saved = customerRepository.save(entity);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.getId())
            .toUri();

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
        return Mono.fromCallable(() -> customerRepository.findOneByCode(code)) // ya trae products
            .subscribeOn(Schedulers.boundedElastic()) // offload JPA bloqueante
            .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty))       // Optional - Mono<Customer>
            .flatMap(customer -> {
                var products = customer.getProducts(); // ya inicializados por @EntityGraph

                Mono<Customer> enriched =
                    (products == null || products.isEmpty())
                        ? Mono.just(customer)
                        : Flux.fromIterable(products)
                            .flatMap(cp -> {
                                Long productId = cp.getProductId();
                                if (productId == null) return Mono.just(cp);
                                return getProductName(productId)               // Mono<String>
                                    .onErrorResume(e -> Mono.empty())          // tolera fallo del micro productos
                                    .switchIfEmpty(Mono.just("N/A"))           // nombre por defecto si vacío
                                    .doOnNext(cp::setProductName)              // setea nombre en el detalle
                                    .thenReturn(cp);
                            })
                            .then(Mono.just(customer));

                // Enriquecer con transacciones (reactivo, sin bloquear)
                return enriched.flatMap(cust ->
                    getTransactions(cust.getIban())                            // Mono<List<?>>
                        .onErrorReturn(List.of())                              // si falla, lista vacía
                        .doOnNext(cust::setTransactions)                       // setea en el customer
                        .thenReturn(cust)
                );
            })
            .map(ResponseEntity::ok)                                           // 200 OK
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build()); // 404 si no hay customer
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

    private Mono<List<Map<String, Object>>> getTransactions(String accountIban) {
    WebClient client = webClientBuilder
        .baseUrl("http://localhost:8082/transaction")
        .build();

    return client.get()
        .uri(uriBuilder -> uriBuilder
            .path("/customer/transactions") // asegúrate de que coincida con tu endpoint real
            .queryParam("accountIban", accountIban)
            .build())
        .exchangeToFlux(resp -> {
            if (resp.statusCode().is2xxSuccessful()) {
                
                return resp.bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {});
            }
            if (resp.statusCode().value() == HttpStatus.NOT_FOUND.value()) {
                
                return Flux.<Map<String, Object>>empty();
            }
            
            return Flux.<Map<String, Object>>error(
                new IllegalStateException("Error remoto: " + resp.statusCode())
            );
        })
        .collectList();
    }


}
