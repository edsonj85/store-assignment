package com.example.store.exception;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExceptionScenarioController.class)
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void resourceNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/test-exceptions/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order 999 not found"));
    }

    @Test
    void invalidRequestBodyReturns400WithFieldDetail() throws Exception {
        mockMvc.perform(post("/test-exceptions/validate-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExceptionScenarioController.Payload(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void constraintViolationOnQueryParamReturns400() throws Exception {
        mockMvc.perform(get("/test-exceptions/validate-param").param("value", "ab"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/test-exceptions/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void unhandledExceptionReturns500WithCorrelationIdAndNoStackTrace() throws Exception {
        mockMvc.perform(get("/test-exceptions/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
