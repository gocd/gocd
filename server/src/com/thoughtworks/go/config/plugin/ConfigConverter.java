package com.thoughtworks.go.config.plugin;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;

/**
 * Helper to transform config repo classes to config-api classes
 */
public class ConfigConverter {

    public static PartialConfig toPartialConfig(CRPartialConfig crPartialConfig) {
        throw new RuntimeException("not implemented");
    }

    //TODO #1133 convert each config element
}
