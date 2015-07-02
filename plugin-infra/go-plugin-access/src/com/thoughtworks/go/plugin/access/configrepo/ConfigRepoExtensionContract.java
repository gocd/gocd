package com.thoughtworks.go.plugin.access.configrepo;


import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;

/**
 * Specifies contract between server and extension point.
 */
public interface ConfigRepoExtensionContract {

    PartialConfig ParseCheckout(String pluginId, final String destinationFolder);
}
