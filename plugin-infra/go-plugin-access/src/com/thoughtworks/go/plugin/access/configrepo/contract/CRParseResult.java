package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CRParseResult {
    private final CRPartialConfig partialConfig;
    private final List<CRError> errors;

    public CRParseResult(CRPartialConfig partialConfig, List<CRError> errors) {
        this.partialConfig = partialConfig;
        this.errors = errors;
    }
    public CRParseResult(CRPartialConfig partialConfig, String... errors) {
        this.partialConfig = partialConfig;
        this.errors = new ArrayList<CRError>();
        for(String message : errors)
        {
            this.errors.add(new CRError(message,null));
        }
    }

    public CRPartialConfig getPartialConfig() {
        return partialConfig;
    }

    public List<CRError> getErrors() {
        return errors;
    }

}
