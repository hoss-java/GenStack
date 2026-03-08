package com.GenStack.helper.validators;

import java.time.Duration;

import com.GenStack.helper.validators.ValidatorInterface;

public class DurationValidator implements ValidatorInterface<Duration> {
    @Override
    public boolean isValid(Duration value) {
        return value != null && !value.isNegative(); // Example: No negative durations
    }

    @Override
    public String getErrorMessage() {
        return "Invalid duration: It must not be negative.";
    }
}
