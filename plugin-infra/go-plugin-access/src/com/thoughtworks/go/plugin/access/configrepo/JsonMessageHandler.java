package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;

import java.util.Collection;

public interface JsonMessageHandler {
    String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfigurationProperty> configurations);

    CRPartialConfig responseMessageForParseDirectory(String responseBody);
}
