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
package com.thoughtworks.go.server.service;

import com.google.common.collect.Streams;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.*;
import com.thoughtworks.go.config.update.AgentUpdateValidator;
import com.thoughtworks.go.config.update.AgentsUpdateValidator;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AllConfigErrors;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.domain.exception.ForceCancelException;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.listener.DatabaseEntityChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.notifications.AgentStatusChangeNotifier;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.utils.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.domain.AgentConfigStatus.Pending;
import static com.thoughtworks.go.domain.AgentInstance.createFromAgent;
import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static com.thoughtworks.go.util.CommaSeparatedString.append;
import static com.thoughtworks.go.util.CommaSeparatedString.remove;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static com.thoughtworks.go.util.TriState.TRUE;
import static java.lang.String.format;
import static java.time.Duration.between;
import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.union;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@SuppressWarnings("deprecation")
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
        agentDao.registerDatabaseAgentEntityChangeListener(this);
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
        return stream(agentInstances.spliterator(), false)
                .map(AgentInstance::getAgent)
                .collect(toCollection(Agents::new));
    }

    public Map<AgentInstance, Collection<String>> getAgentInstanceToSortedEnvMap() {
        return Streams.stream(agentInstances.getAllAgents()).collect(toMap(Function.identity(), AgentService::getSortedEnvironmentList));
    }

    public AgentsViewModel getRegisteredAgentsViewModel() {
        return toAgentViewModels(agentInstances.findRegisteredAgents());
    }

    public AgentInstances findRegisteredAgents() {
        return agentInstances.findRegisteredAgents();
    }

    public AgentInstance updateAgentAttributes(String uuid, String hostname, String resources, String environments, TriState state) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        validateThatAgentExists(agentInstance);

        Agent agent = getPendingAgentOrFromDB(agentInstance);
        if (validateAnyOperationPerformedOnAgent(hostname, environments, resources, state)) {
            new AgentUpdateValidator(agentInstance, state).validate();
            setAgentAttributes(hostname, resources, environments, state, agent);
            saveOrUpdate(agent);
            return createFromAgent(agent, systemEnvironment, agentStatusChangeNotifier);
        }

        return null;
    }

    public void bulkUpdateAgentAttributes(List<String> uuids, List<String> resourcesToAdd, List<String> resourcesToRemove,
                                          List<String> envsToAdd, List<String> envsToRemove, TriState state,
                                          EnvironmentConfigService environmentConfigService) {
        if (isAnyOperationPerformedOnBulkAgents(resourcesToAdd, resourcesToRemove, envsToAdd, envsToRemove, state)) {
            AgentsUpdateValidator validator = new AgentsUpdateValidator(agentInstances, uuids, state, resourcesToAdd, resourcesToRemove);
            validator.validate();

            List<Agent> agents = agentDao.getAgentsByUUIDs(uuids);
            if (isTriStateSet(state)) {
                agents.addAll(agentInstances.filterPendingAgents(uuids));
            }

            agents.forEach(agent -> setResourcesEnvsAndState(agent, resourcesToAdd, resourcesToRemove, envsToAdd, envsToRemove, state, environmentConfigService));
            updateIdsAndGenerateCookiesForPendingAgents(agents, state);
            agentDao.bulkUpdateAgents(agents);
        }
    }

    public void updateAgentsAssociationOfEnvironment(EnvironmentConfig envConfig, List<String> uuids) {
        if (envConfig == null) {
            return;
        }

        AgentsUpdateValidator validator = new AgentsUpdateValidator(agentInstances, uuids, TRUE, emptyList(), emptyList());
        if (isAnyOperationPerformedOnBulkAgents(emptyList(), emptyList(), singletonList(envConfig.name().toString()), emptyList(), TRUE)) {
            validator.validate();

            List<String> uuidsToAssociate = (uuids == null) ? emptyList() : uuids;
            List<Agent> agents = getAgentsToAddEnvToOrRemoveEnvFrom(envConfig, uuidsToAssociate);

            if (agents.isEmpty()) {
                return;
            }
            agentDao.bulkUpdateAgents(agents);
        }
    }

    public void updateAgentsAssociationOfEnvironment(EnvironmentConfig envConfig, List<String> agentUUIDsToAssociate, List<String> agentUUIDsToRemove) {
        if (envConfig == null) {
            return;
        }

        AgentsUpdateValidator validator = new AgentsUpdateValidator(agentInstances, union(agentUUIDsToAssociate, agentUUIDsToRemove), TRUE, emptyList(), emptyList());
        if (isAnyOperationPerformedOnBulkAgents(emptyList(), emptyList(), singletonList(envConfig.name().toString()), emptyList(), TRUE)) {
            validator.validate();

            List<Agent> agents = getAgentsToAddEnvToOrRemoveEnvFrom(envConfig.name().toString(), agentUUIDsToAssociate, agentUUIDsToRemove);

            if (agents.isEmpty()) {
                return;
            }
            agentDao.bulkUpdateAgents(agents);
        }
    }

    public void deleteAgents(List<String> uuids) {
        if (validateThatAllAgentsExistAndCanBeDeleted(uuids)) {
            agentDao.bulkSoftDelete(uuids);
        }
    }

    public void deleteAgentsWithoutValidations(List<String> uuids) {
        if (!isEmpty(uuids)) {
            agentDao.bulkSoftDelete(uuids);
        }
    }

    public void updateRuntimeInfo(AgentRuntimeInfo agentRuntimeInfo) {
        bombIfAgentDoesNotHaveCookie(agentRuntimeInfo);
        bombIfAgentHasDuplicateCookie(agentRuntimeInfo);

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

    public boolean requestRegistration(AgentRuntimeInfo agentRuntimeInfo) {
        LOGGER.debug("Agent is requesting registration {}", agentRuntimeInfo);

        AgentInstance agentInstance = agentInstances.register(agentRuntimeInfo);
        boolean registration = agentInstance.assignCertification();

        Agent agent = agentInstance.getAgent();
        if (agentInstance.isRegistered() && !agent.cookieAssigned()) {
            generateAndAddCookie(agent);
            saveOrUpdate(agentInstance.getAgent());
            bombIfAgentHasErrors(agent);
            LOGGER.debug("New Agent approved {}", agentRuntimeInfo);
        }

        return registration;
    }

    @Deprecated
    public void approve(String uuid) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
        boolean doesAgentExistAndIsRegistered = isRegistered(agentInstance.getUuid());
        agentInstance.enable();

        if (doesAgentExistAndIsRegistered) {
            LOGGER.warn("Registered agent with the same uuid [{}] already approved.", agentInstance);
        } else {
            Agent agent = agentInstance.getAgent();
            if (!agent.cookieAssigned()) {
                generateAndAddCookie(agent);
            }
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
        addWarningForAgentsStuckInCancel();
    }

    private void addWarningForAgentsStuckInCancel() {
        agentInstances.agentsStuckInCancel().stream().forEach(agentInstance -> {
            serverHealthService.update(warning(format("Agent `%s` is stuck in cancel.", agentInstance.getHostname()),
                    format("Looks like agent is stuck in cancelling a job, the job was cancelled: %s (mins) back.", cancelledForMins(agentInstance.cancelledAt())),
                    HealthStateType.general(GLOBAL), Timeout.THIRTY_SECONDS));
        });
    }

    public void killAllRunningTasksOnAgent(String uuid) throws ForceCancelException {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        if (agentInstance.isNullAgent()) {
            throw new RecordNotFoundException(format("Agent with uuid: '%s' not found", uuid));
        }

        agentInstance.forceCancel();
    }

    private long cancelledForMins(Date cancelledAt) {
        return between(now(), ofInstant(Instant.ofEpochMilli(cancelledAt.getTime()), systemDefault())).toMinutes();
    }

    public void building(String uuid, AgentBuildingInfo agentBuildingInfo) {
        agentInstances.building(uuid, agentBuildingInfo);
    }

    public String assignCookie(AgentIdentifier identifier) {
        String cookie = uuidGenerator.randomUuid();
        agentDao.associateCookie(identifier, cookie);
        return cookie;
    }

    public Agent findAgentByUUID(String uuid) {
        if (isNullOrEmpty(uuid)) {
            return null;
        }

        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        Agent agent;
        if (agentInstance != null && !agentInstance.isNullAgent()) {
            agent = agentInstance.getAgent();
        } else {
            agent = agentDao.fetchAgentFromDBByUUIDIncludingDeleted(uuid);
        }
        return agent;
    }

    public AgentsViewModel filterAgentsViewModel(List<String> uuids) {
        return agentInstances.filter(uuids).stream().map(AgentViewModel::new).collect(toCollection(AgentsViewModel::new));
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

    public void register(Agent agent) {
        generateAndAddCookie(agent);
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
        return stream(agentInstances.spliterator(), false)
                .filter(AgentInstance::isRegistered)
                .map(AgentInstance::getUuid)
                .collect(toList());
    }

    public void disableAgents(List<String> uuids) {
        if (!isEmpty(uuids)) {
            agentDao.disableAgents(uuids);
        }
    }

    public void disableAgents(String... uuids) {
        if (uuids != null) {
            agentDao.disableAgents(asList(uuids));
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
            createNewAgentInstanceAndAddToCache(agentAfterUpdate);
        } else {
            notifyAgentChangeListenersAndSyncAgentFromUpdatedAgent(agentAfterUpdate, agentInstanceBeforeUpdate);
        }
    }

    private void notifyAgentChangeListenersAndSyncAgentFromUpdatedAgent(Agent agentAfterUpdate, AgentInstance agentInstanceBeforeUpdate) {
        notifyAgentChangeListeners(agentAfterUpdate);
        agentInstanceBeforeUpdate.syncAgentFrom(agentAfterUpdate);
    }

    private void createNewAgentInstanceAndAddToCache(Agent agentAfterUpdate) {
        AgentInstance agentInstanceBeforeUpdate = createFromAgent(agentAfterUpdate, new SystemEnvironment(), agentStatusChangeNotifier);
        this.agentInstances.add(agentInstanceBeforeUpdate);
    }

    void registerAgentChangeListeners(AgentChangeListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    void entityDeleted(String uuid) {
        notifyAgentDeleteListeners(uuid);
        this.agentInstances.removeAgent(uuid);
    }

    private void syncAgentInstanceCacheFromAgentsInDB() {
        Agents allAgentsFromDB = new Agents(agentDao.getAllAgents());
        agentInstances.syncAgentInstancesFrom(allAgentsFromDB);
    }

    private void setResourcesEnvsAndState(Agent agent, List<String> resourcesToAdd, List<String> resourcesToRemove,
                                          List<String> envsToAdd, List<String> envsToRemove, TriState state, EnvironmentConfigService environmentConfigService) {
        addRemoveEnvsAndResources(agent, envsToAdd, envsToRemove, resourcesToAdd, resourcesToRemove, environmentConfigService);
        enableOrDisableAgent(agent, state);
    }

    private void generateAndAddCookie(Agent agent) {
        String cookie = uuidGenerator.randomUuid();
        agent.setCookie(cookie);
    }

    private void bombIfAgentHasErrors(Agent agent) {
        if (agent.hasErrors()) {
            List<ConfigErrors> errors = agent.errorsAsList();
            throw new GoConfigInvalidException(null, new AllConfigErrors(errors));
        }
    }

    private Agent getPendingAgentOrFromDB(AgentInstance agentInstance) {
        if (agentInstance.isPending()) {
            Agent agent = new Agent(agentInstance.getAgent());
            generateAndAddCookie(agent);
            return agent;
        }
        return agentDao.fetchAgentFromDBByUUID(agentInstance.getUuid());
    }

    private void setAgentAttributes(String newHostname, String resources, String environments, TriState state, Agent agent) {
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
        if (environments != null) {
            agent.setEnvironments(environments);
        }
    }

    private void notifyAgentChangeListeners(Agent agentAfterUpdate) {
        listeners.forEach(listener -> listener.agentChanged(agentAfterUpdate));
    }

    private void notifyAgentDeleteListeners(String uuid) {
        listeners.forEach(listener -> listener.agentDeleted(agentInstances.findAgent(uuid).getAgent()));
    }

    private static Collection<String> getSortedEnvironmentList(AgentInstance agentInstance) {
        return agentInstance.getAgent().getEnvironmentsAsList().stream().sorted().collect(toList());
    }

    private void bombIfAgentHasDuplicateCookie(AgentRuntimeInfo agentRuntimeInfo) {
        if (agentRuntimeInfo.hasDuplicateCookie(agentDao.cookieFor(agentRuntimeInfo.getIdentifier()))) {
            LOGGER.warn("Found agent [{}] with duplicate uuid. Please check the agent installation.", agentRuntimeInfo.agentInfoDebugString());
            serverHealthService.update(
                    warning(format("[%s] has duplicate unique identifier which conflicts with [%s]", agentRuntimeInfo.agentInfoForDisplay(), findAgentAndRefreshStatus(agentRuntimeInfo.getUUId()).agentInfoForDisplay()),
                            "Please check the agent installation. Click <a href='" + docsUrl("/faq/agent_guid_issue.html") + "' target='_blank'>here</a> for more info.",
                            HealthStateType.duplicateAgent(HealthStateScope.forAgent(agentRuntimeInfo.getCookie())), Timeout.THIRTY_SECONDS));
            throw new AgentWithDuplicateUUIDException(format("Agent [%s] has invalid cookie", agentRuntimeInfo.agentInfoDebugString()));
        }
    }

    private void bombIfAgentDoesNotHaveCookie(AgentRuntimeInfo agentRuntimeInfo) {
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

    private List<Agent> getAgentsFromDBToAddEnvTo(List<String> UUIDs, List<String> associatedUUIDs, String env) {
        List<String> UUIDsToAddEnvTo = getUUIDsToAddEnvTo(UUIDs, associatedUUIDs);
        return UUIDsToAddEnvTo.stream()
                .map(uuid -> getAgentFromDBAfterAddingEnvToExistingEnvs(env, uuid))
                .collect(toList());
    }

    private Agent getAgentFromDBAfterAddingEnvToExistingEnvs(String env, String uuid) {
        Agent agent = agentDao.getAgentByUUIDFromCacheOrDB(uuid);
        String envsToSet = append(agent.getEnvironments(), singletonList(env));
        agent.setEnvironments(envsToSet);
        return agent;
    }

    private List<Agent> getAgentsFromDBToRemoveEnvFrom(List<String> UUIDs, List<String> associatedUUIDs, String env) {
        List<String> uuidsToRemoveEnvFrom = getUUIDsToRemoveEnvFrom(UUIDs, associatedUUIDs);
        return uuidsToRemoveEnvFrom.stream()
                .map(uuid -> getAgentFromDBAfterRemovingEnvFromExistingEnvs(env, uuid))
                .collect(toList());
    }

    private Agent getAgentFromDBAfterRemovingEnvFromExistingEnvs(String env, String uuid) {
        Agent agent = agentDao.getAgentByUUIDFromCacheOrDB(uuid);
        String envsToSet = remove(agent.getEnvironments(), singletonList(env));
        agent.setEnvironments(envsToSet);
        return agent;
    }

    private List<String> toUUIDList(EnvironmentAgentsConfig asssociatedAgents) {
        List<String> associatedUUIDs = emptyList();

        if (!isEmpty(asssociatedAgents)) {
            associatedUUIDs = asssociatedAgents.stream().map(EnvironmentAgentConfig::getUuid).collect(toList());
        }

        return associatedUUIDs;
    }

    private void enableOrDisableAgent(Agent agent, TriState triState) {
        if (triState.isTrue()) {
            agent.setDisabled(false);
        } else if (triState.isFalse()) {
            agent.setDisabled(true);
        }
    }

    private void addOnlyThoseEnvsThatAreNotAssociatedWithAgentFromConfigRepo(List<String> envsToAdd, Agent agent, EnvironmentConfigService environmentConfigService) {
        if (envsToAdd != null) {
            String uuid = agent.getUuid();
            envsToAdd.forEach(envName -> {
                EnvironmentConfig env = environmentConfigService.find(envName);
                if (env != null && env.containsAgentRemotely(uuid)) {
                    LOGGER.info(format("Not adding Agent [%s] to Environment [%s] as it is already associated from a Config Repo", uuid, envName));
                } else {
                    agent.addEnvironment(envName);
                }
            });
        }
    }

    private void addRemoveEnvsAndResources(Agent agent, List<String> envsToAdd, List<String> envsToRemove,
                                           List<String> resourcesToAdd, List<String> resourcesToRemove, EnvironmentConfigService environmentConfigService) {
        addOnlyThoseEnvsThatAreNotAssociatedWithAgentFromConfigRepo(envsToAdd, agent, environmentConfigService);
        agent.removeEnvironments(envsToRemove);
        agent.addResources(resourcesToAdd);
        agent.removeResources(resourcesToRemove);
    }

    private boolean validateThatAllAgentsExistAndCanBeDeleted(List<String> uuids) {
        if (isEmpty(uuids)) {
            return true;
        }
        return uuids.stream().allMatch(uuid -> validateThatAgentExistAndCanBeDeleted(uuid, uuids.size()));
    }

    private boolean validateThatAgentExists(AgentInstance agentInstance) {
        if (agentInstance.isNullAgent()) {
            throw new RecordNotFoundException(EntityType.Agent, agentInstance.getUuid());
        }
        return true;
    }

    private boolean validateThatAgentExistAndCanBeDeleted(String uuid, int totalAgentsToValidate) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
        return validateThatAgentExists(agentInstance) && validateThatAgentCanBeDeleted(agentInstance, totalAgentsToValidate);
    }

    private boolean validateThatAgentCanBeDeleted(AgentInstance agentInstance, int totalAgentToValidate) {
        if (!agentInstance.canBeDeleted()) {
            throw new UnprocessableEntityException(getFailedToDeleteMessage(totalAgentToValidate));
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

    private String getFailedToDeleteMessage(int numOfAgents) {
        if (numOfAgents == 1) {
            return "Failed to delete an agent, as it is not in a disabled state or is still building.";
        } else {
            return "Could not delete any agents, as one or more agents might not be disabled or are still building.";
        }
    }

    boolean validateAnyOperationPerformedOnAgent(String hostname, String environments, String resources, TriState state) {
        boolean anyOperationPerformed = (resources != null || environments != null || hostname != null || isTriStateSet(state));
        if (!anyOperationPerformed) {
            throw new BadRequestException("Bad Request. No operation is specified in the request to be performed on agent.");
        }
        return true;
    }

    private boolean isTriStateSet(TriState state) {
        return state.isTrue() || state.isFalse();
    }

    boolean isAnyOperationPerformedOnBulkAgents(List<String> resourcesToAdd, List<String> resourcesToRemove,
                                                List<String> envsToAdd, List<String> envsToRemove,
                                                TriState state) {
        boolean anyOperationPerformed
                = isNotEmpty(resourcesToAdd)
                || isNotEmpty(resourcesToRemove)
                || isNotEmpty(envsToAdd)
                || isNotEmpty(envsToRemove)
                || isTriStateSet(state);
        if (!anyOperationPerformed) {
            throw new BadRequestException("Bad Request. No operation is specified in the request to be performed on agents.");
        }
        return true;
    }

    private List<Agent> getAgentsToAddEnvToOrRemoveEnvFrom(EnvironmentConfig envConfig, List<String> uuids) {
        List<String> associatedUUIDs = toUUIDList(envConfig.getAgents());
        String envName = envConfig.name().toString();

        List<Agent> removeEnvFromAgents = getAgentsFromDBToRemoveEnvFrom(uuids, associatedUUIDs, envName);
        List<Agent> addEnvToAgents = getAgentsFromDBToAddEnvTo(uuids, associatedUUIDs, envName);

        return union(removeEnvFromAgents, addEnvToAgents);
    }

    private List<Agent> getAgentsToAddEnvToOrRemoveEnvFrom(String envName, List<String> agentUUIDsToAssociate, List<String> agentUUIDsToRemove) {
        List<Agent> removeEnvFromAgents = getAgentsFromDBToRemoveEnvFrom(Collections.emptyList(), agentUUIDsToRemove, envName);
        List<Agent> addEnvToAgents = getAgentsFromDBToAddEnvTo(agentUUIDsToAssociate, Collections.emptyList(), envName);

        return union(removeEnvFromAgents, addEnvToAgents);
    }

    void updateIdsAndGenerateCookiesForPendingAgents(List<Agent> agents, TriState state) {
        if (isTriStateSet(state)) {
            agents.stream()
                    .filter(agent -> findAgent(agent.getUuid()).getStatus().getConfigStatus() == Pending)
                    .forEach(this::updateIdAndGenerateCookieForPendingAgent);
        }
    }

    private void updateIdAndGenerateCookieForPendingAgent(Agent pendingAgent) {
        agentDao.updateAgentIdFromDBIfAgentDoesNotHaveAnIdAndAgentExistInDB(pendingAgent);
        generateAndAddCookie(pendingAgent);
    }
}
