package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRError;

import java.util.List;

public class InvalidPartialConfigException extends RuntimeException {
    private Object partialConfig;
    private String errors;

    public InvalidPartialConfigException(Object partialConfig, String errors) {
        super(errors);
        this.partialConfig = partialConfig;
        this.errors = errors;
    }

    public Object getPartialConfig() {
        return partialConfig;
    }

    public String getErrors() {
        return errors;
    }
}
