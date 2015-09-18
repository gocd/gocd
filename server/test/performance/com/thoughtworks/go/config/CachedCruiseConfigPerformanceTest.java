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

import java.io.InputStream;

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.hamcrest.core.Is;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertThat;

@Ignore("This is used for benchmarking cruise-config performance. It is not run automatically")
public class CachedCruiseConfigPerformanceTest {
    private static MergedGoConfig cache;

    @BeforeClass
    public static void setup() throws Exception {
        InputStream stream = CachedCruiseConfigPerformanceTest.class.getResourceAsStream("some-big-cruise-config.xml");
        String config = FileUtil.readToEnd(stream);
        SystemEnvironment env = new SystemEnvironment();
        ConfigRepository repo = new ConfigRepository(env);
        ConfigCache configCache = new ConfigCache();
        ServerVersion serverVersion = new ServerVersion();
        ConfigElementImplementationRegistry register = ConfigElementImplementationRegistryMother.withNoPlugins();
        cache = new MergedGoConfig(new GoFileConfigDataSource(new GoConfigMigration(repo, new TimeProvider(), configCache, register,
                null), repo, env, new TimeProvider(), configCache,
                serverVersion, register, null, new ServerHealthService()), new ServerHealthService());
        cache.save(config, true);
        assertThat(cache.currentConfig().getAllPipelineConfigs().size(), Is.is(164));
    }

    @Test
    public void addAnAgent() throws Exception {
        //warm up
        int count = 1;
        for (int i=0; i<100; i++) {
            assertCommandPerformance(addAgent(count++), 1);
        }
        assertCommandPerformance(addAgent(count++), 1);
    }

    private UpdateConfigCommand addAgent(int count) {
        return GoConfigDao.createAddAgentCommand(new AgentConfig("UUID" + count, "host-" + count, "127.0.0." + count));
    }

    private void assertCommandPerformance(UpdateConfigCommand command, int seconds) {
        long start = System.currentTimeMillis();
        cache.writeWithLock(command);
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        System.out.println("Took: " + elapsed);
//        assertThat(elapsed, lessThan(seconds * 1000L));
    }

}
