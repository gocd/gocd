package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;

import java.util.Collection;

public interface JsonMessageHandler {
    String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfigurationProperty> configurations);

    CRParseResult responseMessageForParseDirectory(String responseBody);
}
