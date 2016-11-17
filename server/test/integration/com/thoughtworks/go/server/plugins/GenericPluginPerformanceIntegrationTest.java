/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.monitor.PluginFileDetails;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarChangeListener;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GenericPluginPerformanceIntegrationTest  {
    @Autowired private DefaultPluginManager defaultPluginManager;
    @Autowired private PluginJarChangeListener pluginJarChangeListener;
    @Autowired private MaterialService materialService;

    @Ignore
    @Test
    public void testPluginCallsInParallel() throws Exception {
        String pathToPluginJar = "/home/godev/Downloads/yum-repo-exec-poller.jar";

        // Example: To hit 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3
        // Total number of requests: 15, concurrencyLevel = anything, numberOfDifferentIndexes = 3
        int totalNumberOfRequests = 100;
        int concurrencyLevel = 10;
        int numberOfDifferentIndexes = 20;

        defaultPluginManager.startInfrastructure(true);
        pluginJarChangeListener.pluginJarAdded(new PluginFileDetails(new File(pathToPluginJar), true));

        List<Callable<Object>> operations = new ArrayList<Callable<Object>>();
        for (int i = 0; i < totalNumberOfRequests; i++) {
            operations.add(operation(i % numberOfDifferentIndexes));
        }

        runInParallel(concurrencyLevel, operations);
    }

    private void runInParallel(int concurrencyLevel, List<Callable<Object>> operations) throws InterruptedException {
        long start = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(concurrencyLevel);
        List<Future<Object>> futures = executorService.invokeAll(operations);

        for (Future<Object> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();
        System.err.println((end - start) / 1000.0);
    }

    private Callable<Object> operation(final int index) {
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                System.err.println("START " + index + " " + Thread.currentThread() + " " + System.currentTimeMillis());
                ConfigurationProperty repoUrl = ConfigurationPropertyMother.create("REPO_URL", false, "http://10.4.7.3:9090/yum/yum-repo-" + (index + 1));
                ConfigurationProperty packageSpec = ConfigurationPropertyMother.create("PACKAGE_SPEC", false, "perf*");

                materialService.latestModification(MaterialsMother.packageMaterial("repo-id", "repo-name", "pkg-id", "pkg-name", "yum", "1.0", Arrays.asList(repoUrl), Arrays.asList(packageSpec)),
                        new File("/tmp"), null);

                System.err.println("END " + index + " " + Thread.currentThread() + " " + System.currentTimeMillis());
                return null;
            }
        };
    }
}
