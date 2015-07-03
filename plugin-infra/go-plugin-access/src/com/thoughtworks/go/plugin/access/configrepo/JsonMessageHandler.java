package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;

import java.util.Collection;

public interface JsonMessageHandler {
    String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfiguration> configurations);

    CRPartialConfig responseMessageForParseDirectory(String responseBody);
}
