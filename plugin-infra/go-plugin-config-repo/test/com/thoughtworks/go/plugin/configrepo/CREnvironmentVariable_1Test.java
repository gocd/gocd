package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CREnvironmentVariable_1Test extends CRBaseTest<CREnvironmentVariable_1> {

    private CREnvironmentVariable_1 key1;

    private CREnvironmentVariable_1 invalidNameNotSet;
    private CREnvironmentVariable_1 invalidValueNotSet;
    private CREnvironmentVariable_1 invalid2ValuesSet;

    public CREnvironmentVariable_1Test()
    {
        key1 = new CREnvironmentVariable_1("key1");
        key1.setValue("value1");

        invalidNameNotSet = new CREnvironmentVariable_1();
        invalidNameNotSet.setValue("23");

        invalidValueNotSet = new CREnvironmentVariable_1("key5");

        invalid2ValuesSet = new CREnvironmentVariable_1("keyd");
        invalid2ValuesSet.setValue("value1");
        invalid2ValuesSet.setEncryptedValue("v123445");
    }

    @Override
    public void addExamples(Map<String, CREnvironmentVariable_1> examples) {
        examples.put("key1", key1);
    }

    @Override
    public void addBadExamples(Map<String, CREnvironmentVariable_1> examples) {
        examples.put("invalidNameNotSet",invalidNameNotSet);
        examples.put("invalidValueNotSet",invalidValueNotSet);
        examples.put("invalid2ValuesSet",invalid2ValuesSet);
    }

}
