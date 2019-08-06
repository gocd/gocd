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
package com.thoughtworks.go.server.service;

import com.google.common.collect.Streams;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.config.update.AgentUpdateValidator;
import com.thoughtworks.go.config.update.AgentsUpdateValidator;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.listener.DatabaseEntityChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.notifications.AgentStatusChangeNotifier;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.utils.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.domain.AgentInstance.createFromAgent;
import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static com.thoughtworks.go.util.CommaSeparatedString.append;
import static com.thoughtworks.go.util.CommaSeparatedString.remove;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class AgentService implements DatabaseEntityChangeListener<Agent> {
    private final SystemEnvironment systemEnvironment;
    private final UuidGenerator uuidGenerator;
    private final ServerHealthService serverHealthService;
    private AgentStatusChangeNotifier agentStatusChangeNotifier;
    private final AgentDao agentDao;

    private AgentInstances agentInstances;

    private Set<AgentChangeListener> listeners = new HashSet<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentService.class);

    @Autowired
    public AgentService(SystemEnvironment systemEnvironment, AgentDao agentDao, UuidGenerator uuidGenerator,
                        ServerHealthService serverHealthService, AgentStatusChangeNotifier agentStatusChangeNotifier) {
        this(systemEnvironment, null, agentDao, uuidGenerator, serverHealthService, agentStatusChangeNotifier);
        this.agentInstances = new AgentInstances(agentStatusChangeNotifier);
    }

    AgentService(SystemEnvironment systemEnvironment, AgentInstances agentInstances, AgentDao agentDao, UuidGenerator uuidGenerator,
                 ServerHealthService serverHealthService, AgentStatusChangeNotifier agentStatusChangeNotifier) {
        this.systemEnvironment = systemEnvironment;
        this.agentInstances = agentInstances;
        this.agentDao = agentDao;
        this.uuidGenerator = uuidGenerator;
        this.serverHealthService = serverHealthService;
        this.agentStatusChangeNotifier = agentStatusChangeNotifier;
    }

    public void initialize() {
        this.syncAgentInstanceCacheFromAgentsInDB();
        agentDao.registerListener(this);
    }

    /**
     * not for use externally, created for testing whether listeners are correctly registered or not
     */
    void setAgentChangeListeners(Set<AgentChangeListener> setOfListener) {
        if (setOfListener == null) {
            this.listeners = new HashSet<>();
        } else {
            this.listeners = setOfListener;
        }
    }

    public AgentInstances getAgentInstances() {
        return agentInstances;
    }

    public Agents agents() {
        return agentInstances.values().stream().map(AgentInstance::getAgent).collect(toCollection(Agents::new));
    }

    public Map<AgentInstance, Collection<String>> getAgentInstanceToSortedEnvMap() {
        return Streams.stream(agentInstances.getAllAgents()).collect(toMap(e -> e, AgentService::getSortedEnvironmentList));
    }

    public AgentsViewModel getRegisteredAgentsViewModel() {
        return toAgentViewModels(agentInstances.findRegisteredAgents());
    }

    public AgentInstances findRegisteredAgents() {
        return agentInstances.findRegisteredAgents();
    }

    public AgentInstance updateAgentAttributes(String uuid, String hostname, String resources, EnvironmentsConfig envsConfig,
                                               TriState state, HttpOperationResult result) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        if (isAgentNotFound(agentInstance, result)) {
            return null;
        }
        AgentUpdateValidator validator = new AgentUpdateValidator(agentInstance, state, result);
        Agent agent = agentDao.fetchAgentFromDBByUUID(uuid);
        try {
            if (isAnyOperationPerformedOnAgent(hostname, envsConfig, resources, state, result)) {
                validator.validate();
                setAgentAttributes(hostname, resources, envsConfig, state, agent);
                saveOrUpdate(agent);

                if (agent.hasErrors()) {
                    result.unprocessibleEntity("Updating agent failed.", "", general(GLOBAL));
                } else {
                    result.ok(format("Updated agent with uuid %s.", agent.getUuid()));
                }
                return createFromAgent(agent, systemEnvironment, agentStatusChangeNotifier);
            }
        } catch (InvalidPendingAgentOperationException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            String msg = "Updating agent failed: " + e.getMessage();
            LOGGER.error(msg, e);
            result.internalServerError(msg, general(GLOBAL));
        }
        return null;
    }

    public void bulkUpdateAgentAttributes(List<String> uuids, List<String> resourcesToAdd, List<String> resourcesToRemove,
                                          EnvironmentsConfig envsToAdd, List<String> envsToRemove, TriState state, LocalizedOperationResult result) {
        AgentsUpdateValidator validator
                = new AgentsUpdateValidator(agentInstances, uuids, state, resourcesToAdd, resourcesToRemove, result);
        try {
            if (isAnyOperationPerformedOnBulkAgents(resourcesToAdd, resourcesToRemove, envsToAdd, envsToRemove, state, result)) {
                validator.validate();

                List<Agent> agents = this.agentDao.getAgentsByUUIDs(uuids);
                if (stateIsSet(state)) {
                    agents.addAll(agentInstances.filterPendingAgents(uuids));
                }

                agents.forEach(agent -> setResourcesEnvironmentsAndState(agent, resourcesToAdd, resourcesToRemove,
                        envsToAdd, envsToRemove, state));
                agentDao.bulkUpdateAttributes(agents, createAgentToStatusMap(agents), state);
                result.setMessage("Updated agent(s) with uuid(s): [" + join(uuids, ", ") + "].");
            }
        } catch (Exception e) {
            LOGGER.error("There was an error bulk updating agents", e);
            if (!result.hasMessage()) {
                result.internalServerError("Server error occured. Check log for details.");
            }
        }
    }

    public void updateAgentsAssociationWithSpecifiedEnv(EnvironmentConfig envConfig, List<String> uuidsToAssociateWithEnv, LocalizedOperationResult result) {
        if (envConfig == null) {
            return;
        }

        List<String> uuidsAssociatedWithEnv = getAssociatedUUIDs(envConfig.getAgents());
        EnvironmentsConfig envsConfig = getEnvironmentsConfigFrom(envConfig);
        AgentsUpdateValidator validator = new AgentsUpdateValidator(agentInstances, uuidsToAssociateWithEnv, TriState.TRUE, emptyList(), emptyList(), result);
        try {
            if (isAnyOperationPerformedOnBulkAgents(emptyList(), emptyList(), envsConfig, emptyList(), TriState.TRUE, result)) {
                validator.validate();
                uuidsToAssociateWithEnv = (uuidsToAssociateWithEnv == null) ? emptyList() : uuidsToAssociateWithEnv;
                List<String> uuidsToRemoveEnvFrom = getUUIDsToRemoveEnvFrom(uuidsToAssociateWithEnv, uuidsAssociatedWithEnv);
                List<String> uuidsToAddEnvTo = getUUIDsToAddEnvTo(uuidsToAssociateWithEnv, uuidsAssociatedWithEnv);

                String env = envConfig.name().toString();
                List<Agent> agentsToRemoveEnvFromList = getAgentsToRemoveEnvFromList(uuidsToRemoveEnvFrom, env);
                List<Agent> agents = new ArrayList<>(agentsToRemoveEnvFromList);
                List<Agent> agentsToAddEnvToList = getAgentsToAddEnvToList(uuidsToAddEnvTo, env);
                agents.addAll(agentsToAddEnvToList);

                if (agents.isEmpty()) {
                    return;
                }

                agentDao.bulkUpdateAttributes(agents, emptyMap(), TriState.UNSET);
                String message = "Updated agent(s) with uuid(s): [" + join(uuidsToAssociateWithEnv, ", ") + "].";
                result.setMessage(message);
                LOGGER.debug(message);
            }
        } catch (Exception e) {
            LOGGER.error("There was an error bulk updating agents", e);
            if (!result.hasMessage()) {
                result.internalServerError("Server error occured. Check log for details.");
            }
        }
    }

    public void deleteAgents(List<String> uuids, HttpOperationResult result) {
        try {
            List<AgentInstance> agents = new ArrayList<>();
            if (!validateAndPopulateAgents(uuids, agents, result)) {
                return;
            }
            agentDao.bulkSoftDelete(uuids);
            result.ok(format("Deleted %s agent(s).", agents.size()));
        } catch (Exception e) {
            String errMsg = "Deleting agents failed.";
            LOGGER.error(errMsg, e);
            result.internalServerError(errMsg + e.getMessage(), general(GLOBAL));
        }
    }

    public void updateRuntimeInfo(AgentRuntimeInfo agentRuntimeInfo) {
        bombIfAgentDoesNotHaveACookie(agentRuntimeInfo);
        bombIfAgentHasADuplicateCookie(agentRuntimeInfo);

        AgentInstance agentInstance = findAgentAndRefreshStatus(agentRuntimeInfo.getUUId());
        if (agentInstance.isIpChangeRequired(agentRuntimeInfo.getIpAdress())) {
            LOGGER.warn("Agent with UUID [{}] changed IP Address from [{}] to [{}]", agentRuntimeInfo.getUUId(), agentInstance.getAgent().getIpaddress(), agentRuntimeInfo.getIpAdress());
            Agent agent = (agentInstance.isRegistered() ? agentInstance.getAgent() : null);
            bombIfNull(agent, "Unable to set agent ipAddress; Agent [" + agentInstance.getAgent().getUuid() + "] not found.");
            agent.setIpaddress(agentRuntimeInfo.getIpAdress());
            saveOrUpdate(agent);
        }

        agentInstances.updateAgentRuntimeInfo(agentRuntimeInfo);
    }

    public Username createAgentUsername(String uuId, String ipAddress, String hostNameForDisplay) {
        return new Username(format("agent_%s_%s_%s", uuId, ipAddress, hostNameForDisplay));
    }

    public Registration requestRegistration(AgentRuntimeInfo agentRuntimeInfo) {
        LOGGER.debug("Agent is requesting registration {}", agentRuntimeInfo);

        AgentInstance agentInstance = agentInstances.register(agentRuntimeInfo);
        Registration registration = agentInstance.assignCertification();

        if (agentInstance.isRegistered()) {
            Agent agent = agentInstance.getAgent();
            generateAndAddCookieIfAgentDoesNotHaveCookie(agent);
            saveOrUpdate(agentInstance.getAgent());
            bombIfAgentHasErrors(agent);
            LOGGER.debug("New Agent approved {}", agentRuntimeInfo);
        }

        return registration;
    }

    public void updateAgentApprovalStatus(final String uuid, final Boolean isDenied) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);

        Agent agent = (agentInstance.isRegistered() ? agentInstance.getAgent() : null);
        bombIfNull(agent, "Unable to update agent approval status; Agent [" + uuid + "] not found.");

        agent.setDisabled(isDenied);
        saveOrUpdate(agent);
    }

    @Deprecated
    public void approve(String uuid) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
        boolean hasAgent = isRegistered(agentInstance.getUuid());
        agentInstance.enable();
        if (hasAgent) {
            LOGGER.warn("Registered agent with the same uuid [{}] already approved.", agentInstance);
        } else {
            Agent agent = agentInstance.getAgent();
            generateAndAddCookieIfAgentDoesNotHaveCookie(agent);
            saveOrUpdate(agent);
        }
    }

    public void notifyJobCancelledEvent(String uuid) {
        agentInstances.updateAgentAboutCancelledBuild(uuid, true);
    }

    public AgentInstance findAgentAndRefreshStatus(String uuid) {
        return agentInstances.findAgentAndRefreshStatus(uuid);
    }

    public AgentInstance findAgent(String uuid) {
        return agentInstances.findAgent(uuid);
    }

    public void clearAll() {
        agentInstances.clearAll();
    }

    /**
     * called from spring timer
     */
    public void refresh() {
        agentInstances.refresh();
    }

    public void building(String uuid, AgentBuildingInfo agentBuildingInfo) {
        agentInstances.building(uuid, agentBuildingInfo);
    }

    public String assignCookie(AgentIdentifier identifier) {
        String cookie = uuidGenerator.randomUuid();
        agentDao.associateCookie(identifier, cookie);
        return cookie;
    }

    public Agent findRegisteredAgentByUUID(String uuid) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        if (agentInstance != null && agentInstance.isRegistered()) {
            return agentInstance.getAgent();
        }
        return null;
    }

    public AgentsViewModel filterAgentsViewModel(List<String> uuids) {
        AgentsViewModel agentsViewModel = new AgentsViewModel();
        for (AgentInstance agentInstance : agentInstances.filter(uuids)) {
            agentsViewModel.add(new AgentViewModel(agentInstance));
        }
        return agentsViewModel;
    }

    public AgentViewModel findAgentViewModel(String uuid) {
        return toAgentViewModel(findAgentAndRefreshStatus(uuid));
    }

    public LinkedMultiValueMap<String, ElasticAgentMetadata> allElasticAgents() {
        return agentInstances.getAllElasticAgentsGroupedByPluginId();
    }

    public AgentInstance findElasticAgent(String elasticAgentId, String elasticPluginId) {
        return agentInstances.findElasticAgent(elasticAgentId, elasticPluginId);
    }

    public void register(Agent agent, String agentAutoRegisterResources, String agentAutoRegisterEnvironments) {
        generateAndAddCookieIfAgentDoesNotHaveCookie(agent);
        agent.setResources(agentAutoRegisterResources);
        agent.setEnvironments(agentAutoRegisterEnvironments);
        saveOrUpdate(agent);
    }

    public boolean isRegistered(String uuid) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        return !agentInstance.isNullAgent() && agentInstance.isRegistered();
    }

    public Agent getAgentByUUID(String uuid) {
        return agentInstances.findAgent(uuid).getAgent();
    }

    public List<String> getAllRegisteredAgentUUIDs() {
        return agentInstances.values().stream()
                .filter(AgentInstance::isRegistered)
                .map(AgentInstance::getUuid)
                .collect(toList());
    }

    public void disableAgents(List<String> uuids) {
        if (!isEmpty(uuids)) {
            agentDao.enableOrDisableAgents(uuids, true);
        }
    }

    public void disableAgents(String... uuids) {
        if (uuids != null) {
            agentDao.enableOrDisableAgents(asList(uuids), true);
        }
    }

    public void saveOrUpdate(Agent agent) {
        agent.validate();
        if (!agent.hasErrors()) {
            agentDao.saveOrUpdate(agent);
        }
    }

    public List<String> getListOfResourcesAcrossAgents() {
        return agents().stream()
                .map(Agent::getResourcesAsList)
                .flatMap(Collection::stream)
                .distinct()
                .collect(toList());
    }

    @Override
    public void bulkEntitiesChanged(List<Agent> agents) {
        agents.forEach(this::entityChanged);
    }

    @Override
    public void bulkEntitiesDeleted(List<String> deletedUuids) {
        deletedUuids.forEach(this::entityDeleted);
    }

    @Override
    public void entityChanged(Agent agentAfterUpdate) {
        AgentInstance agentInstanceBeforeUpdate = agentInstances.findAgent(agentAfterUpdate.getUuid());

        if (agentInstanceBeforeUpdate instanceof NullAgentInstance) {
            agentInstanceBeforeUpdate = createFromAgent(agentAfterUpdate, new SystemEnvironment(), agentStatusChangeNotifier);
            this.agentInstances.add(agentInstanceBeforeUpdate);
        } else {
            Agent agentBeforeUpdate = agentInstanceBeforeUpdate.getAgent();
            notifyAgentChangeListener(agentBeforeUpdate, agentAfterUpdate);
            agentInstanceBeforeUpdate.syncAgentFrom(agentAfterUpdate);
        }
    }

    public void registerAgentChangeListeners(AgentChangeListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    void entityDeleted(String uuid) {
        notifyAgentDeleteListener(uuid);
        this.agentInstances.removeAgent(uuid);
    }

    private void syncAgentInstanceCacheFromAgentsInDB() {
        Agents allAgentsFromDB = new Agents(agentDao.getAllAgents());
        agentInstances.syncAgentInstancesFrom(allAgentsFromDB);
    }

    private void setResourcesEnvironmentsAndState(Agent agent, List<String> resourcesToAdd, List<String> resourcesToRemove,
                                                  EnvironmentsConfig envsToAdd, List<String> envsToRemove, TriState state) {
        addRemoveEnvsAndResources(agent, envsToAdd, envsToRemove, resourcesToAdd, resourcesToRemove);
        enableDisableAgent(state, agent);
    }

    private void generateAndAddCookieIfAgentDoesNotHaveCookie(Agent agent) {
        if (agent.getCookie() == null) {
            String cookie = uuidGenerator.randomUuid();
            agent.setCookie(cookie);
        }
    }

    private void bombIfAgentHasErrors(Agent agent) {
        if (agent.hasErrors()) {
            List<ConfigErrors> errors = agent.errorsAsList();
            throw new GoConfigInvalidException(null, new AllConfigErrors(errors));
        }
    }

    private void setAgentAttributes(String newHostname, String resources, EnvironmentsConfig environments, TriState state, Agent agent) {
        if (state.isTrue()) {
            agent.enable();
        }

        if (state.isFalse()) {
            agent.disable();
        }

        if (newHostname != null) {
            agent.setHostname(newHostname);
        }

        if (resources != null) {
            agent.setResources(resources);
        }

        setOnlyThoseEnvsThatAreNotAssociatedWithAgentFromConfigRepo(environments, agent);
    }

    private void notifyAgentChangeListener(Agent agentBeforeUpdate, Agent agentAfterUpdate) {
        AgentChangedEvent event = new AgentChangedEvent(agentBeforeUpdate, agentAfterUpdate);
        listeners.forEach(listener -> listener.agentChanged(event));
    }

    private void notifyAgentDeleteListener(String uuid) {
        listeners.forEach(listener -> {
            AgentInstance agent = agentInstances.findAgent(uuid);
            listener.agentDeleted(agent.getAgent());
        });
    }

    private static Collection<String> getSortedEnvironmentList(AgentInstance agentInstance) {
        return agentInstance.getAgent().getEnvironmentsAsList().stream().sorted().collect(toList());
    }

    private void bombIfAgentHasADuplicateCookie(AgentRuntimeInfo agentRuntimeInfo) {
        if (agentRuntimeInfo.hasDuplicateCookie(agentDao.cookieFor(agentRuntimeInfo.getIdentifier()))) {
            LOGGER.warn("Found agent [{}] with duplicate uuid. Please check the agent installation.", agentRuntimeInfo.agentInfoDebugString());
            serverHealthService.update(
                    ServerHealthState.warning(format("[%s] has duplicate unique identifier which conflicts with [%s]", agentRuntimeInfo.agentInfoForDisplay(), findAgentAndRefreshStatus(agentRuntimeInfo.getUUId()).agentInfoForDisplay()),
                            "Please check the agent installation. Click <a href='" + docsUrl("/faq/agent_guid_issue.html") + "' target='_blank'>here</a> for more info.",
                            HealthStateType.duplicateAgent(HealthStateScope.forAgent(agentRuntimeInfo.getCookie())), Timeout.THIRTY_SECONDS));
            throw new AgentWithDuplicateUUIDException(format("Agent [%s] has invalid cookie", agentRuntimeInfo.agentInfoDebugString()));
        }
    }

    private void bombIfAgentDoesNotHaveACookie(AgentRuntimeInfo agentRuntimeInfo) {
        if (!agentRuntimeInfo.hasCookie()) {
            LOGGER.warn("Agent [{}] has no cookie set", agentRuntimeInfo.agentInfoDebugString());
            throw new AgentNoCookieSetException(format("Agent [%s] has no cookie set", agentRuntimeInfo.agentInfoDebugString()));
        }
    }

    private List<String> getUUIDsToAddEnvTo(List<String> uuidsToAssociateWithEnv, List<String> uuidsAssociatedWithEnv) {
        return uuidsToAssociateWithEnv.stream()
                .filter(uuid -> !uuidsAssociatedWithEnv.contains(uuid))
                .collect(toList());
    }

    private List<String> getUUIDsToRemoveEnvFrom(List<String> uuidsToAssociateWithEnv, List<String> uuidsAssociatedWithEnv) {
        return uuidsAssociatedWithEnv.stream()
                .filter(uuid -> !uuidsToAssociateWithEnv.contains(uuid))
                .collect(toList());
    }

    private List<Agent> getAgentsToAddEnvToList(List<String> uuidsToAddEnvsTo, String env) {
        return uuidsToAddEnvsTo.stream()
                .map(uuid -> {
                    Agent agent = agentDao.getAgentByUUIDFromCacheOrDB(uuid);
                    String envsToSet = append(agent.getEnvironments(), singletonList(env));
                    agent.setEnvironments(envsToSet);
                    return agent;
                })
                .collect(toList());
    }

    private List<Agent> getAgentsToRemoveEnvFromList(List<String> agentsToRemoveEnvFrom, String env) {
        return agentsToRemoveEnvFrom.stream()
                .map(uuid -> {
                    Agent agent = agentDao.getAgentByUUIDFromCacheOrDB(uuid);
                    String envsToSet = remove(agent.getEnvironments(), singletonList(env));
                    agent.setEnvironments(envsToSet);
                    return agent;
                })
                .collect(toList());
    }

    private List<String> getAssociatedUUIDs(EnvironmentAgentsConfig asssociatedAgents) {
        List<String> associatedUUIDs = emptyList();

        if (!isEmpty(asssociatedAgents)) {
            associatedUUIDs = asssociatedAgents.stream().map(EnvironmentAgentConfig::getUuid).collect(toList());
        }

        return associatedUUIDs;
    }

    private HashMap<String, AgentConfigStatus> createAgentToStatusMap(List<Agent> agents) {
        HashMap<String, AgentConfigStatus> agentToStatusMap = new HashMap<>();

        agents.forEach(agent -> {
            AgentInstance agentInstance = agentInstances.findAgent(agent.getUuid());
            AgentConfigStatus agentConfigStatus = agentInstance.getStatus().getConfigStatus();
            agentToStatusMap.put(agent.getUuid(), agentConfigStatus);
        });

        return agentToStatusMap;
    }

    private void enableDisableAgent(TriState enable, Agent agent) {
        if (enable.isTrue()) {
            agent.setDisabled(false);
        } else if (enable.isFalse()) {
            agent.setDisabled(true);
        }
    }

    private void addOnlyThoseEnvsThatAreNotAssociatedWithAgentFromConfigRepo(EnvironmentsConfig envsToAdd, Agent agent) {
        if (envsToAdd != null) {
            String uuid = agent.getUuid();
            envsToAdd.forEach(env -> {
                if (env.containsAgentRemotely(uuid)) {
                    LOGGER.info(format("Not adding Agent [%s] to Environment [%s] as it is already associated from a Config Repo", uuid, env.name().toString()));
                } else {
                    String envName = env.name().toString();
                    agent.addEnvironment(envName);
                }
            });
        }
    }

    private void setOnlyThoseEnvsThatAreNotAssociatedWithAgentFromConfigRepo(EnvironmentsConfig envsConfig, Agent agent) {
        if (envsConfig != null) {
            ArrayList<String> envsToSetList = envsConfig.stream()
                    .filter(env -> !env.containsAgentRemotely(agent.getUuid()))
                    .map(env -> env.name().toString())
                    .collect(toCollection(ArrayList::new));

            agent.setEnvironmentsFrom(envsToSetList);
        }
    }

    private void addRemoveEnvsAndResources(Agent agent, EnvironmentsConfig envsToAdd, List<String> envsToRemove,
                                           List<String> resourcesToAdd, List<String> resourcesToRemove) {
        addOnlyThoseEnvsThatAreNotAssociatedWithAgentFromConfigRepo(envsToAdd, agent);
        agent.removeEnvironments(envsToRemove);

        agent.addResources(resourcesToAdd);
        agent.removeResources(resourcesToRemove);
    }

    private boolean validateThatAllAgentsExistAndPopulateAgents(List<String> uuids, List<AgentInstance> agents, OperationResult result) {
        if (isEmpty(uuids)) {
            return true;
        }

        for (String uuid : uuids) {
            AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
            if (isAgentNotFound(agentInstance.getAgent(), result)) {
                return false;
            }
            agents.add(agentInstance);
        }

        return true;
    }

    private AgentsViewModel toAgentViewModels(AgentInstances agentInstances) {
        return stream(agentInstances.spliterator(), false)
                .map(this::toAgentViewModel)
                .collect(toCollection(AgentsViewModel::new));
    }

    private AgentViewModel toAgentViewModel(AgentInstance instance) {
        return new AgentViewModel(instance, instance.getAgent().getEnvironmentsAsList());
    }

    private EnvironmentsConfig getEnvironmentsConfigFrom(EnvironmentConfig envConfig) {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        envsConfig.add(envConfig);
        return envsConfig;
    }

    private boolean isAgentNotFound(Agent agent, OperationResult result) {
        if (agent.isNull()) {
            result.notFound("Not Found", format("Agent '%s' not found", agent.getUuid()), general(GLOBAL));
            return true;
        }
        return false;
    }

    private boolean isAgentNotFound(AgentInstance agentInstance, OperationResult result) {
        if (agentInstance.isNullAgent()) {
            String agentNotFoundMessage = format("Agent '%s' not found.", agentInstance.getUuid());
            result.notFound(agentNotFoundMessage, agentNotFoundMessage, general(GLOBAL));
            return true;
        }
        return false;
    }

    private boolean validateAndPopulateAgents(List<String> uuids, List<AgentInstance> agents, HttpOperationResult result) {
        if (!validateThatAllAgentsExistAndPopulateAgents(uuids, agents, result)) {
            return false;
        }
        if (!validateThatAllAgentsCanBeDeleted(agents, result)) {
            return false;
        }
        return true;
    }

    private boolean validateThatAllAgentsCanBeDeleted(List<AgentInstance> agents, HttpOperationResult result) {
        for (AgentInstance agentInstance : agents) {
            if (!agentInstance.canBeDeleted()) {
                result.notAcceptable(getFailedToDeleteMessage(agents.size()), general(GLOBAL));
                return false;
            }
        }
        return true;
    }

    private String getFailedToDeleteMessage(int numOfAgents) {
        if (numOfAgents == 1) {
            return "Failed to delete an agent, as it is not in a disabled state or is still building.";
        } else {
            return "Could not delete any agents, as one or more agents might not be disabled or are still building.";
        }
    }

    boolean isAnyOperationPerformedOnAgent(String hostname, EnvironmentsConfig environments,
                                           String resources, TriState state, HttpOperationResult result) {
        boolean anyOperationPerformed = (resources != null || environments != null || hostname != null || stateIsSet(state));
        if (!anyOperationPerformed) {
            String msg = "Bad Request. No operation is specified in the request to be performed on agent.";
            result.badRequest(msg, msg, general(GLOBAL));
            return false;
        }
        return true;
    }

    private boolean stateIsSet(TriState state) {
        return state.isTrue() || state.isFalse();
    }

    boolean isAnyOperationPerformedOnBulkAgents(List<String> resourcesToAdd, List<String> resourcesToRemove,
                                                EnvironmentsConfig envsToAdd, List<String> envsToRemove,
                                                TriState state, LocalizedOperationResult result) {
        boolean anyOperationPerformed
                = isNotEmpty(resourcesToAdd)
                || isNotEmpty(resourcesToRemove)
                || isNotEmpty(envsToAdd)
                || isNotEmpty(envsToRemove)
                || stateIsSet(state);
        if (!anyOperationPerformed) {
            result.badRequest("Bad Request. No operation is specified in the request to be performed on agents.");
            return false;
        }
        return true;
    }
}
