package com.thoughtworks.go.plugin.configrepo;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class CRBaseTest<T extends CRBase> {

    private boolean printExamples = true;
    protected Gson gson;

    @Before
    public void SetUp()
    {
        GsonBuilder builder = new GsonBuilder();

        if(printExamples)
            builder.setPrettyPrinting();

        GsonCodec codec = new GsonCodec(builder);

        gson = codec.getGson();
    }

    public abstract void addGoodExamples(Map<String, T> examples);
    public abstract void addBadExamples(Map<String, T> examples);

    /**
     * Gets collection of example instances. Key is name of example to identify it during tests.
     */
    public Map<String,T> getExamples()
    {
        Map<String, T> examples = new HashMap<String, T>();
        this.addGoodExamples(examples);
        this.addBadExamples(examples);
        return examples;
    }
    /**
     * Gets collection of good example instances. Key is name of example to identify it during tests.
     */
    public Map<String,T> getGoodExamples()
    {
        Map<String, T> examples = new HashMap<String, T>();
        this.addGoodExamples(examples);
        return examples;
    }

    /**
     * Gets collection of bad example instances. Key is name of example to identify it during tests.
     */
    public Map<String,T> getBadExamples()
    {
        Map<String, T> examples = new HashMap<String, T>();
        this.addBadExamples(examples);
        return examples;
    }

    @Test
    public void shouldHaveEqualsImplementedForTests()
    {
        //just try equality of each example with other
        for(Object o : getExamples().entrySet()) {
            Map.Entry<String, T> right = (Map.Entry<String, T>) o;
            for (Map.Entry<String, T> left : getExamples().entrySet()) {
                if (left.getValue() == right.getValue())
                    assertThat(String.format("example '%s' should equal to itself", left.getKey()),
                            left.getValue().equals(right.getValue()), is(true));
                else
                    assertThat(String.format("example '%s' should not equal to '%s'", left.getKey(), right.getKey()),
                            left.getValue().equals(right.getValue()), is(false));
            }
        }
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
                System.out.print("-----\n");
                System.out.print(String.format("Example '%s':\n", exampleName));
                System.out.print(json);
                System.out.print("\n");
            }
        }
    }

    @Test
    public void shouldIgnoreWhenJsonHasUnknownElements()
    {
        Map<String,T> examples = getExamples();
        for(Map.Entry<String,T> example : examples.entrySet()) {
            T value = example.getValue();
            JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
            jsonObject.add("extraProperty", new JsonPrimitive("This is not part of message type"));
            String json = gson.toJson(jsonObject);

            T deserializedValue = (T)gson.fromJson(json,value.getClass());
            assertThat(String.format("Example %s - Deserialized value should equal to value before serialization", example.getKey()),
                    deserializedValue, is(value));
        }
    }

    @Test
    public void shouldSerializeToJsonAndDeserialize()
    {
        Map<String,T> examples = getExamples();
        for(Map.Entry<String,T> example : examples.entrySet())
        {
            T value = example.getValue();
            String json = gson.toJson(value);

            Type typeOfT = new TypeToken<T>(){}.getType();

            T deserializedValue = (T)gson.fromJson(json,value.getClass());
            assertThat(String.format("Example %s - Deserialized value should equal to value before serialization",example.getKey()),
                    deserializedValue,is(value));
        }
    }
    @Test
    public void shouldThrowWhenJsonFormatIsInvalid()
    {
        Map<String,T> examples = getExamples();
        for(Map.Entry<String,T> example : examples.entrySet())
        {
            T value = example.getValue();
            String json = gson.toJson(value);

            json += "some extra non-json content";

            try {
                gson.fromJson(json, value.getClass());
            }
            catch (Exception ex)
            {
                return;
            }
            fail("Should have thrown invalid format for " + example.getKey());
        }
    }

    @Test
    public void shouldErrorWhenBadExample(){
        Map<String,T> examples = getBadExamples();
        for(Map.Entry<String,T> example : examples.entrySet())
        {
            ErrorCollection errorCollection = new ErrorCollection();
            example.getValue().getErrors(errorCollection);
            assertThat(String.format("Example %s - invalid value should return errors",example.getKey()),
                    errorCollection.isEmpty(),is(false));
        }
    }
    @Test
    public void shouldNotErrorWhenGoodExample(){

    }
}
