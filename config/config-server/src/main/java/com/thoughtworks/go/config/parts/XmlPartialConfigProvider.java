/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config.parts;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.WildcardScanner;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import org.jdom2.JDOMException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class XmlPartialConfigProvider implements PartialConfigProvider {
    public static final String providerName = "gocd-xml";

    private static final String defaultPattern = "**/*.gocd.xml";

    private final MagicalGoConfigXmlLoader loader;

    public XmlPartialConfigProvider(MagicalGoConfigXmlLoader loader) {
        this.loader = loader;
    }

    @Override
    public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
        File[] allFiles = getFiles(configRepoCheckoutDirectory, context);

        // if context had changed files list then we could parse only new content

        PartialConfig[] allFragments = parseFiles(allFiles);

        PartialConfig partialConfig = new PartialConfig();

        collectFragments(allFragments, partialConfig);

        return partialConfig;
    }

    @Override
    public String displayName() {
        return "GoCD XML";
    }

    public File[] getFiles(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
        String pattern = defaultPattern;

        Configuration configuration = context.configuration();
        if (configuration != null) {
            ConfigurationProperty explicitPattern = configuration.getProperty("pattern");
            if (explicitPattern != null) {
                pattern = explicitPattern.getValue();
            }
        }

        return getFiles(configRepoCheckoutDirectory, pattern);
    }

    private File[] getFiles(File configRepoCheckoutDirectory, String pattern) {
        WildcardScanner scanner = new WildcardScanner(configRepoCheckoutDirectory, pattern);

        return scanner.getFiles();
    }

    private void collectFragments(PartialConfig[] allFragments, PartialConfig partialConfig) {
        for (PartialConfig frag : allFragments) {
            for (PipelineConfigs pipesInGroup : frag.getGroups()) {
                for (PipelineConfig pipe : pipesInGroup) {
                    partialConfig.getGroups().addPipeline(pipesInGroup.getGroup(), pipe);
                }
            }
            for (EnvironmentConfig env : frag.getEnvironments()) {
                partialConfig.getEnvironments().add(env);
            }
        }
    }

    public PartialConfig[] parseFiles(File[] allFiles) {
        PartialConfig[] parts = new PartialConfig[allFiles.length];
        for (int i = 0; i < allFiles.length; i++) {
            parts[i] = parseFile(allFiles[i]);
        }
        return parts;
    }

    public PartialConfig parseFile(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return loader.fromXmlPartial(inputStream, PartialConfig.class);
        } catch (JDOMException e) {
            throw new RuntimeException("Syntax error in xml file: " + file.getName(), e);
        } catch (IOException e) {
            throw new RuntimeException("IO error when trying to parse xml file: " + file.getName(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse xml file: " + file.getName(), e);
        }
    }
}
