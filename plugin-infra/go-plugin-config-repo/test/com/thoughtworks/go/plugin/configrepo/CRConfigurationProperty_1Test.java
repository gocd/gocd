package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRConfigurationProperty_1Test extends CRBaseTest<CRConfigurationProperty_1> {

    private final CRConfigurationProperty_1 configProperty;
    private final CRConfigurationProperty_1 configPropertyEncrypted;
    private final CRConfigurationProperty_1 invalid2ValuesSet;
    private final CRConfigurationProperty_1 invalidEmpty;

    public CRConfigurationProperty_1Test()
    {
        configProperty = new CRConfigurationProperty_1("key1", "value1");
        configPropertyEncrypted = new CRConfigurationProperty_1("key1");
        configPropertyEncrypted.setKey("213476%$");

        invalid2ValuesSet = new CRConfigurationProperty_1("key1", "value1","213476%$");
        invalidEmpty = new CRConfigurationProperty_1();
    }

    @Override
    public void addGoodExamples(Map<String, CRConfigurationProperty_1> examples) {
        examples.put("configProperty",configProperty);
        examples.put("configPropertyEncrypted",configPropertyEncrypted);
    }

    @Override
    public void addBadExamples(Map<String, CRConfigurationProperty_1> examples) {
        examples.put("invalid2ValuesSet",invalid2ValuesSet);
    }
}
