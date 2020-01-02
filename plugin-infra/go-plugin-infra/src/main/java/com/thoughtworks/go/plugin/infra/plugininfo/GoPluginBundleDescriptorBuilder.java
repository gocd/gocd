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
package com.thoughtworks.go.plugin.infra.plugininfo;

import com.thoughtworks.go.plugin.infra.monitor.BundleOrPluginFileDetails;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

@Slf4j
@Component
@NoArgsConstructor
public class GoPluginBundleDescriptorBuilder {

    public GoPluginBundleDescriptor build(BundleOrPluginFileDetails bundleOrPluginJarFile) {
        if (!bundleOrPluginJarFile.exists()) {
            throw new RuntimeException(format("Plugin or bundle jar does not exist: %s", bundleOrPluginJarFile.file()));
        }

        String defaultId = bundleOrPluginJarFile.file().getName();
        GoPluginBundleDescriptor goPluginBundleDescriptor = new GoPluginBundleDescriptor(GoPluginDescriptor.builder()
                .id(defaultId)
                .bundleLocation(bundleOrPluginJarFile.extractionLocation())
                .pluginJarFileLocation(bundleOrPluginJarFile.file().getAbsolutePath())
                .isBundledPlugin(bundleOrPluginJarFile.isBundledPlugin())
                .build());

        try {
            if (bundleOrPluginJarFile.isBundleJar()) {
                return GoPluginBundleDescriptorParser.parseXML(bundleOrPluginJarFile.getBundleXml(), bundleOrPluginJarFile);
            }

            if (bundleOrPluginJarFile.isPluginJar()) {
                return GoPluginDescriptorParser.parseXML(bundleOrPluginJarFile.getPluginXml(), bundleOrPluginJarFile);
            }

            goPluginBundleDescriptor.markAsInvalid(List.of(format("Plugin with ID (%s) is not valid. The plugin does not seem to contain plugin.xml or gocd-bundle.xml", defaultId)), new RuntimeException("The plugin does not seem to contain plugin.xml or gocd-bundle.xml"));
        } catch (Exception e) {
            log.warn("Unable to load the jar file {}", bundleOrPluginJarFile.file(), e);
            String cause = e.getCause() != null ? format("%s. Cause: %s", e.getMessage(), e.getCause().getMessage()) : e.getMessage();
            goPluginBundleDescriptor.markAsInvalid(singletonList(format("Plugin with ID (%s) is not valid: %s", defaultId, cause)), e);
        }

        return goPluginBundleDescriptor;
    }

}
