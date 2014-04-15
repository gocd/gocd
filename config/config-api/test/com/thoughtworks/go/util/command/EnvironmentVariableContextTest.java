/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EnvironmentVariableContextTest {
    private static final String PROPERTY_NAME = "PROPERTY_NAME";
    private static final String PROPERTY_VALUE = "property value";
    private static final String NEW_VALUE = "new value";

    @Test
    public void shouldBeAbleToAddProperties() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);
        assertThat(context.getProperty(PROPERTY_NAME), is(PROPERTY_VALUE));
    }

    @Test
    public void shouldReportLastAddedAsPropertyValue() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);
        context.setProperty(PROPERTY_NAME, NEW_VALUE, false);
        assertThat(context.getProperty(PROPERTY_NAME), is(NEW_VALUE));
    }

    @Test
    public void shouldReportWhenAVariableIsSet() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);
        InMemoryStreamConsumer consumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        Map<String, String> map = new HashMap<String, String>();
        context.setupRuntimeEnvironment(map, consumer);

        assertThat(map.get(PROPERTY_NAME), is(PROPERTY_VALUE));
        assertThat(consumer.getAllOutput(),
                containsString("[go] setting environment variable 'PROPERTY_NAME' to value 'property value'"));
    }

    @Test
    public void shouldReportWhenAVariableIsOverridden() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);
        context.setProperty(PROPERTY_NAME, NEW_VALUE, false);
        InMemoryStreamConsumer consumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        Map<String, String> map = new HashMap<String, String>();        
        context.setupRuntimeEnvironment(map, consumer);

        assertThat(map.get(PROPERTY_NAME), is(NEW_VALUE));
        assertThat(consumer.getAllOutput(), containsString("[go] setting environment variable 'PROPERTY_NAME' to value 'property value'"));
        assertThat(consumer.getAllOutput(), containsString("[go] overriding environment variable 'PROPERTY_NAME' with value 'new value'"));
    }

    @Test
    public void shouldMaskOverRiddenSecureVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, true);
        context.setProperty(PROPERTY_NAME, NEW_VALUE, true);
        InMemoryStreamConsumer consumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        Map<String, String> map = new HashMap<String, String>();
        context.setupRuntimeEnvironment(map, consumer);

        assertThat(map.get(PROPERTY_NAME), is(NEW_VALUE));
        assertThat(consumer.getAllOutput(), containsString(String.format("[go] setting environment variable 'PROPERTY_NAME' to value '%s'", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE)));
        assertThat(consumer.getAllOutput(), containsString(String.format("[go] overriding environment variable 'PROPERTY_NAME' with value '%s'", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE)));
    }

    @Test
    public void shouldReportSecureVariableAsMaskedValue() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, true);
        InMemoryStreamConsumer consumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        Map<String, String> map = new HashMap<String, String>();
        context.setupRuntimeEnvironment(map, consumer);

        assertThat(map.get(PROPERTY_NAME), is(PROPERTY_VALUE));
        assertThat(consumer.getAllOutput(), containsString(String.format("[go] setting environment variable 'PROPERTY_NAME' to value '%s'", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE)));
    }

    @Test
    public void shouldBeAbleToSerialize() throws ClassNotFoundException, IOException {
        EnvironmentVariableContext original = new EnvironmentVariableContext("blahKey", "blahValue");
        EnvironmentVariableContext clone = (EnvironmentVariableContext) SerializationTester.serializeAndDeserialize(original);
        assertThat(clone,is(original));
    }

    @Test
    public void shouldSaveSecureStateAboutAEnvironmentVariable() {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);

        assertThat(environmentVariableContext.getProperty(PROPERTY_NAME), is(PROPERTY_VALUE));
        assertThat(environmentVariableContext.getPropertyForDisplay(PROPERTY_NAME), is(PROPERTY_VALUE));
        assertThat(environmentVariableContext.isPropertySecure(PROPERTY_NAME), is(false));
    }

    @Test
    public void shouldSaveSecureStateForASecureEnvironmentVariable() {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty(PROPERTY_NAME, PROPERTY_VALUE, true);

        assertThat(environmentVariableContext.getProperty(PROPERTY_NAME), is(PROPERTY_VALUE));
        assertThat(environmentVariableContext.getPropertyForDisplay(PROPERTY_NAME), is(EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE));
        assertThat(environmentVariableContext.isPropertySecure(PROPERTY_NAME), is(true));
    }

    @Test
    public void shouldGetSecureEnvironmentVariables() {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        environmentVariableContext.setProperty("secure_foo", "secure_foo_value", true);
        environmentVariableContext.setProperty("plain_foo", "plain_foo_value", false);
        List<EnvironmentVariableContext.EnvironmentVariable> secureEnvironmentVariables = environmentVariableContext.getSecureEnvironmentVariables();
        assertThat(secureEnvironmentVariables.size(), is(1));
        assertThat(secureEnvironmentVariables, hasItem(new EnvironmentVariableContext.EnvironmentVariable("secure_foo", "secure_foo_value", true)));
    }

}


