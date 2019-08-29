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
package com.thoughtworks.go.server.service.perf;

import com.google.common.collect.Sets;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.server.service.perf.commands.*;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

import static com.google.common.collect.Streams.stream;

public class AgentPerformanceVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(AgentPerformanceVerifier.class);
    private static final int DEFAULT_NO_OF_THREADS_TO_USE = 5;

    private int noOfThredsToUse;
    private AgentService agentService;
    private EnvironmentConfigService environmentConfigService;

    public AgentPerformanceVerifier(AgentService agentService, EnvironmentConfigService environmentConfigService, int threadCount) {
        this.agentService = agentService;
        this.environmentConfigService = environmentConfigService;
        this.noOfThredsToUse = threadCount > 0 ? threadCount : DEFAULT_NO_OF_THREADS_TO_USE;
    }

    public void verify() {
        ScheduledExecutorService execService = Executors.newScheduledThreadPool(noOfThredsToUse);
        Collection<Future<Optional<String>>> futures = new ArrayList<>(noOfThredsToUse);

        registerSpecifiedNumberOfAgents(execService, futures);

        IntStream.iterate(0, i -> i + 1)
                .limit(noOfThredsToUse)
                .forEach(val -> {
                    UpdateAgentHostCommandAgent updateAgentHostCmd = new UpdateAgentHostCommandAgent(agentService);
                    UpdateAgentResourcesCommand updateAgentResourcesCmd = new UpdateAgentResourcesCommand(agentService);
                    UpdateAgentEnvironmentsCommand updateAgentEnvsCmd = new UpdateAgentEnvironmentsCommand(agentService);
                    UpdateAllAgentAttributesCommand updateAllAgentDetailsCmd = new UpdateAllAgentAttributesCommand(agentService);
                    DisableAgentCommand disableAgentCmd = new DisableAgentCommand(agentService);
                    BulkUpdateAgentCommand bulkUpdateAgentCmd = new BulkUpdateAgentCommand(agentService, environmentConfigService);

                    futures.add(execService.submit(new AgentPerformanceCommandDecorator(updateAgentHostCmd)));
                    futures.add(execService.submit(new AgentPerformanceCommandDecorator(updateAgentResourcesCmd)));
                    futures.add(execService.submit(new AgentPerformanceCommandDecorator(updateAgentEnvsCmd)));
                    futures.add(execService.submit(new AgentPerformanceCommandDecorator(updateAllAgentDetailsCmd)));
                    futures.add(execService.submit(new AgentPerformanceCommandDecorator(disableAgentCmd)));
                    futures.add(execService.submit(new AgentPerformanceCommandDecorator(bulkUpdateAgentCmd)));
                });

        joinFutures(futures);
        doAssertAgentCache();
    }

    private void registerSpecifiedNumberOfAgents(ScheduledExecutorService execService, Collection<Future<Optional<String>>> futures) {
        IntStream.iterate(0, i -> i + 1)
                .limit(noOfThredsToUse)
                .forEach(val -> {
                    RegisterAgentCommand registerAgentCmd = new RegisterAgentCommand(agentService);
                    AgentPerformanceCommandDecorator cmdDecorator = new AgentPerformanceCommandDecorator(registerAgentCmd);
                    futures.add(execService.submit(cmdDecorator));
                });
    }

    private void doAssertAgentCache(){
        stream(agentService.getAgentInstances())
                .filter(agentInstance -> agentInstance.getUuid().startsWith("Perf-Test-Agent-"))
                .forEach(agentInstance -> {
                    Agent agentInCache = agentInstance.getAgent();
                    Agent agentInDB = agentService.fetchAgentFromDBByUUID(agentInCache.getUuid());
                    bombIfAgentInDBAndCacheAreDifferent(agentInCache, agentInDB);

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
        if(!containsOnlyUnknownEnvs){
            LOG.error("Throwing RuntimeException as verification of agent environments {} in db and environments cache has failed. There are some agent environment associations in DB that does not exist in environment cache", agentInCache.getUuid());
            throw new RuntimeException("WARNING : There is some threading issue found during agent performance test!!!");
        }
    }

    private void bombIfAgentEnvAssociationInDBAndEnvCacheAreDifferent(Agent agentInCache, Set<String> agentEnvsInDB, Set<String> agentEnvsInEnvCache) {
        if(!agentEnvsInDB.containsAll(agentEnvsInEnvCache)){
            LOG.error("Throwing RuntimeException as verification of agent environments {} in db and environments cache has failed", agentInCache.getUuid());
            throw new RuntimeException("WARNING : There is some threading issue found during agent performance test!!!");
        }
    }

    private void bombIfAgentInDBAndCacheAreDifferent(Agent agentInCache, Agent agentInDB) {
        if (!agentInCache.equals(agentInDB)) {
            LOG.error("Throwing RuntimeException as verification of agents {} in db and cache has failed", agentInCache.getUuid());
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
