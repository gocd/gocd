/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractCRTest<T extends CRBase> {

    private boolean printExamples = false;
    protected Gson gson;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        Map<String, T> examples = new HashMap<>();
        this.addGoodExamples(examples);
        this.addBadExamples(examples);
        return examples;
    }
    /**
     * Gets collection of good example instances. Key is name of example to identify it during tests.
     */
    public Map<String,T> getGoodExamples()
    {
        Map<String, T> examples = new HashMap<>();
        this.addGoodExamples(examples);
        return examples;
    }

    /**
     * Gets collection of bad example instances. Key is name of example to identify it during tests.
     */
    public Map<String,T> getBadExamples()
    {
        Map<String, T> examples = new HashMap<>();
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
    public void shouldReturnLocation()
    {
        Map<String,T> examples = getExamples();
        for(Map.Entry<String,T> example : examples.entrySet())
        {
            String exampleName = example.getKey();
            T value = example.getValue();
            String location = value.getLocation("TEST_PARENT");
            if(printExamples) {
                System.out.print("-----\n");
                System.out.print(String.format("Example '%s' Location:\n", exampleName));
                System.out.print(location);
            }
            assertNotNull(location);
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

            T deserializedValue = (T)gson.fromJson(json,value.getClass());
            assertThat(String.format("Example %s - Deserialized value should equal to value before serialization",example.getKey()),
                    deserializedValue,is(value));
        }
    }
    @Test
    public void shouldGetErrorsWhenDeserializedFromEmptyBlock()
    {
        String json = "{}";

        Class<? extends CRBase> typeOfT = null;
        for(T example : getGoodExamples().values()) {
            typeOfT = example.getClass();
            break;
        }

        T deserializedValue = (T)gson.fromJson(json,typeOfT);

        ErrorCollection errorCollection = new ErrorCollection();
        deserializedValue.getErrors(errorCollection,"GetErrorsWhenDeserializedFromEmptyBlockTest");
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
            example.getValue().getErrors(errorCollection,"ErrorWhenBadExampleTest");
            assertThat(String.format("Example %s - invalid value should return errors",example.getKey()),
                    errorCollection.isEmpty(),is(false));
        }
    }
    @Test
    public void shouldNotErrorWhenGoodExample(){
        Map<String,T> examples = getGoodExamples();
        for(Map.Entry<String,T> example : examples.entrySet())
        {
            ErrorCollection errorCollection = new ErrorCollection();
            example.getValue().getErrors(errorCollection,"NotErrorWhenGoodExampleTest");
            assertThat(String.format("Example %s - valid value should not return errors",example.getKey()),
                    errorCollection.isEmpty(),is(true));
        }
    }
}
