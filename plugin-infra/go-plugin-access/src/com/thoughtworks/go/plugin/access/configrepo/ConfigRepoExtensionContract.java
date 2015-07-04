package com.thoughtworks.go.plugin.access.configrepo;


import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;

import java.util.Collection;

/**
 * Specifies contract between server and extension point.
 */
public interface ConfigRepoExtensionContract {

    CRPartialConfig parseDirectory(String pluginId, final String destinationFolder, final Collection<CRConfiguration> configurations);
}
