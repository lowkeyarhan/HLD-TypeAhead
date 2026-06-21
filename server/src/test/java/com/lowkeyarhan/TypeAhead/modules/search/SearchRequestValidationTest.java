package com.lowkeyarhan.TypeAhead.modules.search;

import com.lowkeyarhan.TypeAhead.modules.search.dto.SearchRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Unit test verifying SearchRequestDTO constraints.
class SearchRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidationBlankQueryFails() {
        SearchRequestDTO blank = new SearchRequestDTO("   ");
        Set<ConstraintViolation<SearchRequestDTO>> violations = validator.validate(blank);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testValidationLongQueryFails() {
        String longQuery = "a".repeat(101);
        SearchRequestDTO oversized = new SearchRequestDTO(longQuery);
        Set<ConstraintViolation<SearchRequestDTO>> violations = validator.validate(oversized);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testValidationValidQuerySucceeds() {
        SearchRequestDTO valid = new SearchRequestDTO("valid query");
        Set<ConstraintViolation<SearchRequestDTO>> violations = validator.validate(valid);
        assertThat(violations).isEmpty();
    }
}
