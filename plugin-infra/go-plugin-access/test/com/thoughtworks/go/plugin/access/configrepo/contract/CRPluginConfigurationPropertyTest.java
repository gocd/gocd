package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Map;

public class CRPluginConfigurationPropertyTest extends CRBaseTest<CRPluginConfiguration> {

    private final CRPluginConfiguration pluginConfig;
    private final CRPluginConfiguration invalidPluginConfigNoId;
    private final CRPluginConfiguration invalidPluginConfigNoVersion;

    public CRPluginConfigurationPropertyTest()
    {
        pluginConfig = new CRPluginConfiguration("curl.task.plugin","1");

        invalidPluginConfigNoId = new CRPluginConfiguration(null,"1");
        invalidPluginConfigNoVersion = new CRPluginConfiguration("curl.task.plugin",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRPluginConfiguration> examples) {
        examples.put("pluginConfig",pluginConfig);
    }

    @Override
    public void addBadExamples(Map<String, CRPluginConfiguration> examples) {
        examples.put("invalidPluginConfigNoId",invalidPluginConfigNoId);
        examples.put("invalidPluginConfigNoVersion",invalidPluginConfigNoVersion);
    }
}
