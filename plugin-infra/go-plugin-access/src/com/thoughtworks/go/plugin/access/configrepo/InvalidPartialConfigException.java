package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRError;
import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;
import com.thoughtworks.go.plugin.configrepo.ErrorCollection;

import java.util.List;

public class InvalidPartialConfigException extends RuntimeException {
    private Object partialConfig;
    private List<CRError> errors;

    public InvalidPartialConfigException(Object partialConfig, List<CRError> errors) {
        super(errors.toString());
        this.partialConfig = partialConfig;
        this.errors = errors;
    }

    public Object getPartialConfig() {
        return partialConfig;
    }

    public List<CRError> getErrors() {
        return errors;
    }
}
