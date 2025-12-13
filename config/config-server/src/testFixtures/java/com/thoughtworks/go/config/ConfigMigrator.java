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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.TimeProvider;
import org.jdom2.JDOMException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigMigrator {
    public static void migrate(final Path configFile) {
        try {
            String content = Files.readString(configFile, UTF_8);

            GoConfigMigration upgrader = new GoConfigMigration(new TimeProvider());
            //TODO: LYH & GL GoConfigMigration should be able to handle stream instead of binding to file
            String upgradedContent = upgrader.upgradeIfNecessary(content);

            Files.writeString(configFile, upgradedContent, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String migrate(String configXml) throws IOException {
        Path tempFile = Files.createTempFile("cruise-config", ".xml");
        Files.writeString(tempFile, configXml, UTF_8);
        migrate(tempFile);
        String newConfigXml = Files.readString(tempFile, UTF_8);
        Files.delete(tempFile);
        return newConfigXml;
    }

    public static String migrate(String content, int fromVersion, int toVersion) {
        GoConfigMigration upgrader = new GoConfigMigration(new TimeProvider());
        return upgrader.upgrade(content, fromVersion, toVersion);
    }

    public static GoConfigHolder loadWithMigration(String xml) {
        GoConfigHolder configHolder;
        try {
            configHolder = loadWithMigration(new ByteArrayInputStream(xml.getBytes()));
        } catch (IOException | JDOMException e) {
            throw new RuntimeException(e);
        }
        return configHolder;
    }

    public static GoConfigHolder loadWithMigration(InputStream input) throws IOException, JDOMException {
        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        return loadWithMigration(input, registry);
    }

    public static GoConfigHolder loadWithMigration(InputStream input, final ConfigElementImplementationRegistry registry) throws IOException, JDOMException {
        Path tempFile = Files.createTempFile("cruise-config", ".xml");
        try {
            MagicalGoConfigXmlLoader xmlLoader = new MagicalGoConfigXmlLoader(new ConfigCache(), registry);
            try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                input.transferTo(out);
            }
            migrate(tempFile);
            return xmlLoader.loadConfigHolder(Files.readString(tempFile, UTF_8));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public static CruiseConfig load(String content) {
        try {
            ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

            return new MagicalGoConfigXmlLoader(new ConfigCache(), registry).loadConfigHolder(migrate(content)).config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
