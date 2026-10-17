package com.sirab.registro.web;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

final class RegistroFormErrors {

    private RegistroFormErrors() {
    }

    static BindingResult withoutRejectedValues(Object target, BindingResult original) {
        BindingResult sanitized = new BeanPropertyBindingResult(target, original.getObjectName());
        for (ObjectError error : original.getAllErrors()) {
            if (error instanceof FieldError fieldError) {
                sanitized.addError(new FieldError(
                        fieldError.getObjectName(),
                        fieldError.getField(),
                        null,
                        fieldError.isBindingFailure(),
                        fieldError.getCodes(),
                        fieldError.getArguments(),
                        fieldError.getDefaultMessage()));
            } else {
                sanitized.addError(error);
            }
        }
        return sanitized;
    }
}
