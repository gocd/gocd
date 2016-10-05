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

package com.thoughtworks.go.server.service;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.update.*;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.BaseUrlChangeListener;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.persistence.PipelineRepository;
import com.thoughtworks.go.server.security.GoAcl;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.SystemTimeClock;
import org.apache.log4j.Logger;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jdom.input.JDOMParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.util.*;

import static com.thoughtworks.go.config.validation.GoConfigValidity.invalid;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

@Service
public class GoConfigService implements Initializer, CruiseConfigProvider {
    private GoConfigDao goConfigDao;
    private PipelineRepository pipelineRepository;
    private GoConfigMigration upgrader;
    private GoCache goCache;
    private ConfigRepository configRepository;
    private ConfigCache configCache;

    private Cloner cloner = new Cloner();
    private Clock clock = new SystemTimeClock();

    private static final Logger LOGGER = Logger.getLogger(GoConfigService.class);

    public static final String INVALID_CRUISE_CONFIG_XML = "Invalid Configuration";
    private final ConfigElementImplementationRegistry registry;
    private InstanceFactory instanceFactory;
    private final CachedGoPartials cachedGoPartials;

    @Autowired
    public GoConfigService(GoConfigDao goConfigDao, PipelineRepository pipelineRepository, GoConfigMigration upgrader, GoCache goCache,
                           ConfigRepository configRepository, ConfigCache configCache, ConfigElementImplementationRegistry registry,
                           InstanceFactory instanceFactory, CachedGoPartials cachedGoPartials) {
        this.goConfigDao = goConfigDao;
        this.pipelineRepository = pipelineRepository;
        this.goCache = goCache;
        this.configRepository = configRepository;
        this.configCache = configCache;
        this.registry = registry;
        this.upgrader = upgrader;
        this.instanceFactory = instanceFactory;
        this.cachedGoPartials = cachedGoPartials;
    }

    //for testing
    public GoConfigService(GoConfigDao goConfigDao, PipelineRepository pipelineRepository, Clock clock, GoConfigMigration upgrader, GoCache goCache,
                           ConfigRepository configRepository, ConfigElementImplementationRegistry registry,
                           InstanceFactory instanceFactory, CachedGoPartials cachedGoPartials) {
        this(goConfigDao, pipelineRepository, upgrader, goCache, configRepository, new ConfigCache(), registry, instanceFactory, cachedGoPartials);
        this.clock = clock;
    }

