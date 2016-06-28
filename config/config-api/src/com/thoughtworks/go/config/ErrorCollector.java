package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;

import java.util.ArrayList;
import java.util.List;

public class ErrorCollector {
    public static List<ConfigErrors> getAllErrors(Validatable v) {
        final List<ConfigErrors> allErrors = new ArrayList<>();
        new GoConfigGraphWalker(v).walk(new ErrorCollectingHandler(allErrors) {
            @Override
            public void handleValidation(Validatable validatable, ValidationContext context) {
            }
        });
        return allErrors;
    }
}
