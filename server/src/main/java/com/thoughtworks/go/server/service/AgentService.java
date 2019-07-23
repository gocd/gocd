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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
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
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;
import com.thoughtworks.go.utils.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static com.thoughtworks.go.util.CommaSeparatedString.append;
import static com.thoughtworks.go.util.CommaSeparatedString.remove;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.join;
import static org.springframework.util.CollectionUtils.isEmpty;


@Service
public class AgentService implements DatabaseEntityChangeListener<Agent> {
    private final SystemEnvironment systemEnvironment;
    private final SecurityService securityService;
    private final UuidGenerator uuidGenerator;
    private final ServerHealthService serverHealthService;
    private AgentStatusChangeNotifier agentStatusChangeNotifier;
    private GoConfigService goConfigService;
    private final AgentDao agentDao;

    private AgentInstances agentInstances;

    private Set<AgentChangeListener> listeners = new HashSet<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentService.class);

    @Autowired
    public AgentService(SystemEnvironment systemEnvironment,
                        SecurityService securityService, AgentDao agentDao, UuidGenerator uuidGenerator, ServerHealthService serverHealthService,
                        AgentStatusChangeNotifier agentStatusChangeNotifier, GoConfigService goConfigService) {
        this(systemEnvironment, null, securityService, agentDao, uuidGenerator, serverHealthService,
                agentStatusChangeNotifier, goConfigService);
        this.agentInstances = new AgentInstances(agentStatusChangeNotifier);
    }

    AgentService(SystemEnvironment systemEnvironment, AgentInstances agentInstances,
                 SecurityService securityService, AgentDao agentDao, UuidGenerator uuidGenerator,
                 ServerHealthService serverHealthService, AgentStatusChangeNotifier agentStatusChangeNotifier, GoConfigService goConfigService) {
        this.systemEnvironment = systemEnvironment;
        this.securityService = securityService;
        this.agentInstances = agentInstances;
        this.agentDao = agentDao;
        this.uuidGenerator = uuidGenerator;
        this.serverHealthService = serverHealthService;
        this.agentStatusChangeNotifier = agentStatusChangeNotifier;
        this.goConfigService = goConfigService;
    }

    public void initialize() {
        this.sync();
        agentDao.registerListener(this);
    }

    public void sync() {
        agentInstances.sync(new Agents(agentDao.allAgents()));
    }

    public AgentInstances agentInstances() {
        return agentInstances;
    }

    public Agents agents() {
        return agentInstances.values().stream().map(AgentInstance::getAgent).collect(Collectors.toCollection(Agents::new));
    }

    public Map<AgentInstance, Collection<String>> agentsToEnvNameMap() {
        Map<AgentInstance, Collection<String>> allAgents = new LinkedHashMap<>();

        for (AgentInstance agentInstance : agentInstances.allAgents()) {
            TreeSet<String> sortedEnvSet = new TreeSet<>(new AlphaAsciiComparator());
            sortedEnvSet.addAll(agentInstance.getAgent().getEnvironmentsAsList());
            allAgents.put(agentInstance, sortedEnvSet);
        }
        return allAgents;
    }

    public AgentsViewModel registeredAgents() {
        return toAgentViewModels(agentInstances.findRegisteredAgents());
    }

    private AgentsViewModel toAgentViewModels(AgentInstances agentInstances) {
        return stream(agentInstances.spliterator(), false)
                    .map(this::toAgentViewModel)
                    .collect(Collectors.toCollection(AgentsViewModel::new));
    }

    private AgentViewModel toAgentViewModel(AgentInstance instance) {
        return new AgentViewModel(instance, instance.getAgent().getEnvironmentsAsList());
    }

    public AgentInstances findRegisteredAgents() {
        return agentInstances.findRegisteredAgents();
    }

    private boolean isUnknownAgent(AgentInstance agentInstance, OperationResult result) {
        if (agentInstance.isNullAgent()) {
            String agentNotFoundMessage = format("Agent '%s' not found", agentInstance.getUuid());
            result.notFound("Agent not found.", agentNotFoundMessage, general(GLOBAL));
            return true;
        }
        return false;
    }

    private boolean doesNotHaveOperatePermission(Username username, OperationResult operationResult) {
        if (!securityService.hasOperatePermissionForAgents(username)) {
            String message = "Unauthorized to operate on agent";
            operationResult.forbidden(message, message, general(GLOBAL));
            return true;
        }
        return false;
    }

    public AgentInstance updateAgentAttributes(Username username, HttpOperationResult result, String uuid,
                                               String newHostname, String resources, String environments,
                                               TriState state) {
        if (doesNotHaveOperatePermission(username, result)) {
            return null;
        }

        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        if (isUnknownAgent(agentInstance, result)) {
            return null;
        }

        Agent agent = new Agent(agentInstance.getAgent());

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

        try {
            saveOrUpdate(agent);

            if (agent.hasErrors()) {
                result.unprocessibleEntity("Updating agent failed:", "", general(GLOBAL));
            } else {
                result.ok(format("Updated agent with uuid %s.", agent.getUuid()));
            }
        } catch (Exception e) {
            result.internalServerError("Updating agent failed: " + e.getMessage(), general(GLOBAL));
            return null;
        }

        return AgentInstance.createFromAgent(agent, systemEnvironment, agentStatusChangeNotifier);
    }

    public void bulkUpdateAgentAttributes(Username username, LocalizedOperationResult result, List<String> uuids,
                                          List<String> resourcesToAdd, List<String> resourcesToRemove,
                                          EnvironmentsConfig envsToAdd, List<String> envsToRemove, TriState state) {
        AgentsUpdateValidator validator
                = new AgentsUpdateValidator(agentInstances, username, result, uuids, state, envsToAdd,
                                            envsToRemove, resourcesToAdd, resourcesToRemove, goConfigService);
        try {
            if (validator.canContinue()) {
                validator.validate();

                List<Agent> agents = this.agentDao.agentsByUUIds(uuids);
                if (state.isTrue() || state.isFalse()) {
                    agents.addAll(agentInstances.findPendingAgents(uuids));
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

    private void setResourcesEnvironmentsAndState(Agent agent, List<String> resourcesToAdd, List<String> resourcesToRemove,
                                                  EnvironmentsConfig envsToAdd, List<String> envsToRemove, TriState state) {
        addRemoveEnvsAndResources(agent, envsToAdd, envsToRemove, resourcesToAdd, resourcesToRemove);
        enableDisableAgent(state, agent);
    }

    public void updateAgentsAssociationWithSpecifiedEnv(Username username, EnvironmentConfig envConfig,
                                                        List<String> uuidsToAssociateWithEnv, LocalizedOperationResult result) {
        if (envConfig == null) {
            return;
        }

        List<String> uuidsAssociatedWithEnv = getAssociatedUUIDs(envConfig.getAgents());
        EnvironmentsConfig envsConfig = getEnvironmentsConfigFrom(envConfig);
        AgentsUpdateValidator validator
                = new AgentsUpdateValidator(agentInstances, username, result, uuidsToAssociateWithEnv,
                                            TriState.TRUE, envsConfig, emptyList(), emptyList(), emptyList(), goConfigService);
        try {
            if (validator.canContinue()) {
                validator.validate();

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

    private List<String> getUUIDsToAddEnvTo(List<String> uuidsToAssociateWithEnv, List<String> uuidsAssociatedWithEnv) {
        return uuidsToAssociateWithEnv.stream()
                                      .filter(uuid -> !uuidsAssociatedWithEnv.contains(uuid))
                                      .collect(Collectors.toList());
    }

    private List<String> getUUIDsToRemoveEnvFrom(List<String> uuidsToAssociateWithEnv, List<String> uuidsAssociatedWithEnv) {
        return uuidsAssociatedWithEnv.stream()
                                     .filter(uuid -> !uuidsToAssociateWithEnv.contains(uuid))
                                     .collect(Collectors.toList());
    }

    private List<Agent> getAgentsToAddEnvToList(List<String> uuidsToAddEnvsTo, String env) {
        return uuidsToAddEnvsTo.stream()
                               .map(uuid -> {
                                    Agent agent = agentDao.agentByUuid(uuid);
                                    String envsToSet = append(agent.getEnvironments(), singletonList(env));
                                    agent.setEnvironments(envsToSet);
                                    return agent;
                               })
                               .collect(Collectors.toList());
    }

    private List<Agent> getAgentsToRemoveEnvFromList(List<String> agentsToRemoveEnvFrom, String env) {
        return agentsToRemoveEnvFrom.stream()
                .map(uuid -> {
                    Agent agent = agentDao.agentByUuid(uuid);
                    String envsToSet = remove(agent.getEnvironments(), singletonList(env));
                    agent.setEnvironments(envsToSet);
                    return agent;
                })
                .collect(Collectors.toList());
    }

    private List<String> getAssociatedUUIDs(EnvironmentAgentsConfig asssociatedAgents) {
        List<String> associatedUUIDs = emptyList();

        if (!isEmpty(asssociatedAgents)) {
            associatedUUIDs = asssociatedAgents.stream().map(EnvironmentAgentConfig::getUuid).collect(Collectors.toList());
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

    private void addOnlyThoseEnvsThatAreNotAssociatedWithTheAgentFromConfigRepo(EnvironmentsConfig envsToAdd, Agent agent) {
        if(envsToAdd != null) {
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

    private void addRemoveEnvsAndResources(Agent agent, EnvironmentsConfig envsToAdd, List<String> envsToRemove,
                                           List<String> resourcesToAdd, List<String> resourcesToRemove) {
        addOnlyThoseEnvsThatAreNotAssociatedWithTheAgentFromConfigRepo(envsToAdd, agent);
        agent.removeEnvironments(envsToRemove);

        agent.addResources(resourcesToAdd);
        agent.removeResources(resourcesToRemove);
    }

    private boolean populateAgentInstancesForUUIDs(OperationResult result, List<String> uuids, List<AgentInstance> agents) {
        for (String uuid : uuids) {
            AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
            if (isUnknownAgent(agentInstance, result)) {
                return false;
            }
            agents.add(agentInstance);
        }
        return true;
    }

    public void deleteAgents(Username username, HttpOperationResult result, List<String> uuids) {
        if (doesNotHaveOperatePermission(username, result)) {
            return;
        }

        List<AgentInstance> agents = new ArrayList<>();
        if (!populateAgentInstancesForUUIDs(result, uuids, agents)) {
            return;
        }

        for (AgentInstance agentInstance : agents) {
            if (!agentInstance.canBeDeleted()) {
                result.notAcceptable(format("Failed to delete %s agent(s), as agent(s) might not be disabled or are still building.", agents.size()), general(GLOBAL));
                return;
            }
        }

        try {
            List<Agent> existingFoundAgents = filterAgents(uuids);
            if (existingFoundAgents.size() != uuids.size()) {
                List<String> foundUUIDs = existingFoundAgents.stream().map(Agent::getUuid).collect(Collectors.toList());
                List<String> notFoundUUIDs = uuids.stream().filter(uuid -> !foundUUIDs.contains(uuid)).collect(Collectors.toList());

                if (!notFoundUUIDs.isEmpty()) {
                    bomb("Unable to delete agent; Agent [" + notFoundUUIDs + "] not found.");
                }
            }
            agentDao.bulkSoftDelete(uuids);
            result.ok(format("Deleted %s agent(s).", agents.size()));
        } catch (Exception e) {
            result.internalServerError("Deleting agents failed:" + e.getMessage(), general(GLOBAL));
        }
    }

    private List<Agent> filterAgents(List<String> uuids) {
        return agentInstances.values().stream().filter(agentInstance -> uuids.contains(agentInstance.getUuid()))
                                               .map(AgentInstance::getAgent)
                                               .collect(Collectors.toList());
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

    public Username agentUsername(String uuId, String ipAddress, String hostNameForDisplay) {
        return new Username(format("agent_%s_%s_%s", uuId, ipAddress, hostNameForDisplay));
    }

    public Registration requestRegistration(Username username, AgentRuntimeInfo agentRuntimeInfo) {
        LOGGER.debug("Agent is requesting registration {}", agentRuntimeInfo);

        AgentInstance agentInstance = agentInstances.register(agentRuntimeInfo);
        Registration registration = agentInstance.assignCertification();

        if (agentInstance.isRegistered()) {
            //TODO : Re-look at below flow to see if we need to set the cookie and save back agent into db
            Agent agent = agentInstance.getAgent();
            generateCookieForAgentIfAgentDoesNotHaveACookie(agent);

            saveOrUpdateAgentInstance(agentInstance);
            bombIfAgentHasErrors(agent);
            LOGGER.debug("New Agent approved {}", agentRuntimeInfo);
        }

        return registration;
    }

    private void generateCookieForAgentIfAgentDoesNotHaveACookie(Agent agent) {
        if (agent.getCookie() == null) {
            String cookie = uuidGenerator.randomUuid();
            agent.setCookie(cookie);
        }
    }

    private void bombIfAgentHasErrors(Agent agent) {
        if (agent.hasErrors()) {
            List<ConfigErrors> errors = ErrorCollector.getAllErrors(agent);

            throw new GoConfigInvalidException(null, new AllConfigErrors(errors));
        }
    }

    public void updateAgentApprovalStatus(final String uuid, final Boolean isDenied) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);

        Agent agent = (agentInstance.isRegistered() ? agentInstance.getAgent() : null);
        bombIfNull(agent, "Unable to update agent approval status; Agent [" + uuid + "] not found.");

        agent.setDisabled(isDenied);
        saveOrUpdate(agent);
    }

    private void saveOrUpdateAgentInstance(AgentInstance agentInstance) {
        Agent agent = agentInstance.getAgent();
        if (!agentInstance.isRegistered()) {
            this.updateAgentApprovalStatus(agent.getUuid(), agent.isDisabled());
        } else {
            saveOrUpdate(agent);
        }
    }

    @Deprecated
    public void approve(String uuid) {
        AgentInstance agentInstance = findAgentAndRefreshStatus(uuid);
        boolean hasAgent = hasAgent(agentInstance.getUuid());
        agentInstance.enable();
        if (hasAgent) {
            LOGGER.warn("Registered agent with the same uuid [{}] already approved.", agentInstance);
        } else {
            Agent agent = agentInstance.getAgent();
            generateCookieForAgentIfAgentDoesNotHaveACookie(agent);
            saveOrUpdate(agent);
        }
    }

    public void notifyJobCancelledEvent(String agentId) {
        agentInstances.updateAgentAboutCancelledBuild(agentId, true);
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

    public Agent findAgentObjectByUuid(String uuid) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        if (agentInstance != null && agentInstance.isRegistered()) {
            return agentInstance.getAgent();
        }
        return null;
    }

    public AgentsViewModel filter(List<String> uuids) {
        AgentsViewModel viewModels = new AgentsViewModel();
        for (AgentInstance agentInstance : agentInstances.filter(uuids)) {
            viewModels.add(new AgentViewModel(agentInstance));
        }
        return viewModels;
    }

    public AgentViewModel findAgentViewModel(String uuid) {
        return toAgentViewModel(findAgentAndRefreshStatus(uuid));
    }

    public LinkedMultiValueMap<String, ElasticAgentMetadata> allElasticAgents() {
        return agentInstances.allElasticAgentsGroupedByPluginId();
    }

    public AgentInstance findElasticAgent(String elasticAgentId, String elasticPluginId) {
        return agentInstances.findElasticAgent(elasticAgentId, elasticPluginId);
    }

    public AgentInstances findEnabledAgents() {
        return agentInstances.findEnabledAgents();
    }

    public AgentInstances findDisabledAgents() {
        return agentInstances.findDisabledAgents();
    }

    public void register(Agent agent, String agentAutoRegisterResources, String agentAutoRegisterEnvironments, HttpOperationResult result) {
        generateCookieForAgentIfAgentDoesNotHaveACookie(agent);
        agent.setResources(agentAutoRegisterResources);
        agent.setEnvironments(agentAutoRegisterEnvironments);
        saveOrUpdate(agent);
    }

    public boolean hasAgent(String uuid) {
        AgentInstance agentInstance = agentInstances.findAgent(uuid);
        return !agentInstance.isNullAgent() && agentInstance.isRegistered();
    }

    public Agent agentByUuid(String agentUuid) {
        Agent agent = agentInstances.findAgent(agentUuid).getAgent();
        if (agent == null) {
            agent = NullAgent.createNullAgent();
        }
        return agent;
    }

    public List<String> allAgentUuids() {
        return agentInstances.values().stream().filter(AgentInstance::isRegistered)
                .map(AgentInstance::getUuid)
                .collect(Collectors.toList());
    }

    public void disableAgents(List<String> uuids) {
        agentDao.changeDisabled(uuids, true);
    }

    public void disableAgents(String... uuids) {
        agentDao.changeDisabled(Arrays.asList(uuids), true);
    }

    public void saveOrUpdate(Agent agent) {
        agent.validate(null);
        if (!agent.hasErrors()) {
            agentDao.saveOrUpdate(agent);
        }
    }

    public Set<ResourceConfig> getAllResources() {
        Set<ResourceConfig> resourceConfigSet = new HashSet<>();
        agents().forEach(agent -> resourceConfigSet.addAll(agent.getResources()));
        return resourceConfigSet;
    }

    public List<String> getResourceList() {
        return getAllResources().stream().map(ResourceConfig::getName).collect(Collectors.toList());
    }

    @Override
    public void bulkEntitiesChanged(List<Agent> agents) {
        agents.forEach(this::entityChanged);
    }

    @Override
    public void bulkEntitiesDeleted(List<String> deletedUuids) {
        deletedUuids.forEach(this::entityDeleted);
    }

    private void entityDeleted(String uuid) {
        notifyAgentDeleteListener(uuid);
        this.agentInstances.removeAgent(uuid);
    }

    @Override
    public void entityChanged(Agent newAgent) {
        AgentInstance oldAgentInstance = agentInstances.findAgent(newAgent.getUuid());

        // this is the case when agent is created in DB newly and it has not been cached yet.
        if (oldAgentInstance instanceof NullAgentInstance) {
            oldAgentInstance = AgentInstance.createFromAgent(newAgent, new SystemEnvironment(), agentStatusChangeNotifier);
            this.agentInstances.add(oldAgentInstance);
        } else {
            Agent oldAgent = oldAgentInstance.getAgent();
            notifyAgentChangeListener(oldAgent, newAgent);
            oldAgentInstance.syncConfig(newAgent);
        }
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

    public void registerAgentChangeListeners(AgentChangeListener listener) {
        listeners.add(listener);
    }

    public void validate(Agent agent) {
        agent.validate(null);
    }

    private EnvironmentsConfig getEnvironmentsConfigFrom(EnvironmentConfig envConfig) {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        envsConfig.add(envConfig);
        return envsConfig;
    }
}
