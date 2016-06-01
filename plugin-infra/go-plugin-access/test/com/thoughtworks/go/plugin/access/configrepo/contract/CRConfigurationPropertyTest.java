package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Map;

public class CRConfigurationPropertyTest extends CRBaseTest<CRConfigurationProperty> {

    private final CRConfigurationProperty configProperty;
    private final CRConfigurationProperty configPropertyEncrypted;
    private final CRConfigurationProperty invalid2ValuesSet;
    private final CRConfigurationProperty invalidEmpty;

    public CRConfigurationPropertyTest()
    {
        configProperty = new CRConfigurationProperty("key1", "value1");
        configPropertyEncrypted = new CRConfigurationProperty("key1");
        configPropertyEncrypted.setKey("213476%$");

        invalid2ValuesSet = new CRConfigurationProperty("key1", "value1","213476%$");
        invalidEmpty = new CRConfigurationProperty();
    }

    @Override
    public void addGoodExamples(Map<String, CRConfigurationProperty> examples) {
        examples.put("configProperty",configProperty);
        examples.put("configPropertyEncrypted",configPropertyEncrypted);
    }

    @Override
    public void addBadExamples(Map<String, CRConfigurationProperty> examples) {
        examples.put("invalid2ValuesSet",invalid2ValuesSet);
    }
}
