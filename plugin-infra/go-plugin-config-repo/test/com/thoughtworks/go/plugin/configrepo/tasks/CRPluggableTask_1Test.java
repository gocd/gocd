package com.thoughtworks.go.plugin.configrepo.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import com.thoughtworks.go.plugin.configrepo.CRConfigurationProperty_1;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRPluggableTask_1Test extends CRBaseTest<CRPluggableTask_1> {

    private final CRPluggableTask_1 curl;
    private final CRPluggableTask_1 example;
    private final CRPluggableTask_1 invalidNoPlugin;
    private final CRPluggableTask_1 invalidDuplicatedKeys;

    public CRPluggableTask_1Test()
    {
        curl = new CRPluggableTask_1("curl.task.plugin","1",
                new CRConfigurationProperty_1("Url","http://www.google.com"),
                new CRConfigurationProperty_1("SecureConnection","no"),
                new CRConfigurationProperty_1("RequestType","no")
                );
        example = new CRPluggableTask_1("example.task.plugin","1");

        invalidNoPlugin = new CRPluggableTask_1();
        invalidDuplicatedKeys = new CRPluggableTask_1("curl.task.plugin","1",
                new CRConfigurationProperty_1("Url","http://www.google.com"),
                new CRConfigurationProperty_1("Url","http://www.gg.com")
        );
    }

    @Override
    public void addGoodExamples(Map<String, CRPluggableTask_1> examples) {
        examples.put("curl",curl);
        examples.put("example",example);
    }

    @Override
    public void addBadExamples(Map<String, CRPluggableTask_1> examples) {
        examples.put("invalidNoPlugin",invalidNoPlugin);
        examples.put("invalidDuplicatedKeys",invalidDuplicatedKeys);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingTask()
    {
        CRTask_1 value = curl;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRPluggableTask_1.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingTask()
    {
        CRTask_1 value = curl;
        String json = gson.toJson(value);

        CRPluggableTask_1 deserializedValue = (CRPluggableTask_1)gson.fromJson(json,CRTask_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
