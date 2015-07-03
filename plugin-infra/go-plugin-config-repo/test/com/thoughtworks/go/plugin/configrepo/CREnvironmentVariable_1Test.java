package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CREnvironmentVariable_1Test extends SerializationBaseTest<CREnvironmentVariable_1> {

    private CREnvironmentVariable_1 key1;

    public CREnvironmentVariable_1Test()
    {
        key1 = new CREnvironmentVariable_1("key1");
        key1.setValue("value1");
    }

    @Override
    public void addExamples(Map<String, CREnvironmentVariable_1> examples) {
        examples.put("key1", key1);
    }

}
