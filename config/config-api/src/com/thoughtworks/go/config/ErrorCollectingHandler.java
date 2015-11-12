package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;

import java.util.List;

public abstract class ErrorCollectingHandler implements GoConfigGraphWalker.Handler {
    private final List<ConfigErrors> allErrors;

    public ErrorCollectingHandler(List<ConfigErrors> allErrors) {
        this.allErrors = allErrors;
    }

    public void handle(Validatable validatable, ValidationContext context) {
        handleValidation(validatable, context);
        ConfigErrors configErrors = validatable.errors();

        if (!configErrors.isEmpty()) {
            allErrors.add(configErrors);
        }
    }

    public abstract void handleValidation(Validatable validatable, ValidationContext context);
}