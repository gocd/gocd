package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPluginConfiguration;

import java.util.Collection;


public class CRScmConfig {

    private String id;
    private String name;
    private boolean autoUpdate = true;
    private CRPluginConfiguration pluginConfiguration ;
    private Collection<CRConfiguration> configuration;
}
