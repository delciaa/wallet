package io.github.delciaa.wallet.transfer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** A money amount: present, from 0.01 to 10,000.00, with at most two decimals. */
@NotNull
@DecimalMin("0.01")
@DecimalMax("10000.00")
@Digits(integer = 5, fraction = 2)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAmount {

	String message() default "must be between 0.01 and 10000.00 with at most 2 decimals";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
