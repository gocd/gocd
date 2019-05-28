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

import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

public class GoPluginDescriptorModel {
    public static GoPluginDescriptor convertToDescriptorWithAllValues(GoPluginDescriptor pluginDescriptor) {
        GoPluginDescriptor.About about = aboutOrDefault(pluginDescriptor.about());
        GoPluginDescriptor.Vendor vendor = vendorOrDefault(about.vendor());

        vendor = new GoPluginDescriptor.Vendor(valueOrDefault(vendor.name(), "Unknown"), verifyProtocol(vendor.url()));

        about = new GoPluginDescriptor.About(valueOrDefault(about.name(), pluginDescriptor.id()), valueOrDefault(about.version(), ""), valueOrDefault(about.targetGoVersion(), "Unknown"),
                valueOrDefault(about.description(), "No description available."), vendor, valueOrDefault(about.targetOperatingSystems(), Arrays.asList("No restrictions")));

        GoPluginDescriptor descriptorWithAllValues = new GoPluginDescriptor(pluginDescriptor.id(), valueOrDefault(pluginDescriptor.version(), "1"), about,
                pluginDescriptor.pluginFileLocation(), pluginDescriptor.bundleLocation(), pluginDescriptor.isBundledPlugin());
        descriptorWithAllValues.setStatus(pluginDescriptor.getStatus());

        return descriptorWithAllValues;
    }

    private static String verifyProtocol(String url) {
        if (url != null && !(url.startsWith("http://") || url.startsWith("https://"))) {
            url = "http://".concat(url);
        }
        return url;
    }

    private static List<String> valueOrDefault(List<String> list, List<String> defaultValue) {
        return list.isEmpty() ? defaultValue : list;
    }

    private static GoPluginDescriptor.Vendor vendorOrDefault(GoPluginDescriptor.Vendor vendor) {
        GoPluginDescriptor.Vendor defaultVendor = new GoPluginDescriptor.Vendor("Unknown", null);
        return vendor == null ? defaultVendor : vendor;
    }

    private static GoPluginDescriptor.About aboutOrDefault(GoPluginDescriptor.About about) {
        GoPluginDescriptor.About defaultAbout = new GoPluginDescriptor.About(null, null, null, null, vendorOrDefault(null), null);
        return about == null ? defaultAbout : about;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
