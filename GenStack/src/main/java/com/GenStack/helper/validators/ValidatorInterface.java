package com.GenStack.helper.validators;

public interface ValidatorInterface<T> {
    boolean isValid(T value);
    String getErrorMessage();
}