    @Override
    public void initialize() {
        this.goConfigDao.load();
        register(new BaseUrlChangeListener(serverConfig().getSiteUrl(), serverConfig().getSecureSiteUrl(), goCache));
        File dir = artifactsDir();
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                bomb("Unable to create artifacts directory at " + dir.getAbsolutePath());
            }
        }
        if (!dir.canRead()) {
            bomb("Cruise does not have read permission on " + dir.getAbsolutePath());
        }
        if (!dir.canWrite()) {
            bomb("Cruise does not have write permission on " + dir.getAbsolutePath());
        }
    }

    public ConfigForEdit<PipelineConfig> loadForEdit(String pipelineName, Username username, HttpLocalizedOperationResult result) {
        if (!canEditPipeline(pipelineName, username, result)) {
            return null;
        }
        GoConfigHolder configHolder = getConfigHolder();
        configHolder = cloner.deepClone(configHolder);
        PipelineConfig config = configHolder.configForEdit.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        return new ConfigForEdit<>(config, configHolder);
    }

    private boolean canEditPipeline(String pipelineName, Username username, LocalizedOperationResult result) {
        return canEditPipeline(pipelineName, username, result, findGroupNameByPipeline(new CaseInsensitiveString(pipelineName)));
    }

    public boolean canEditPipeline(String pipelineName, Username username, LocalizedOperationResult result, String groupName) {
        if (!doesPipelineExist(pipelineName, result)) {
            return false;
        }
        if (!isUserAdminOfGroup(username.getUsername(), groupName)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", pipelineName), HealthStateType.unauthorisedForPipeline(pipelineName));
            return false;
        }
        return true;
    }

    private boolean doesPipelineExist(String pipelineName, LocalizedOperationResult result) {
        if (!getCurrentConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound(LocalizedMessage.string("PIPELINE_NOT_FOUND", pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return false;
        }
        return true;
    }

    public Agents agents() {
        return getCurrentConfig().agents();
    }

    @Deprecated()
    public CruiseConfig currentCruiseConfig() {
        return getCurrentConfig();
    }

    public EnvironmentsConfig getEnvironments() {
        return cruiseConfig().getEnvironments();
    }

    @Deprecated()
    public CruiseConfig getCurrentConfig() {
        return cruiseConfig();
    }

    @Deprecated()
    public CruiseConfig getConfigForEditing() {
        return goConfigDao.loadForEditing();
    }

    @Deprecated()
    public CruiseConfig getMergedConfigForEditing() {
        return goConfigDao.loadMergedForEditing();
    }

    CruiseConfig cruiseConfig() {
        return goConfigDao.load();
    }

    public AgentConfig agentByUuid(String uuid) {
        return agents().getAgentByUuid(uuid);
    }

    public StageConfig stageConfigNamed(String pipelineName, String stageName) {
        return getCurrentConfig().stageConfigByName(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    public boolean hasPipelineNamed(final CaseInsensitiveString pipelineName) {
        return getCurrentConfig().hasPipelineNamed(pipelineName);
    }

    public PipelineConfig pipelineConfigNamed(final CaseInsensitiveString name) {
        return getCurrentConfig().pipelineConfigByName(name);
    }

    public boolean stageHasTests(String pipelineName, String stageName) {
        return stageConfigNamed(pipelineName, stageName).hasTests();
    }

    public boolean stageExists(String pipelineName, String stageName) {
        try {
            stageConfigNamed(pipelineName, stageName);
            return true;
        } catch (StageNotFoundException e) {
            return false;
        }
    }

    public String fileLocation() {
        return goConfigDao.fileLocation();
    }

    public File artifactsDir() {
        ServerConfig serverConfig = serverConfig();
        String s = serverConfig.artifactsDir();
        return new File(s);
    }

    public boolean hasStageConfigNamed(String pipelineName, String stageName) {
        return getCurrentConfig().hasStageConfigNamed(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName), true);
    }

    public ConfigSaveState updateConfig(UpdateConfigCommand command) {
        return goConfigDao.updateConfig(command);
    }

    public void updateConfig(EntityConfigUpdateCommand command, Username currentUser) {
        goConfigDao.updateConfig(command, currentUser);
    }

    public long getUnresponsiveJobTerminationThreshold(JobIdentifier identifier) {
        JobConfig jobConfig = getJob(identifier);
        if (jobConfig == null) {
            return toMillis(Long.parseLong(serverConfig().getJobTimeout()));
        }
        String timeout = jobConfig.getTimeout();
        return timeout != null ? toMillis(Long.parseLong(timeout)) : toMillis(Long.parseLong(serverConfig().getJobTimeout()));
    }

    private JobConfig getJob(JobIdentifier identifier) {
        JobConfig jobConfig = null;
        try {
            jobConfig = cruiseConfig().findJob(identifier.getPipelineName(), identifier.getStageName(), identifier.getBuildName());
        } catch (Exception ignored) {
        }
        return jobConfig;
    }

    private long toMillis(final long minutes) {
        return minutes * 60 * 1000;
    }

    public boolean canCancelJobIfHung(JobIdentifier jobIdentifier) {
        JobConfig jobConfig = getJob(jobIdentifier);
        if (jobConfig == null) {
            return false;
        }
        String timeout = jobConfig.getTimeout();
        if ("0".equals(timeout)) {
            return false;
        }
        if (timeout == null && !"0".equals(serverConfig().getJobTimeout())) {
            return true;
        }
        return timeout != null && !"0".equals(timeout);
    }

    public ConfigUpdateResponse updateConfigFromUI(final UpdateConfigFromUI command, final String md5, Username username, final LocalizedOperationResult result) {
        UiBasedConfigUpdateCommand updateCommand = new UiBasedConfigUpdateCommand(md5, command, result, cachedGoPartials);
        UpdatedNodeSubjectResolver updatedConfigResolver = new UpdatedNodeSubjectResolver();
        try {
            ConfigSaveState configSaveState = updateConfig(updateCommand);
            return latestUpdateResponse(command, updateCommand, updatedConfigResolver, clonedConfigForEdit(), configSaveState);
        } catch (ConfigFileHasChangedException e) {
            CruiseConfig updatedConfig = handleMergeException(md5, updateCommand);
            result.conflict(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getMessage()));
            return latestUpdateResponse(command, updateCommand, new OldNodeSubjectResolver(), updatedConfig, null);
        } catch (ConfigUpdateCheckFailedException e) {
            //result is already set
        } catch (Exception e) {
            ConfigMergeException mergeException = ExceptionUtils.getCause(e, ConfigMergeException.class);
            ConfigMergePostValidationException mergePostValidationException = ExceptionUtils.getCause(e, ConfigMergePostValidationException.class);
            if (mergeException != null || mergePostValidationException != null) {
                CruiseConfig updatedConfig = handleMergeException(md5, updateCommand);
                result.conflict(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getMessage()));
                return latestUpdateResponse(command, updateCommand, new OldNodeSubjectResolver(), updatedConfig, null);
            }
            GoConfigInvalidException ex = ExceptionUtils.getCause(e, GoConfigInvalidException.class);
            if (ex != null) {
                CruiseConfig badConfig = ex.getCruiseConfig();
                setMD5(md5, badConfig);
                Validatable node = updatedConfigResolver.getNode(command, updateCommand.cruiseConfig());
                BasicCruiseConfig.copyErrors(command.updatedNode(badConfig), node);
                result.badRequest(LocalizedMessage.string("SAVE_FAILED"));
                return new ConfigUpdateResponse(badConfig, node, subjectFromNode(command, updatedConfigResolver, node), updateCommand, null);
            } else {
                result.badRequest(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", e.getMessage()));
            }
        }

        CruiseConfig newConfigSinceNoOtherConfigExists = clonedConfigForEdit();
        setMD5(md5, newConfigSinceNoOtherConfigExists);
        return latestUpdateResponse(command, updateCommand, new OldNodeSubjectResolver(), newConfigSinceNoOtherConfigExists, null);
    }

    private CruiseConfig handleMergeException(String md5, UiBasedConfigUpdateCommand updateCommand) {
        CruiseConfig updatedConfig;
        try {
            updateCommand.update(clonedConfigForEdit());
            updatedConfig = updateCommand.cruiseConfig();
        } catch (Exception oops) {
            //Ignore this. We are trying to retain the user's input. However, if things have changed so massively that we cannot apply this update we cannot do anything.
            //But hey, at least we tried...
            updatedConfig = clonedConfigForEdit();
        }
        setMD5(md5, updatedConfig);
        return updatedConfig;
    }

    private void setMD5(String md5, CruiseConfig badConfig) {
        try {
            MagicalGoConfigXmlLoader.setMd5(badConfig, md5);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Ignore
        }
    }

    private Validatable subjectFromNode(UpdateConfigFromUI command, NodeSubjectResolver updatedConfigResolver, Validatable node) {
        return node != null ? updatedConfigResolver.getSubject(command, node) : null;
    }

    private ConfigUpdateResponse latestUpdateResponse(UpdateConfigFromUI command, UiBasedConfigUpdateCommand updateCommand, final NodeSubjectResolver nodeSubResolver, final CruiseConfig config,
                                                      ConfigSaveState configSaveState) {
        Validatable node = null;
        Validatable subject = null;
        try {
            node = nodeSubResolver.getNode(command, config);
            subject = subjectFromNode(command, nodeSubResolver, node);
        } catch (Exception e) {
            //ignore, let node be null, will be handled by assert_loaded
        }
        return new ConfigUpdateResponse(config, node, subject, updateCommand, configSaveState);
    }

    public ConfigSaveState updateServerConfig(final MailHost mailHost, final LdapConfig ldapConfig, final PasswordFileConfig passwordFileConfig, final boolean shouldAllowAutoLogin,
                                              final String md5, final String artifactsDir, final Double purgeStart, final Double purgeUpto, final String jobTimeout,
                                              final String siteUrl, final String secureSiteUrl, final String taskRepositoryLocation) {
        final List<ConfigSaveState> result = new ArrayList<>();
        result.add(updateConfig(
                new GoConfigDao.NoOverwriteCompositeConfigCommand(md5,
                        goConfigDao.mailHostUpdater(mailHost),
                        securityUpdater(ldapConfig, passwordFileConfig, shouldAllowAutoLogin),
                        serverConfigUpdater(artifactsDir, purgeStart, purgeUpto, jobTimeout, siteUrl, secureSiteUrl, taskRepositoryLocation))));
        //should not reach here with empty result
        return result.get(0);
    }

    private UpdateConfigCommand serverConfigUpdater(final String artifactsDir, final Double purgeStart, final Double purgeUpto, final String jobTimeout, final String siteUrl,
                                                    final String secureSiteUrl, final String taskRepositoryLocation) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                ServerConfig server = cruiseConfig.server();
                server.setArtifactsDir(artifactsDir);
                server.setPurgeLimits(purgeStart, purgeUpto);
                server.setJobTimeout(jobTimeout);
                server.setSiteUrl(siteUrl);
                server.setSecureSiteUrl(secureSiteUrl);
                server.setCommandRepositoryLocation(taskRepositoryLocation);
                return cruiseConfig;
            }
        };
    }

    private UpdateConfigCommand securityUpdater(final LdapConfig ldapConfig, final PasswordFileConfig passwordFileConfig, final boolean shouldAllowAutoLogin) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                SecurityConfig securityConfig = cruiseConfig.server().security();
                securityConfig.modifyLdap(ldapConfig);
                securityConfig.modifyPasswordFile(passwordFileConfig);
                securityConfig.modifyAllowOnlyKnownUsers(!shouldAllowAutoLogin);
                return cruiseConfig;
            }
        };
    }

    public void addEnvironment(BasicEnvironmentConfig environmentConfig) {
        goConfigDao.addEnvironment(environmentConfig);
    }

    public void addPipeline(PipelineConfig pipeline, String groupName) {
        goConfigDao.addPipeline(pipeline, groupName);
    }

    public void register(ConfigChangedListener listener) {
        goConfigDao.registerListener(listener);
    }

    GoAcl readAclBy(String pipelineName, String stageName) {
        PipelineConfig pipelineConfig = pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(stageName));
        AdminsConfig adminsConfig = stageConfig.getApproval().getAuthConfig();
        List<CaseInsensitiveString> users = getAuthorizedUsers(adminsConfig);
        return new GoAcl(users);
    }

    private List<CaseInsensitiveString> getAuthorizedUsers(AdminsConfig authorizedAdmins) {
        ArrayList<CaseInsensitiveString> users = new ArrayList<>();
        for (Admin admin : authorizedAdmins) {
            if (admin instanceof AdminRole) {
                addRoleUsers(users, admin.getName());
            } else {
                users.add(admin.getName());
            }
        }
        return users;
    }

    private void addRoleUsers(List<CaseInsensitiveString> users, final CaseInsensitiveString roleName) {
        Role role = security().getRoles().findByName(roleName);
        if (role != null) {
            for (RoleUser roleUser : role.getUsers()) {
                users.add(roleUser.getName());
            }
        }
    }

    public GoMailSender getMailSender() {
        return GoSmtpMailSender.createSender(serverConfig().mailHost());
    }

    public List<String> allGroups() {
        List<String> allGroup = new ArrayList<>();
        getCurrentConfig().groups(allGroup);
        return allGroup;
    }

    public PipelineGroups groups() {
        return getCurrentConfig().getGroups();
    }

    public List<Task> tasksForJob(String pipelineName, String stageName, String jobName) {
        return getCurrentConfig().tasksForJob(pipelineName, stageName, jobName);
    }

    public boolean isSmtpEnabled() {
        return currentCruiseConfig().isSmtpEnabled();
    }

    public void accept(PiplineConfigVisitor visitor) {
        getCurrentConfig().accept(visitor);
    }

    public void accept(PipelineGroupVisitor visitor) {
        getCurrentConfig().accept(visitor);
    }

    public String findGroupNameByPipeline(final CaseInsensitiveString pipelineName) {
        return getCurrentConfig().getGroups().findGroupNameByPipeline(pipelineName);
    }

    public void populateAdminModel(Map<String, String> model) {
        model.put("location", fileLocation());
        XmlPartialSaver saver = fileSaver(false);
        model.put("content", saver.asXml());
        model.put("md5", saver.getMd5());
    }

    public MailHost getMailHost() {
        return serverConfig().mailHost();
    }

    public boolean hasAgent(String uuid) {
        return agents().hasAgent(uuid);
    }

    public JobConfigIdentifier translateToActualCase(JobConfigIdentifier identifier) {
        PipelineConfig pipelineConfig = getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString(identifier.getPipelineName()));
        String translatedPipelineName = CaseInsensitiveString.str(pipelineConfig.name());
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(identifier.getStageName()));
        if (stageConfig == null) {
            throw new StageNotFoundException(new CaseInsensitiveString(identifier.getPipelineName()), new CaseInsensitiveString(identifier.getStageName()));
        }
        String translatedStageName = CaseInsensitiveString.str(stageConfig.name());
        JobConfig plan = stageConfig.jobConfigByInstanceName(identifier.getJobName(), true);
        if (plan == null) {
            throw new JobNotFoundException(identifier.getPipelineName(), identifier.getStageName(), identifier.getJobName());
        }
        String translatedJobName = plan.translatedName(identifier.getJobName());
        return new JobConfigIdentifier(translatedPipelineName, translatedStageName, translatedJobName);
    }

    public boolean isAdministrator(String username) {
        return getCurrentConfig().isAdministrator(username);
    }

    public CommentRenderer getCommentRendererFor(String pipelineName) {
        return pipelineConfigNamed(new CaseInsensitiveString(pipelineName)).getCommentRenderer();
    }

    public List<PipelineConfig> getAllPipelineConfigs() {
        return getCurrentConfig().getAllPipelineConfigs();
    }

    /* NOTE: this is called from rails environments controller to build a list of pipelines which user can assign in environment.
       We don't want user to select or unselect any pipeline which is already selected in a remote configuration repository.
     */
    public List<PipelineConfig> getAllLocalPipelineConfigs() {
        return getCurrentConfig().getAllLocalPipelineConfigs(true);
    }

    public List<PipelineConfig> getAllPipelineConfigsForEdit() {
        return getConfigForEditing().getAllPipelineConfigs();
    }

    public String adminEmail() {
        return getCurrentConfig().adminEmail();
    }

    public Set<MaterialConfig> getSchedulableMaterials() {
        return getCurrentConfig().getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos();
    }

    public Stage scheduleStage(String pipelineName, String stageName, SchedulingContext context) {
        PipelineConfig pipelineConfig = getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        return instanceFactory.createStageInstance(pipelineConfig, new CaseInsensitiveString(stageName), context, getCurrentConfig().getMd5(), clock);
    }

    public MaterialConfig findMaterialWithName(final CaseInsensitiveString pipelineName, final CaseInsensitiveString materialName) {
        MaterialConfigs materialConfigs = materialConfigsFor(pipelineName);
        for (MaterialConfig materialConfig : materialConfigs) {
            if (materialName.equals(materialConfig.getName())) {
                return materialConfig;
            }
        }
        LOGGER.error("material [" + materialName + "] not found in pipeline [" + pipelineName + "]");
        return null;
    }

    public MaterialConfig findMaterial(final CaseInsensitiveString pipeline, String pipelineUniqueFingerprint) {
        MaterialConfigs materialConfigs = materialConfigsFor(pipeline);
        for (MaterialConfig materialConfig : materialConfigs) {
            if (pipelineUniqueFingerprint.equals(materialConfig.getPipelineUniqueFingerprint())) {
                return materialConfig;
            }
        }
        LOGGER.error("material with fingerprint [" + pipelineUniqueFingerprint + "] not found in pipeline [" + pipeline + "]");
        return null;
    }

    public MaterialConfigs materialConfigsFor(final CaseInsensitiveString name) {
        return pipelineConfigNamed(name).materialConfigs();
    }

    public MaterialConfig materialForPipelineWithFingerprint(String pipelineName, String fingerprint) {
        for (MaterialConfig materialConfig : pipelineConfigNamed(new CaseInsensitiveString(pipelineName)).materialConfigs()) {
            if (materialConfig.getFingerprint().equals(fingerprint)) {
                return materialConfig;
            }
        }
        throw new RuntimeException(format("Pipeline [%s] does not have a material with fingerprint [%s]", pipelineName, fingerprint));
    }

    public boolean isLockable(String pipelineName) {
        return getCurrentConfig().isPipelineLocked(pipelineName);
    }


    public GoConfigDao.CompositeConfigCommand modifyRolesCommand(List<String> users, List<TriStateSelection> roleSelections) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        for (String user : users) {
            for (TriStateSelection roleSelection : roleSelections) {
                command.addCommand(new GoConfigDao.ModifyRoleCommand(user, roleSelection));
            }
        }
        return command;
    }

    public UpdateConfigCommand modifyAdminPrivilegesCommand(List<String> users, TriStateSelection adminPrivilege) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        for (String user : users) {
            command.addCommand(new GoConfigDao.ModifyAdminPrivilegeCommand(user, adminPrivilege));
        }
        return command;
    }


    public void modifyEnvironments(List<AgentInstance> agents, List<TriStateSelection> selections) {
        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
        for (AgentInstance agentInstance : agents) {
            String uuid = agentInstance.getUuid();
            if (hasAgent(uuid)) {
                for (TriStateSelection selection : selections) {
                    command.addCommand(new ModifyEnvironmentCommand(uuid, selection.getValue(), selection.getAction()));
                }
            }
        }
        updateConfig(command);
    }

    public Set<Resource> getAllResources() {
        return getCurrentConfig().getAllResources();
    }

    public List<String> getResourceList() {
        ArrayList<String> resources = new ArrayList<>();
        for (Resource res : getCurrentConfig().getAllResources()) {
            resources.add(res.getName());
        }
        return resources;
    }

    public List<CaseInsensitiveString> pipelines(String group) {
        PipelineConfigs configs = getCurrentConfig().pipelines(group);
        List<CaseInsensitiveString> pipelines = new ArrayList<>();
        for (PipelineConfig config : configs) {
            pipelines.add(config.name());
        }
        return pipelines;
    }

    public PipelineConfigs getAllPipelinesInGroup(String group) {
        return getCurrentConfig().pipelines(group);
    }

    public GoConfigValidity checkConfigFileValid() {
        return goConfigDao.checkConfigFileValid();
    }

    public boolean isSecurityEnabled() {
        return getCurrentConfig().isSecurityEnabled();
    }

    public SecurityConfig security() {
        return serverConfig().security();
    }

    public ServerConfig serverConfig() {
        return getCurrentConfig().server();
    }

    public boolean hasNextStage(String pipelineName, String lastStageName) {
        return getCurrentConfig().hasNextStage(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(lastStageName));
    }

    public boolean hasPreviousStage(String pipelineName, String lastStageName) {
        return getCurrentConfig().hasPreviousStage(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(lastStageName));
    }

    public boolean isFirstStage(String pipelineName, String stageName) {
        boolean hasPreviousStage = getCurrentConfig().hasPreviousStage(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
        return !hasPreviousStage;
    }

    public boolean requiresApproval(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        return getCurrentConfig().requiresApproval(pipelineName, stageName);
    }

    public StageConfig findFirstStageOfPipeline(final CaseInsensitiveString pipelineName) {
        return getCurrentConfig().pipelineConfigByName(pipelineName).first();
    }

    public StageConfig nextStage(String pipelineName, String lastStageName) {
        return getCurrentConfig().nextStage(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(lastStageName));
    }

    public StageConfig previousStage(String pipelineName, String lastStageName) {
        return getCurrentConfig().previousStage(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(lastStageName));
    }

    public Tabs getCustomizedTabs(String pipelineName, String stageName, String buildName) {
        try {
            JobConfig plan = getCurrentConfig().jobConfigByName(pipelineName, stageName, buildName, false);
            return plan.getTabs();
        } catch (Exception e) {
            return new Tabs();
        }
    }

    public XmlPartialSaver groupSaver(String groupName) {
        return new XmlPartialPipelineGroupSaver(groupName);
    }

    public XmlPartialSaver fileSaver(final boolean shouldUpgrade) {
        return new XmlPartialFileSaver(shouldUpgrade, registry);
    }

    public String configFileMd5() {
        return goConfigDao.md5OfConfigFile();
    }

    public List<PipelineConfig> downstreamPipelinesOf(String pipelineName) {
        List<PipelineConfig> dependencies = new ArrayList<>();
        for (PipelineConfig config : getAllPipelineConfigs()) {
            if (config.dependsOn(new CaseInsensitiveString(pipelineName))) {
                dependencies.add(config);
            }
        }
        return dependencies;
    }

    public boolean hasVariableInScope(String pipelineName, String variableName) {
        return cruiseConfig().hasVariableInScope(pipelineName, variableName);
    }

    public EnvironmentVariablesConfig variablesFor(String pipelineName) {
        return cruiseConfig().variablesFor(pipelineName);
    }

    public PipelineConfigDependencyGraph upstreamDependencyGraphOf(String pipelineName) {
        CruiseConfig currentConfig = getCurrentConfig();
        return upstreamDependencyGraphOf(pipelineName, currentConfig);
    }

    public PipelineConfigDependencyGraph upstreamDependencyGraphOf(String pipelineName, CruiseConfig currentConfig) {
        return findUpstream(currentConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName)));
    }

    private PipelineConfigDependencyGraph findUpstream(PipelineConfig currentPipeline) {
        List<PipelineConfigDependencyGraph> graphs = new ArrayList<>();
        for (CaseInsensitiveString name : currentPipeline.upstreamPipelines()) {
            PipelineConfig pipelineConfig = getCurrentConfig().pipelineConfigByName(name);
            graphs.add(findUpstream(pipelineConfig));
        }
        return new PipelineConfigDependencyGraph(currentPipeline, graphs.toArray(new PipelineConfigDependencyGraph[0]));
    }

    public PipelineSelections getSelectedPipelines(String id, Long userId) {
        PipelineSelections pipelineSelections = getPersistedPipelineSelections(id, userId);
        if (pipelineSelections == null) {
            pipelineSelections = PipelineSelections.ALL;
        }
        return pipelineSelections;
    }

    public long persistSelectedPipelines(String id, Long userId, List<String> selectedPipelines, boolean isBlacklist) {
        PipelineSelections pipelineSelections = findOrCreateCurrentPipelineSelectionsFor(id, userId);

        if (isBlacklist) {
            List<String> unselectedPipelines = invertSelections(selectedPipelines);
            pipelineSelections.update(unselectedPipelines, clock.currentTime(), userId, isBlacklist);
        } else {
            pipelineSelections.update(selectedPipelines, clock.currentTime(), userId, isBlacklist);
        }

        return pipelineRepository.saveSelectedPipelines(pipelineSelections);
    }

    private PipelineSelections findOrCreateCurrentPipelineSelectionsFor(String id, Long userId) {
        PipelineSelections pipelineSelections = isSecurityEnabled() ? pipelineRepository.findPipelineSelectionsByUserId(userId) : pipelineRepository.findPipelineSelectionsById(id);
        if (pipelineSelections == null) {
            pipelineSelections = new PipelineSelections(new ArrayList<String>(), clock.currentTime(), userId, true);
        }
        return pipelineSelections;
    }

    private List<String> invertSelections(List<String> selectedPipelines) {
        List<String> unselectedPipelines = new ArrayList<>();
        List<PipelineConfig> pipelineConfigList = cruiseConfig().getAllPipelineConfigs();
        for (PipelineConfig pipelineConfig : pipelineConfigList) {
            String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
            if (!selectedPipelines.contains(pipelineName)) {
                unselectedPipelines.add(pipelineName);
            }
        }
        return unselectedPipelines;
    }

    private PipelineSelections getPersistedPipelineSelections(String id, Long userId) {
        PipelineSelections pipelineSelections = null;
        if (isSecurityEnabled()) {
            pipelineSelections = pipelineRepository.findPipelineSelectionsByUserId(userId);
        }
        if (pipelineSelections == null) {
            pipelineSelections = pipelineRepository.findPipelineSelectionsById(id);
        }
        return pipelineSelections;
    }

    public List<Role> rolesForUser(final CaseInsensitiveString user) {
        return security().getRoles().memberRoles(new AdminUser(user));
    }

    public boolean isGroupAdministrator(final CaseInsensitiveString userName) {
        return getCurrentConfig().isGroupAdministrator(userName);
    }

    public boolean isGroupAdministrator(final Username userName) {
        return getCurrentConfig().isGroupAdministrator(userName.getUsername());
    }

    public boolean hasEnvironmentNamed(final CaseInsensitiveString environmentName) {
        return getCurrentConfig().getEnvironments().hasEnvironmentNamed(environmentName);
    }

    public boolean isOnlyKnownUserAllowedToLogin() {
        return serverConfig().security().isAllowOnlyKnownUsersToLogin();
    }

    public boolean isLdapConfigured() {
        return ldapConfig().isEnabled();
    }

    public boolean isPasswordFileConfigured() {
        return passwordFileConfig().isEnabled();
    }

    public boolean shouldFetchMaterials(String pipelineName, String stageName) {
        return stageConfigNamed(pipelineName, stageName).isFetchMaterials();
    }

    public LdapConfig ldapConfig() {
        return serverConfig().security().ldapConfig();
    }

    private PasswordFileConfig passwordFileConfig() {
        return serverConfig().security().passwordFileConfig();
    }

    public boolean isUserAdminOfGroup(final CaseInsensitiveString userName, String groupName) {
        PipelineConfigs group = null;
        if (groupName != null) {
            group = getCurrentConfig().findGroup(groupName);
        }
        return isUserAdmin(new Username(userName)) || isUserAdminOfGroup(userName, group);
    }

    public boolean isUserAdminOfGroup(final CaseInsensitiveString userName, PipelineConfigs group) {
        return group.isUserAnAdmin(userName, rolesForUser(userName));
    }

    public boolean isUserAdmin(Username username) {
        return isAdministrator(CaseInsensitiveString.str(username.getUsername()));
    }

    private boolean isUserTemplateAdmin(Username username) {
        return getCurrentConfig().getTemplates().canViewAndEditTemplate(username.getUsername());
    }

    public GoConfigRevision getConfigAtVersion(String version) {
        GoConfigRevision goConfigRevision = null;
        try {
            goConfigRevision = configRepository.getRevision(version);
        } catch (Exception e) {
            LOGGER.info("[Go Config Service] Could not fetch cruise config xml at version=" + version, e);
        }
        return goConfigRevision;
    }

    public List<PipelineConfig> pipelinesForFetchArtifacts(String pipelineName) {
        return currentCruiseConfig().pipelinesForFetchArtifacts(pipelineName);
    }

    private boolean isValidGroup(String groupName, CruiseConfig cruiseConfig, HttpLocalizedOperationResult result) {
        if (!cruiseConfig.hasPipelineGroup(groupName)) {
            result.notFound(LocalizedMessage.string("PIPELINE_GROUP_NOT_FOUND", groupName), HealthStateType.general(HealthStateScope.forGroup(groupName)));
            return false;
        }
        return true;
    }

    private boolean isAdminOfGroup(String toGroupName, Username username, HttpLocalizedOperationResult result) {
        if (!isUserAdminOfGroup(username.getUsername(), toGroupName)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_GROUP", toGroupName), HealthStateType.unauthorised());
            return false;
        }
        return true;
    }

    @Deprecated()
    public GoConfigHolder getConfigHolder() {
        return goConfigDao.loadConfigHolder();
    }

    @Deprecated()
    public CruiseConfig loadCruiseConfigForEdit(Username username, HttpLocalizedOperationResult result) {
        if (!isUserAdmin(username) && !isUserTemplateAdmin(username)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_ADMINISTER"), HealthStateType.unauthorised());
        }
        return clonedConfigForEdit();
    }

    private CruiseConfig clonedConfigForEdit() {
        return cloner.deepClone(getConfigForEditing());
    }

    public ConfigForEdit<PipelineConfigs> loadGroupForEditing(String groupName, Username username, HttpLocalizedOperationResult result) {
        GoConfigHolder configForEdit = cloner.deepClone(getConfigHolder());
        if (!isValidGroup(groupName, configForEdit.configForEdit, result)) {
            return null;
        }

        if (!isAdminOfGroup(groupName, username, result)) {
            return null;
        }
        PipelineConfigs config = cloner.deepClone(configForEdit.configForEdit.findGroup(groupName));
        return new ConfigForEdit<>(config, configForEdit);
    }

    public boolean doesMd5Match(String md5) {
        return configFileMd5().equals(md5);
    }

    public String getServerId() {
        return serverConfig().getServerId();
    }

    public String configChangesFor(String laterMd5, String earlierMd5, LocalizedOperationResult result) {
        try {
            return configRepository.configChangesFor(laterMd5, earlierMd5);
        } catch (IllegalArgumentException e) {
            result.badRequest(LocalizedMessage.string("CONFIG_VERSION_NOT_FOUND"));
        } catch (Exception e) {
            result.internalServerError(LocalizedMessage.string("COULD_NOT_RETRIEVE_CONFIG_DIFF"));
        }
        return null;
    }

    public boolean isAuthorizedToEditTemplate(String templateName, Username username) {
        return isUserAdmin(username) || getCurrentConfig().getTemplates().canUserEditTemplate(templateName, username.getUsername());
    }

    public boolean isAuthorizedToViewAndEditTemplates(Username username) {
        return getCurrentConfig().getTemplates().canViewAndEditTemplate(username.getUsername());
    }

    public void updateUserPipelineSelections(String id, Long userId, CaseInsensitiveString pipelineToAdd) {
        PipelineSelections currentSelections = findOrCreateCurrentPipelineSelectionsFor(id, userId);
        if (!currentSelections.isBlacklist()) {
            currentSelections.addPipelineToSelections(pipelineToAdd);
            pipelineRepository.saveSelectedPipelines(currentSelections);
        }
    }

    public boolean isPipelineEditableViaUI(String pipelineName) {
        PipelineConfig pipelineConfig = this.pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        if(pipelineConfig == null)
            return false;
        return isOriginLocal(pipelineConfig.getOrigin());
    }

    private boolean isOriginLocal(ConfigOrigin origin) {
        // when null we assume that it comes from file or UI
        return origin == null || origin.isLocal();
    }

    public ArrayList<SCM> getSCMs() {
        return cruiseConfig().getSCMs();
    }

    public boolean isAdministrator(CaseInsensitiveString username) {
        return isAdministrator(username.toString());
    }

    public PackageRepository getPackageRepository(String repoId) {
        return cruiseConfig().getPackageRepositories().find(repoId);
    }

    public PackageRepositories getPackageRepositories() {
        return cruiseConfig().getPackageRepositories();
    }

    public abstract class XmlPartialSaver<T> {
        protected final SAXReader reader;
        private final ConfigElementImplementationRegistry registry;

        protected XmlPartialSaver(ConfigElementImplementationRegistry registry) {
            this.registry = registry;
            reader = new SAXReader();
        }

        private String md5;

        protected ConfigSaveState updatePartial(String xmlPartial, final String md5) throws Exception {
            LOGGER.debug("[Config Save] Updating partial");
            org.dom4j.Document document = documentRoot();
            Element root = document.getRootElement();

            Element configElement = ((Element) root.selectSingleNode(getXpath()));
            List nodes = configElement.getParent().content();
            int index = nodes.indexOf(configElement);

            LOGGER.debug("[Config Save] Converting to object");
            Element newConfigElement = reader.read(new StringReader(xmlPartial)).getRootElement();
            nodes.set(index, newConfigElement);

            return saveConfig(document.asXML(), md5);

        }

        protected ConfigSaveState saveConfig(final String xmlString, final String md5) throws Exception {
            LOGGER.debug("[Config Save] Started saving XML");
            final MagicalGoConfigXmlLoader configXmlLoader = new MagicalGoConfigXmlLoader(configCache, registry);

            LOGGER.debug("[Config Save] Updating config");
            final CruiseConfig deserializedConfig = configXmlLoader.deserializeConfig(xmlString);
            ConfigSaveState configSaveState = goConfigDao.updateConfig(new NoOverwriteUpdateConfigCommand() {
                public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                    deserializedConfig.setPartials(cruiseConfig.getPartials());
                    return deserializedConfig;
                }

                public String unmodifiedMd5() {
                    return md5;
                }
            });

            LOGGER.debug("[Config Save] Finished saving XML");
            return configSaveState;
        }

        protected org.dom4j.Document documentRoot() throws Exception {
            CruiseConfig cruiseConfig = goConfigDao.loadForEditing();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new MagicalGoConfigXmlWriter(configCache, registry).write(cruiseConfig, out, true);
            org.dom4j.Document document = reader.read(new StringReader(out.toString()));
            Map<String, String> map = new HashMap<>();
            map.put("go", MagicalGoConfigXmlWriter.XML_NS);
            //TODO: verify this doesn't cache the factory
            DocumentFactory factory = DocumentFactory.getInstance();
            factory.setXPathNamespaceURIs(map);
            return document;
        }

        protected abstract T valid();

        public String asXml() {
            return new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(valid());
        }

        public GoConfigValidity saveXml(String xmlPartial, String expectedMd5) {
            GoConfigValidity hasValidRequest = checkValidity();
            if (!hasValidRequest.isValid()) {
                return hasValidRequest;
            }

            try {
                return GoConfigValidity.valid(updatePartial(xmlPartial, expectedMd5));
            } catch (JDOMParseException jsonException) {
                return GoConfigValidity.invalid(String.format("%s - %s", INVALID_CRUISE_CONFIG_XML, jsonException.getMessage())).fromConflict();
            } catch (ConfigMergePreValidationException e) {
                return invalid(e).mergePreValidationError();
            } catch (Exception e) {
                if (e.getCause() instanceof ConfigMergePostValidationException) {
                    return GoConfigValidity.invalid(e.getCause().getMessage()).mergePostValidationError();
                }
                if (e.getCause() instanceof ConfigMergeException) {
                    return GoConfigValidity.invalid(e.getCause().getMessage()).mergeConflict();
                }
                return GoConfigValidity.invalid(e).fromConflict();
            }
        }

        private GoConfigValidity checkValidity() {
            try {
                valid();
                return GoConfigValidity.valid();
            } catch (Exception e) {
                return GoConfigValidity.invalid(e);
            }
        }

        protected final CruiseConfig configForEditing() {
            CruiseConfig config = getConfigForEditing();
            this.md5 = config.getMd5();
            return config;
        }

        public String getMd5() {
            return md5;
        }

        protected String getXpath() {
            throw new RuntimeException("Must provide xpath or override the default updating");
        }
    }


    private class XmlPartialFileSaver extends XmlPartialSaver<CruiseConfig> {
        private final boolean shouldUpgrade;

        XmlPartialFileSaver(final boolean shouldUpgrade, final ConfigElementImplementationRegistry registry) {
            super(registry);
            this.shouldUpgrade = shouldUpgrade;
        }

        protected ConfigSaveState updatePartial(String xmlFile, final String md5) throws Exception {
            if (shouldUpgrade) {
                xmlFile = upgrader.upgradeIfNecessary(xmlFile);
            }
            return saveConfig(xmlFile, md5);
        }

        public String asXml() {
            return configAsXml(valid());
        }

        protected CruiseConfig valid() {
            return configForEditing();
        }
    }

    public String xml() {
        return configAsXml(getConfigForEditing());
    }

    private String configAsXml(CruiseConfig cruiseConfig) {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            new MagicalGoConfigXmlWriter(configCache, registry).write(cruiseConfig, outStream, true);
            return outStream.toString();
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    private class XmlPartialPipelineGroupSaver extends XmlPartialSaver<Object> {
        private final String groupName;

        public XmlPartialPipelineGroupSaver(String groupName) {
            super(registry);
            this.groupName = groupName;
        }

        protected Object valid() {
            CruiseConfig config = configForEditing();
            PipelineConfigs group = config.findGroup(groupName);
            return group.getCopyForEditing();
        }

        @Override
        protected String getXpath() {
            return String.format("//cruise/pipelines[@group='%s']", groupName);
        }
    }

    // for test
    public void forceNotifyListeners() throws Exception {
        goConfigDao.reloadListeners();
    }

    public ConfigElementImplementationRegistry getRegistry() {
        return registry;
    }

}
