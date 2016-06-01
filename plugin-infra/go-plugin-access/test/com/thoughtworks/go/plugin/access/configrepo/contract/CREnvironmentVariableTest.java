package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Map;

public class CREnvironmentVariableTest extends CRBaseTest<CREnvironmentVariable> {

    private CREnvironmentVariable key1;

    private CREnvironmentVariable invalidNameNotSet;
    private CREnvironmentVariable invalidValueNotSet;
    private CREnvironmentVariable invalid2ValuesSet;

    public CREnvironmentVariableTest()
    {
        key1 = new CREnvironmentVariable("key1");
        key1.setValue("value1");

        invalidNameNotSet = new CREnvironmentVariable();
        invalidNameNotSet.setValue("23");

        invalidValueNotSet = new CREnvironmentVariable("key5");

        invalid2ValuesSet = new CREnvironmentVariable("keyd");
        invalid2ValuesSet.setValue("value1");
        invalid2ValuesSet.setEncryptedValue("v123445");
    }

    @Override
    public void addGoodExamples(Map<String, CREnvironmentVariable> examples) {
        examples.put("key1", key1);
    }

    @Override
    public void addBadExamples(Map<String, CREnvironmentVariable> examples) {
        examples.put("invalidNameNotSet",invalidNameNotSet);
        examples.put("invalidValueNotSet",invalidValueNotSet);
        examples.put("invalid2ValuesSet",invalid2ValuesSet);
    }

}
