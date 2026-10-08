package com.gallery.validation;

import com.gallery.annotation.ValidPhotoAccess;
import com.gallery.dto.PhotoRequest;
import com.gallery.model.AccessLevel;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PhotoAccessValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private PhotoRequest photo(AccessLevel level, String url, Set<Long> tagIds) {
        return new PhotoRequest("Sunset", url, "desc", level, null, tagIds);
    }

    private Set<String> messages(Set<ConstraintViolation<PhotoRequest>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void annotation_declaresValidatorViaConstraintMetaAnnotation() {
        Constraint constraint = ValidPhotoAccess.class.getAnnotation(Constraint.class);

        assertNotNull(constraint);
        assertTrue(Arrays.asList(constraint.validatedBy()).contains(PhotoAccessValidator.class));
    }

    @Test
    void validate_violatingDto_validatorIsInvokedAutomatically() {
        PhotoRequest request = photo(AccessLevel.PUBLIC, "https://example.com/a.jpg", Set.of());

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_publicPhotoWithTagAndHttps_hasNoViolations() {
        PhotoRequest request = photo(AccessLevel.PUBLIC, "https://example.com/a.jpg", Set.of(1L));

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_privatePhotoWithoutTagsAndWithHttp_hasNoViolations() {
        PhotoRequest request = photo(AccessLevel.PRIVATE, "http://example.com/a.jpg", null);

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_sharedPhotoWithoutTags_hasNoViolations() {
        PhotoRequest request = photo(AccessLevel.SHARED, "https://example.com/a.jpg", Set.of());

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_publicPhotoWithoutTags_violationExplainsThatTagIsRequired() {
        PhotoRequest request = photo(AccessLevel.PUBLIC, "https://example.com/a.jpg", Set.of());

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<PhotoRequest> violation = violations.iterator().next();
        assertEquals(PhotoAccessValidator.TAGS_REQUIRED, violation.getMessage());
        assertTrue(violation.getMessage().contains("at least one tag"));
    }

    @Test
    void validate_publicPhotoWithNullTags_violationExplainsThatTagIsRequired() {
        PhotoRequest request = photo(AccessLevel.PUBLIC, "https://example.com/a.jpg", null);

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertEquals(Set.of(PhotoAccessValidator.TAGS_REQUIRED), messages(violations));
    }

    @Test
    void validate_publicPhotoWithHttpUrl_violationExplainsThatHttpsIsRequired() {
        PhotoRequest request = photo(AccessLevel.PUBLIC, "http://example.com/a.jpg", Set.of(1L));

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<PhotoRequest> violation = violations.iterator().next();
        assertEquals(PhotoAccessValidator.HTTPS_REQUIRED, violation.getMessage());
        assertTrue(violation.getMessage().contains("https://"));
    }

    @Test
    void validate_publicPhotoBreaksBothRules_reportsBothViolations() {
        PhotoRequest request = photo(AccessLevel.PUBLIC, "http://example.com/a.jpg", Set.of());

        Set<ConstraintViolation<PhotoRequest>> violations = validator.validate(request);

        assertEquals(Set.of(PhotoAccessValidator.TAGS_REQUIRED, PhotoAccessValidator.HTTPS_REQUIRED),
                messages(violations));
    }
}
