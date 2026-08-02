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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.TimeProvider;
import org.jdom2.JDOMException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigMigrator {

    /**
     * Schema-migrating config content is expensive - common test fixtures are ~120 schema versions old, and are
     * repeatedly migrated with identical input across tests - so cache upgrade output per unique input. Since some
     * migrations generate random ids, this also keeps the migrated form of a given fixture stable within a run.
     * (Tests of migration behaviour itself use GoConfigMigration/GoConfigMigrator directly)
     */
    private static final LoadingCache<String, String> migratedXmlCache = Caffeine.newBuilder()
        .maximumSize(32)
        .build(xml -> new GoConfigMigration(new TimeProvider()).upgradeIfNecessary(xml));

    public static void migrate(final Path configFile) {
        try {
            Files.writeString(configFile, migrate(Files.readString(configFile, UTF_8)), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String migrate(String configXml) {
        return migratedXmlCache.get(configXml);
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
            MagicalGoConfigXmlLoader xmlLoader = new MagicalGoConfigXmlLoader(registry);
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
            return new MagicalGoConfigXmlLoader(ConfigElementImplementationRegistryMother.withNoPlugins())
                .loadConfigHolder(migrate(content)).config;
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        }
    }
}
