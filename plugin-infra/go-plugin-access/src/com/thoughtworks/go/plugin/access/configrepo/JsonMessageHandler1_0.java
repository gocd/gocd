package com.thoughtworks.go.plugin.access.configrepo;


import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;

import java.util.Collection;

public class JsonMessageHandler1_0 implements JsonMessageHandler {
    @Override
    public String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfiguration> configurations) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public CRPartialConfig responseMessageForParseDirectory(String responseBody) {
        throw new RuntimeException("not implemented");
    }
}
