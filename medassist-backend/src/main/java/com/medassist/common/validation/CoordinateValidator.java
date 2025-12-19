package com.medassist.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

public class CoordinateValidator implements ConstraintValidator<ValidCoordinates, Object> {

    private String latitudeField;
    private String longitudeField;

    @Override
    public void initialize(ValidCoordinates constraintAnnotation) {
        this.latitudeField = constraintAnnotation.latitudeField();
        this.longitudeField = constraintAnnotation.longitudeField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let other validators handle null checks
        }

        try {
            Field latField = value.getClass().getDeclaredField(latitudeField);
            Field lonField = value.getClass().getDeclaredField(longitudeField);

            latField.setAccessible(true);
            lonField.setAccessible(true);

            Double latitude = (Double) latField.get(value);
            Double longitude = (Double) lonField.get(value);

            if (latitude == null || longitude == null) {
                return false;
            }

            boolean validLatitude = latitude >= -90.0 && latitude <= 90.0;
            boolean validLongitude = longitude >= -180.0 && longitude <= 180.0;

            if (!validLatitude || !validLongitude) {
                context.disableDefaultConstraintViolation();

                if (!validLatitude) {
                    context.buildConstraintViolationWithTemplate(
                        "Latitude must be between -90 and 90 degrees")
                        .addPropertyNode(latitudeField)
                        .addConstraintViolation();
                }

                if (!validLongitude) {
                    context.buildConstraintViolationWithTemplate(
                        "Longitude must be between -180 and 180 degrees")
                        .addPropertyNode(longitudeField)
                        .addConstraintViolation();
                }

                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
