package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRPluginConfigurationProperty_1Test extends CRBaseTest<CRPluginConfiguration_1> {

    private final CRPluginConfiguration_1 pluginConfig;
    private final CRPluginConfiguration_1 invalidPluginConfigNoId;
    private final CRPluginConfiguration_1 invalidPluginConfigNoVersion;

    public CRPluginConfigurationProperty_1Test()
    {
        pluginConfig = new CRPluginConfiguration_1("curl.task.plugin","1");

        invalidPluginConfigNoId = new CRPluginConfiguration_1(null,"1");
        invalidPluginConfigNoVersion = new CRPluginConfiguration_1("curl.task.plugin",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRPluginConfiguration_1> examples) {
        examples.put("pluginConfig",pluginConfig);
    }

    @Override
    public void addBadExamples(Map<String, CRPluginConfiguration_1> examples) {
        examples.put("invalidPluginConfigNoId",invalidPluginConfigNoId);
        examples.put("invalidPluginConfigNoVersion",invalidPluginConfigNoVersion);
    }
}
