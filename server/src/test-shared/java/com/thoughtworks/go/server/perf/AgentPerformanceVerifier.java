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
package com.thoughtworks.go.server.perf;

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.server.perf.commands.*;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.Csv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

import static com.google.common.collect.Streams.stream;

/**
 * A test component which helps in verifying multi-threaded scenarios related to agents
 * Usage: add the following line as a test. Make sure that the service objects passed are autowired ones.
 * new AgentPerformanceVerifier(agentService, agentDao, envConfigService, goConfigService, 10).verify();
 */
public class AgentPerformanceVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(AgentPerformanceVerifier.class);
    private static final int DEFAULT_NO_OF_THREADS_TO_USE = 5;

    private final GoConfigService goConfigService;
    private int noOfThreadsToUse;
    private AgentService agentService;
    private final AgentDao agentDao;
    private EnvironmentConfigService environmentConfigService;

    public AgentPerformanceVerifier(AgentService agentService, AgentDao agentDao, EnvironmentConfigService environmentConfigService, GoConfigService goConfigService, int threadCount) {
        this.agentService = agentService;
        this.agentDao = agentDao;
        this.environmentConfigService = environmentConfigService;
        this.goConfigService = goConfigService;
        this.noOfThreadsToUse = threadCount > 0 ? threadCount : DEFAULT_NO_OF_THREADS_TO_USE;
    }

    public void verify() {
        ScheduledExecutorService execService = Executors.newScheduledThreadPool(noOfThreadsToUse);
        Collection<Future<Optional<String>>> futures = new ArrayList<>(noOfThreadsToUse);
        registerSpecifiedNumberOfAgents(execService, futures);

        IntStream.iterate(0, i -> i + 1)
                .limit(noOfThreadsToUse)
                .forEach(val -> {
                    int nextInt = new Random().nextInt(val+1);
//                    UpdateAgentHostCommand updateAgentHostCmd = new UpdateAgentHostCommand(agentService);
                    UpdateAgentResourcesCommand updateAgentResourcesCmd = new UpdateAgentResourcesCommand(agentService);
                    UpdateAgentEnvironmentsCommand updateAgentEnvsCmd = new UpdateAgentEnvironmentsCommand(agentService);
                    UpdateAllAgentAttributesCommand updateAllAgentDetailsCmd = new UpdateAllAgentAttributesCommand(agentService);
                    DisableAgentCommand disableAgentCmd = new DisableAgentCommand(agentService);
//                    BulkUpdateAgentCommand bulkUpdateAgentCmd = new BulkUpdateAgentCommand(agentService, environmentConfigService);
                    CreateEnvironmentCommand createEnvironmentCommand = new CreateEnvironmentCommand(goConfigService, "e" + val);
                    DeleteEnvironmentCommand deleteEnvironmentCommand = new DeleteEnvironmentCommand(goConfigService, "e" + nextInt);


                    futures.add(execService.submit(updateAgentResourcesCmd));
                    futures.add(execService.submit(updateAgentEnvsCmd));
                    futures.add(execService.submit(updateAllAgentDetailsCmd));
                    futures.add(execService.submit(disableAgentCmd));
                    futures.add(execService.submit(createEnvironmentCommand));
                    futures.add(execService.submit(deleteEnvironmentCommand));
                });

        joinFutures(futures);
        generateReport();
        doAssertAgentAndItsAssociationInDBAndCache();
    }

    private void generateReport() {
        Csv csv = new Csv();
        LinkedBlockingQueue<AgentPerformanceCommand> queue = AgentPerformanceCommand.queue;
        try {
            while (!queue.isEmpty()) {
                AgentPerformanceCommand commandExecuted = queue.take();
                addRowToCsv(csv, commandExecuted.getResult());
            }
            String logDir = System.getProperty("gocd.server.log.dir", "logs");
            Path reportFilePath = Files.write(Paths.get(logDir + "/agent-perf-result.csv"), csv.toString().getBytes(), StandardOpenOption.CREATE);
            LOG.info("Report is available at {}", reportFilePath.toAbsolutePath());
        } catch (InterruptedException e) {
            LOG.error("Error while dequeuing", e);
        } catch (IOException e) {
            LOG.error("Error while appending to csv file", e);
        }
    }

    private void addRowToCsv(Csv csv, AgentPerformanceCommandResult result) {
        csv.newRow()
                .put("command_name", result.getName())
                .put("agents", result.getAgentUuids())
                .put("status", result.getStatus())
                .put("failure_message", result.getFailureMessage())
                .put("time_taken_in_millis", String.valueOf(result.getTimeTakenInMillis()));
    }

    private void registerSpecifiedNumberOfAgents(ScheduledExecutorService execService, Collection<Future<Optional<String>>> futures) {
        IntStream.iterate(0, i -> i + 1)
                .limit(noOfThreadsToUse)
                .forEach(val -> {
                    RegisterAgentCommand registerAgentCmd = new RegisterAgentCommand(agentService);
                    futures.add(execService.submit(registerAgentCmd));
                });
    }

    private void doAssertAgentAndItsAssociationInDBAndCache() {
        stream(agentService.getAgentInstances())
                .filter(agentInstance -> agentInstance.getUuid().startsWith("Perf-Test-Agent-"))
                .forEach(agentInstance -> {
                    Agent agentInCache = agentInstance.getAgent();
                    Agent agentInDB = agentDao.fetchAgentFromDBByUUID(agentInCache.getUuid());
                    if (agentInDB == null && !agentInstance.isPending()) {
                        LOG.debug("Agent {} is not pending but not present in DB", agentInCache.getUuid());
                        bombIfAgentInDBAndCacheAreDifferent(agentInCache, agentInDB);
                    }

                    Set<String> agentEnvsInEnvCache = environmentConfigService.getAgentEnvironmentNames(agentInCache.getUuid());
                    HashSet<String> agentEnvsInDB = new HashSet<>(agentInDB.getEnvironmentsAsList());
                    bombIfAgentEnvAssociationInDBAndEnvCacheAreDifferent(agentInCache, agentEnvsInDB, agentEnvsInEnvCache);

                    bombIfMissingAgentKnownEnvAssociationInEnvCache(agentInCache, agentEnvsInEnvCache, agentEnvsInDB);
                });
        LOG.debug("\n\n*************** Hurray! Verification of performance tests succeeded and there are no threading issues reported! ***************\n\n");
    }

    private void bombIfMissingAgentKnownEnvAssociationInEnvCache(Agent agentInCache, Set<String> agentEnvsInEnvCache, HashSet<String> agentEnvsInDB) {
        Set<String> difference = Sets.difference(agentEnvsInDB, agentEnvsInEnvCache);
        HashSet<String> knownEnvNames = new HashSet<>(environmentConfigService.getEnvironmentNames());

        boolean containsOnlyUnknownEnvs = (difference.isEmpty() || !knownEnvNames.containsAll(difference));
        if (!containsOnlyUnknownEnvs) {
            LOG.error("Throwing RuntimeException as verification of agent environments {} in db and environments cache has failed. There are some agent environment associations in DB that does not exist in environment cache", agentInCache.getUuid());
            throw new RuntimeException("WARNING : There is some threading issue found during agent performance test!!!");
        }
    }

    private void bombIfAgentEnvAssociationInDBAndEnvCacheAreDifferent(Agent agentInCache, Set<String> agentEnvsInDB, Set<String> agentEnvsInEnvCache) {
        if (!agentEnvsInDB.containsAll(agentEnvsInEnvCache)) {
            LOG.error("Throwing RuntimeException as verification of agent environments {} in db and environments cache has failed", agentInCache.getUuid());
            throw new RuntimeException("WARNING : There is some threading issue found during agent performance test!!!");
        }
    }

    private void bombIfAgentInDBAndCacheAreDifferent(Agent agentInCache, Agent agentInDB) {
        if (!agentInCache.equals(agentInDB)) {
            LOG.error("Throwing RuntimeException as verification of agents {} in db and cache has failed.\nAgent in DB: {}\nAgent in cache: {}", agentInCache.getUuid(), agentInDB, agentInCache);
            throw new RuntimeException("WARNING : There is some threading issue found during agent performance test!!!");
        }
    }

    private void joinFutures(Collection<Future<Optional<String>>> futures) {
        for (Future<Optional<String>> f : futures) {
            try {
                Optional<String> uuid = f.get();
                uuid.orElseThrow(() -> new Exception("Operation Failed!"));
            } catch (Exception e) {
                LOG.error("############# Exception while performing some operation on performance agent...!!! #############", e);
            }
        }
    }
}
