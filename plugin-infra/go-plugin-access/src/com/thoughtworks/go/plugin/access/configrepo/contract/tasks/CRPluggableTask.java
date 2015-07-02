package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;

import java.util.Collection;

public class CRPluggableTask extends CRTask {
    private CRPluginConfiguration pluginConfiguration ;
    private Collection<CRConfiguration> configuration ;
}
