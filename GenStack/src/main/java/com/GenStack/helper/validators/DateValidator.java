package com.GenStack.helper.validators;

import java.time.LocalDate;

import com.GenStack.helper.validators.ValidatorInterface;

public class DateValidator implements ValidatorInterface<LocalDate> {
    @Override
    public boolean isValid(LocalDate value) {
        return value != null && !value.isBefore(LocalDate.now()); // Example: No past dates
    }

    @Override
    public String getErrorMessage() {
        return "Invalid date: It must not be in the past.";
    }
}
