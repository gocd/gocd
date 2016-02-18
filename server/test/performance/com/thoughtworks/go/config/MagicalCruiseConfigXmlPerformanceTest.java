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

package com.thoughtworks.go.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.thoughtworks.go.helper.NoOpMetricsProbeService;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

@Ignore
public class MagicalCruiseConfigXmlPerformanceTest {
    private MagicalGoConfigXmlLoader xmlLoader;
    private MagicalGoConfigXmlWriter xmlWriter;
    private static final File CONFIG_FILE = new File("../common/test-resources/unit/data/big-uat-cruise-config.xml");
    private CruiseConfig cruiseConfig;
    private static ConfigCache configCache;
    @BeforeClass
    public void beforeAll() {
        configCache = new ConfigCache();
    }

    @Before public void setup() throws Exception {
        xmlLoader = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        FileInputStream inputStream = new FileInputStream(CONFIG_FILE);
        try {
            cruiseConfig = xmlLoader.loadConfigHolder(FileUtil.readToEnd(inputStream)).config;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Test
    public void shouldLoadQuickly() throws Exception {
        for (int i = 0; i < 10; i++) {
            long total = loadConfig();
            System.out.println("load total: " + total);
        }
        assertThat(loadConfig(), lessThan(500L));
    }

    @Test
    public void shouldWriteQuickly() throws Exception {
        for (int i = 0; i < 10; i++) {
            long total = writeConfig(cruiseConfig);
            System.out.println("write total: " + total);
        }
        assertThat(writeConfig(cruiseConfig), lessThan(250L));
    }

    private long loadConfig() throws Exception {
        FileInputStream inputStream = new FileInputStream(CONFIG_FILE);
        try {
            long startTry = System.currentTimeMillis();
            xmlLoader.loadConfigHolder(FileUtil.readToEnd(inputStream));
            long endTry = System.currentTimeMillis();
            return endTry - startTry;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private long writeConfig(CruiseConfig cruiseConfig) throws Exception {
        FileOutputStream outputStream = new FileOutputStream(tempCruiseConfig());
        try {
            long startTry = System.currentTimeMillis();
            xmlWriter.write(cruiseConfig, outputStream, false);
            long endTry = System.currentTimeMillis();
            return endTry - startTry;
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private File tempCruiseConfig() throws IOException {
        File tempFile = File.createTempFile("cruise-config", "xml");
        tempFile.deleteOnExit();
        return tempFile;
    }
}
