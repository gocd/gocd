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
package com.thoughtworks.go.config;

import java.io.File;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.FileUtil;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import org.jdom2.Element;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class GoConfigFieldTest {
    public SystemEnvironment systemEnvironment;
    private ConfigCache configCache = new ConfigCache();


    @Test public void shouldConvertFromXmlToJavaObjectCorrectly() throws Exception {
        final Foo object = new Foo();
        final GoConfigFieldWriter field = new GoConfigFieldWriter(Foo.class.getDeclaredField("number"), object, configCache, null);
        final Element element = new Element("foo");
        element.setAttribute("number", "100");
        field.setValueIfNotNull(element, object);
        assertThat(object.number, is(100L));
    }

    @Test public void shouldConvertFileCorrectly() throws Exception {
        final Foo object = new Foo();
        final GoConfigFieldWriter field = new GoConfigFieldWriter(Foo.class.getDeclaredField("directory"), object, configCache, null);
        final Element element = new Element("foo");
        element.setAttribute("directory", "foo" + FileUtil.fileseparator() + "dir");
        field.setValueIfNotNull(element, object);
        assertThat(object.directory.getPath(), is("foo" + FileUtil.fileseparator() + "dir"));
    }

    @Test public void shouldSetFileToNullifValueIsNotSpecified() throws Exception {
        final Foo object = new Foo();
        final GoConfigFieldWriter field = new GoConfigFieldWriter(Foo.class.getDeclaredField("directory"), object, configCache, null);
        final Element element = new Element("foo");
        field.setValueIfNotNull(element, object);
        assertThat(object.directory, is(nullValue()));
    }

    @Test(expected = RuntimeException.class) public void shouldValidateAndConvertOnlyIfAppropriate()
            throws NoSuchFieldException {
        final Foo object = new Foo();
        final GoConfigFieldWriter field = new GoConfigFieldWriter(Foo.class.getDeclaredField("number"), object, configCache, null);
        final Element element = new Element("foo");
        element.setAttribute("number", "anything");
        field.setValueIfNotNull(element, object);
    }

    @Before
    public void setUp() throws Exception {
        systemEnvironment = new SystemEnvironment();
    }

    private class Foo {
        @ConfigAttribute("number")
        private Long number;

        @ConfigAttribute(value = "directory", allowNull = true)
        private File directory;
    }
}
