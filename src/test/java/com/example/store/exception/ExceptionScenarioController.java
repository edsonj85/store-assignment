package com.example.store.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Fixture controller (not part of the real API) exercising each exception GlobalExceptionHandler covers.
@RestController
@RequestMapping("/test-exceptions")
@Validated
class ExceptionScenarioController {

    @GetMapping("/not-found")
    String notFound() {
        throw new ResourceNotFoundException("Order", 999L);
    }

    @PostMapping("/validate-body")
    String validateBody(@Valid @RequestBody Payload payload) {
        return payload.name();
    }

    @GetMapping("/validate-param")
    String validateParam(@RequestParam @Size(min = 3) String value) {
        return value;
    }

    @GetMapping("/{id}")
    String byId(@PathVariable Long id) {
        return "order-" + id;
    }

    @GetMapping("/boom")
    String boom() {
        throw new IllegalStateException("boom");
    }

    record Payload(@NotBlank String name) {}
}
