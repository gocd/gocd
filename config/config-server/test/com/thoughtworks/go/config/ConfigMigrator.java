/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.TimeProvider;

import java.io.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.mockito.Mockito.mock;

public class ConfigMigrator {
    public static GoConfigMigration migrate(final File configFile) {
        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

        GoConfigMigration upgrader = new GoConfigMigration(new GoConfigMigration.UpgradeFailedHandler() {
            public void handle(Exception e) {
                String content = "";
                try {
                    content = FileUtil.readContentFromFile(configFile);
                } catch (IOException e1) {
                }
                throw bomb(e.getMessage() + ": content=\n" + content + "\n" + (e.getCause() == null ? "" : e.getCause().getMessage()), e);
            }
        }, mock(ConfigRepository.class), new TimeProvider(), new ConfigCache(), registry
        );
        //TODO: LYH & GL GoConfigMigration should be able to handle stream instead of binding to file
        upgrader.upgradeIfNecessary(configFile, "N/A");
        return upgrader;
    }



    public static String migrate(String configXml) throws IOException {
        File tempFile = TestFileUtil.createTempFile("cruise-config.xml");
        FileUtil.writeContentToFile(configXml, tempFile);
        migrate(tempFile);
        String newConfigXml = FileUtil.readContentFromFile(tempFile);
        tempFile.delete();
        return newConfigXml;
    }

    public static GoConfigHolder loadWithMigration(String xml) {
        GoConfigHolder configHolder;
        try {
            configHolder = loadWithMigration(new ByteArrayInputStream(xml.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return configHolder;
    }

    public static GoConfigHolder loadWithMigration(InputStream input) throws Exception {
        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

        return loadWithMigration(input, registry);
    }

    public static GoConfigHolder loadWithMigration(InputStream input, final ConfigElementImplementationRegistry registry) throws Exception {
        MagicalGoConfigXmlLoader xmlLoader = new MagicalGoConfigXmlLoader(new ConfigCache(), registry);
        File tempFile = TestFileUtil.createTempFile("cruise-config.xml");
        FileUtil.writeContentToFile(FileUtil.readToEnd(input), tempFile);
        migrate(tempFile);
        final FileInputStream inputStream = new FileInputStream(tempFile);
        return xmlLoader.loadConfigHolder(FileUtil.readToEnd(inputStream));
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
