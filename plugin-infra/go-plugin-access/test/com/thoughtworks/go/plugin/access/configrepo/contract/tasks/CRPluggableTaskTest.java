package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRPluggableTaskTest extends CRBaseTest<CRPluggableTask> {

    private final CRPluggableTask curl;
    private final CRPluggableTask example;
    private final CRPluggableTask invalidNoPlugin;
    private final CRPluggableTask invalidDuplicatedKeys;

    public CRPluggableTaskTest()
    {
        curl = new CRPluggableTask("curl.task.plugin","1",
                new CRConfigurationProperty("Url","http://www.google.com"),
                new CRConfigurationProperty("SecureConnection","no"),
                new CRConfigurationProperty("RequestType","no")
                );
        example = new CRPluggableTask("example.task.plugin","1");

        invalidNoPlugin = new CRPluggableTask();
        invalidDuplicatedKeys = new CRPluggableTask("curl.task.plugin","1",
                new CRConfigurationProperty("Url","http://www.google.com"),
                new CRConfigurationProperty("Url","http://www.gg.com")
        );
    }

    @Override
    public void addGoodExamples(Map<String, CRPluggableTask> examples) {
        examples.put("curl",curl);
        examples.put("example",example);
    }

    @Override
    public void addBadExamples(Map<String, CRPluggableTask> examples) {
        examples.put("invalidNoPlugin",invalidNoPlugin);
        examples.put("invalidDuplicatedKeys",invalidDuplicatedKeys);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingTask()
    {
        CRTask value = curl;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRPluggableTask.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingTask()
    {
        CRTask value = curl;
        String json = gson.toJson(value);

        CRPluggableTask deserializedValue = (CRPluggableTask)gson.fromJson(json,CRTask.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
