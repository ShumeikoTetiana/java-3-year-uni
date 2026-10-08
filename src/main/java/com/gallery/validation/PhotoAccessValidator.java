package com.gallery.validation;

import com.gallery.annotation.ValidPhotoAccess;
import com.gallery.dto.PhotoRequest;
import com.gallery.model.AccessLevel;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhotoAccessValidator implements ConstraintValidator<ValidPhotoAccess, PhotoRequest> {

    public static final String TAGS_REQUIRED =
            "A PUBLIC photo must have at least one tag so that it can be found";
    public static final String HTTPS_REQUIRED =
            "A PUBLIC photo must use a secure URL starting with https://";

    @Override
    public boolean isValid(PhotoRequest photo, ConstraintValidatorContext context) {
        if (photo == null || photo.accessLevel() != AccessLevel.PUBLIC) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (photo.tagIds() == null || photo.tagIds().isEmpty()) {
            addViolation(context, TAGS_REQUIRED);
            valid = false;
        }
        if (photo.url() != null && !photo.url().isBlank()
                && !photo.url().toLowerCase().startsWith("https://")) {
            addViolation(context, HTTPS_REQUIRED);
            valid = false;
        }
        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
