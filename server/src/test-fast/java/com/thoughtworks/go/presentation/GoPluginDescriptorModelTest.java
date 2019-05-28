/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.presentation;

import java.io.File;
import java.util.Arrays;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class GoPluginDescriptorModelTest {

    @Test
    public void shouldFillDefaultValuesWhenPluginXmlIsNotPresent() throws Exception {
        GoPluginDescriptor descriptor = GoPluginDescriptor.usingId("plugin.jar", "some_path", new File("bundle_location"), true);
        GoPluginDescriptor descriptorModel = GoPluginDescriptorModel.convertToDescriptorWithAllValues(descriptor);

        assertThat(descriptor.about(), is(nullValue()));

        assertThat(descriptorModel.about(), is(notNullValue()));
        assertThat(descriptorModel.about().description(), is("No description available."));
        assertThat(descriptorModel.about().version().isEmpty(), is(true));
        assertThat(descriptorModel.about().vendor(), is(notNullValue()));
        assertThat(descriptorModel.about().vendor().name(), is("Unknown"));
        assertThat(descriptorModel.about().vendor().url(), is(nullValue()));
        assertThat(descriptorModel.about().name(), is("plugin.jar"));
        assertThat(descriptorModel.about().targetGoVersion(), is("Unknown"));
        assertThat(descriptorModel.about().targetOperatingSystems().contains("No restrictions"), is(true));
    }

    @Test
    public void shouldFillDefaultValuesForEmptyFieldsWhenPluginXmlIsPresentWithSomeValues() throws Exception {
        GoPluginDescriptor.Vendor vendor = new GoPluginDescriptor.Vendor(null, "http://ali.com");
        GoPluginDescriptor.About about = new GoPluginDescriptor.About(null, null, "13.3.0","some description", vendor, Arrays.asList("Linux", "Windows"));
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin.jar", "1", about, "some_path", new File("bundle_location"), false);
        GoPluginDescriptor descriptorModel = GoPluginDescriptorModel.convertToDescriptorWithAllValues(descriptor);

        assertThat(descriptor.version(), is("1"));
        assertThat(descriptor.about(), is(notNullValue()));
        assertThat(descriptor.about().description(), is("some description"));
        assertThat(descriptor.about().vendor(), is(notNullValue()));
        assertThat(descriptor.about().vendor().name(), is(nullValue()));
        assertThat(descriptor.about().vendor().url(), is("http://ali.com"));
        assertThat(descriptor.about().name(), is(nullValue()));
        assertThat(descriptor.about().targetGoVersion(), is("13.3.0"));
        assertThat(descriptor.about().targetOperatingSystems().contains("Linux"), is(true));
        assertThat(descriptor.about().targetOperatingSystems().contains("Windows"), is(true));
        assertThat(descriptor.about().targetOperatingSystems().contains("Osx"), is(false));

        assertThat(descriptorModel.about(), is(notNullValue()));
        assertThat(descriptorModel.about().description(), is("some description"));
        assertThat(descriptorModel.about().version().isEmpty(), is(true));
        assertThat(descriptorModel.about().vendor(), is(notNullValue()));
        assertThat(descriptorModel.about().vendor().name(), is("Unknown"));
        assertThat(descriptorModel.about().vendor().url(), is("http://ali.com"));
        assertThat(descriptorModel.about().name(), is("plugin.jar"));
        assertThat(descriptorModel.about().targetGoVersion(), is("13.3.0"));
        assertThat(descriptorModel.about().targetOperatingSystems().contains("Linux"), is(true));
        assertThat(descriptor.about().targetOperatingSystems().contains("Windows"), is(true));
        assertThat(descriptor.about().targetOperatingSystems().contains("Osx"), is(false));
    }

    @Test
    public void shouldAddHttpToUrlIfNotPresent() throws Exception {
        GoPluginDescriptor.Vendor vendor = new GoPluginDescriptor.Vendor(null, "ali.com");
        GoPluginDescriptor.About about = new GoPluginDescriptor.About(null, null, "13.3.0","some description", vendor, Arrays.asList("Linux", "Windows"));
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin.jar", "1", about, "some_path", new File("bundle_location"), false);
        GoPluginDescriptor descriptorModel = GoPluginDescriptorModel.convertToDescriptorWithAllValues(descriptor);

        assertThat(descriptorModel.about().vendor().url(), is("http://ali.com"));
    }

    @Test
    public void shouldNotAddHttpToUrlIfProtocolIsAlreadyPresent() throws Exception {
        GoPluginDescriptor.Vendor vendor = new GoPluginDescriptor.Vendor(null, "http://ali.com");
        GoPluginDescriptor.About about = new GoPluginDescriptor.About(null, null, "13.3.0","some description", vendor, Arrays.asList("Linux", "Windows"));
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin.jar", "1", about, "some_path", new File("bundle_location"), false);
        GoPluginDescriptor descriptorModel = GoPluginDescriptorModel.convertToDescriptorWithAllValues(descriptor);

        assertThat(descriptorModel.about().vendor().url(), is("http://ali.com"));
    }

    @Test
    public void shouldNotAddHttpToUrlIfSecureProtocolIsAlreadyPresent() throws Exception {
        GoPluginDescriptor.Vendor vendor = new GoPluginDescriptor.Vendor(null, "https://ali.com");
        GoPluginDescriptor.About about = new GoPluginDescriptor.About(null, null, "13.3.0","some description", vendor, Arrays.asList("Linux", "Windows"));
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin.jar", "1", about, "some_path", new File("bundle_location"), false);
        GoPluginDescriptor descriptorModel = GoPluginDescriptorModel.convertToDescriptorWithAllValues(descriptor);

        assertThat(descriptorModel.about().vendor().url(), is("https://ali.com"));
    }

    @Test
    public void shouldNotAddHttpToUrlIfUrlIsNull() throws Exception {
        GoPluginDescriptor.Vendor vendor = new GoPluginDescriptor.Vendor(null, null);
        GoPluginDescriptor.About about = new GoPluginDescriptor.About(null, null, "13.3.0","some description", vendor, Arrays.asList("Linux", "Windows"));
        GoPluginDescriptor descriptor = new GoPluginDescriptor("plugin.jar", "1", about, "some_path", new File("bundle_location"), false);
        GoPluginDescriptor descriptorModel = GoPluginDescriptorModel.convertToDescriptorWithAllValues(descriptor);

        assertThat(descriptorModel.about().vendor().url(), is(nullValue()));
    }
}
