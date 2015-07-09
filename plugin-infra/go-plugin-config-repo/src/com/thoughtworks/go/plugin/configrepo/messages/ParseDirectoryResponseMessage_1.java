package com.thoughtworks.go.plugin.configrepo.messages;

import com.thoughtworks.go.plugin.configrepo.CRError_1;
import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;

import java.util.ArrayList;
import java.util.List;

public class ParseDirectoryResponseMessage_1 {
    private CRPartialConfig_1 partialConfig;
    private List<CRError_1> pluginErrors = new ArrayList<>();

    public boolean hasErrors() {
        return pluginErrors != null && !pluginErrors.isEmpty();
    }

    public List<CRError_1> getErrors() {
        return pluginErrors;
    }

    public CRPartialConfig_1 getConfig() {
        return partialConfig;
    }

    public void setPartialConfig(CRPartialConfig_1 partialConfig) {
        this.partialConfig = partialConfig;
    }
}
