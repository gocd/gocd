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
import java.util.Collections;
import java.util.List;

import org.junit.Test;

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
        List<String> repo = context.report(Collections.<String>emptyList());
        assertThat(repo.size(), is(1));
        assertThat(repo.get(0),
                is("[go] setting environment variable 'PROPERTY_NAME' to value 'property value'"));
    }

    @Test
    public void shouldReportWhenAVariableIsOverridden() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, false);
        context.setProperty(PROPERTY_NAME, NEW_VALUE, false);
        List<String> report = context.report(Collections.<String>emptyList());
        assertThat(report.size(), is(2));
        assertThat(report.get(0), is("[go] setting environment variable 'PROPERTY_NAME' to value 'property value'"));
        assertThat(report.get(1), is("[go] overriding environment variable 'PROPERTY_NAME' with value 'new value'"));
    }

    @Test
    public void shouldMaskOverRiddenSecureVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, true);
        context.setProperty(PROPERTY_NAME, NEW_VALUE, true);
        List<String> report = context.report(Collections.<String>emptyList());
        assertThat(report.size(), is(2));
        assertThat(report.get(0), is(String.format("[go] setting environment variable 'PROPERTY_NAME' to value '%s'", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE)));
        assertThat(report.get(1), is(String.format("[go] overriding environment variable 'PROPERTY_NAME' with value '%s'", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE)));
    }

    @Test
    public void shouldReportSecureVariableAsMaskedValue() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty(PROPERTY_NAME, PROPERTY_VALUE, true);
        List<String> repot = context.report(Collections.<String>emptyList());
        assertThat(repot.size(), is(1));
        assertThat(repot.get(0), is(String.format("[go] setting environment variable 'PROPERTY_NAME' to value '%s'", EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE)));
    }

    @Test
    public void testReportOverrideForProcessEnvironmentVariables() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        context.setProperty("PATH", "/foo", false);
        List<String> report = context.report(Collections.singleton("PATH"));
        assertThat(report.size(), is(1));
        assertThat(report.get(0), is("[go] overriding environment variable 'PATH' with value '/foo'"));
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


