package com.thoughtworks.go.plugin.configrepo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SerializationBaseTest<T> {

    private boolean printExamples = true;
    private Gson gson;

    @Before
    public void SetUp()
    {
        GsonBuilder builder = new GsonBuilder();

        if(printExamples)
            builder.setPrettyPrinting();

        GsonCodec codec = new GsonCodec(builder);

        gson = codec.getGson();
    }

    public abstract void addExamples(Map<String, T> examples);

    /**
     * Gets collection of example instances. Key is name of example to identify it during tests.
     */
    public Map<String,T> getExamples()
    {
        Map<String, T> examples = new HashMap<String, T>();
        this.addExamples(examples);
        return examples;
    }

    @Test
    public void shouldSerializeToJson()
    {
        Map<String,T> examples = getExamples();
        for(Map.Entry<String,T> example : examples.entrySet())
        {
            String exampleName = example.getKey();
            T value = example.getValue();
            String json = gson.toJson(value);
            if(printExamples) {
                System.out.print(String.format("Example '%s':\n",exampleName));
                System.out.print(json);
            }
        }
    }
}
