package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;
import com.thoughtworks.go.plugin.configrepo.ErrorCollection;

public class InvalidPartialConfigException extends RuntimeException {
    private Object partialConfig;
    private ErrorCollection errors;

    public InvalidPartialConfigException(Object partialConfig, ErrorCollection errors) {
        super(errors.toString());
        this.partialConfig = partialConfig;
        this.errors = errors;
    }

    public Object getPartialConfig() {
        return partialConfig;
    }

    public ErrorCollection getErrors() {
        return errors;
    }
}
