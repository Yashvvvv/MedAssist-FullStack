package com.medassist.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CoordinateValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCoordinates {
    String message() default "Invalid coordinates provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String latitudeField() default "latitude";
    String longitudeField() default "longitude";
}
