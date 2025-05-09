/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.exceptions.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.config.update.PipelineConfigCommand;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.BaseUrlChangeListener;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.presentation.ConfigForEdit;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.security.GoAcl;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.Pair;
import com.thoughtworks.go.util.SystemTimeClock;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jdom2.input.JDOMParseException;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.util.*;

import static com.thoughtworks.go.config.validation.GoConfigValidity.*;
import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEditPipeline;
import static com.thoughtworks.go.serverhealth.HealthStateScope.forPipeline;
import static com.thoughtworks.go.serverhealth.HealthStateType.*;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

@Service
public class GoConfigService implements Initializer, CruiseConfigProvider {
    public static final String INVALID_CRUISE_CONFIG_XML = "Invalid Configuration";
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigService.class);

    private final ConfigElementImplementationRegistry registry;
    private final CachedGoPartials cachedGoPartials;
    private final GoConfigDao goConfigDao;
    private final GoConfigMigration upgrader;
    private final GoCache goCache;
    private final ConfigRepository configRepository;
    private final ConfigCache configCache;
    private final GoConfigCloner cloner = new GoConfigCloner();
    private Clock clock = new SystemTimeClock();
    private final InstanceFactory instanceFactory;
    private final MagicalGoConfigXmlLoader xmlLoader;

    @Autowired
    public GoConfigService(GoConfigDao goConfigDao,
                           GoConfigMigration upgrader,
                           GoCache goCache,
                           ConfigRepository configRepository,
                           ConfigCache configCache,
                           ConfigElementImplementationRegistry registry,
                           InstanceFactory instanceFactory,
                           CachedGoPartials cachedGoPartials) {
        this.goConfigDao = goConfigDao;
        this.goCache = goCache;
        this.configRepository = configRepository;
        this.configCache = configCache;
        this.registry = registry;
        this.upgrader = upgrader;
        this.instanceFactory = instanceFactory;
        this.cachedGoPartials = cachedGoPartials;
        this.xmlLoader = new MagicalGoConfigXmlLoader(configCache, registry);
    }

    @TestOnly
    public GoConfigService(GoConfigDao goConfigDao,
                           Clock clock,
                           GoConfigMigration upgrader,
                           GoCache goCache,
                           ConfigRepository configRepository,
                           ConfigElementImplementationRegistry registry,
                           InstanceFactory instanceFactory,
                           CachedGoPartials cachedGoPartials) {
        this(goConfigDao, upgrader, goCache, configRepository, new ConfigCache(), registry, instanceFactory, cachedGoPartials);
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

    @Override
    public void startDaemon() {

    }

    public ConfigForEdit<PipelineConfig> loadForEdit(String pipelineName,
                                                     Username username,
                                                     HttpLocalizedOperationResult result) {
        if (!canEditPipeline(pipelineName, username, result)) {
            return null;
        }
        GoConfigHolder configHolder = getConfigHolder();
        configHolder = cloner.deepClone(configHolder);
        PipelineConfig config = configHolder.configForEdit.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        return new ConfigForEdit<>(config, configHolder);
    }

    boolean canEditPipeline(String pipelineName, Username username, LocalizedOperationResult result) {
        return canEditPipeline(pipelineName, username, result, findGroupNameByPipeline(new CaseInsensitiveString(pipelineName)));
    }

    @TestOnly
    //do not use this method as it is coupled with the origin.
    // ideally these should be two different checks:
    // - pipeline is editable (Not defined in config repository)
    // - whether a user has permissions to edit the pipeline
    public boolean canEditPipeline(String pipelineName, Username username) {
        PipelineConfig pipelineConfig;
        try {
            pipelineConfig = pipelineConfigNamed(new CaseInsensitiveString(pipelineName));
        } catch (RecordNotFoundException e) {
            return false;
        }
        return pipelineConfig != null && pipelineConfig.isLocal() && isUserAdminOfGroup(username.getUsername(), findGroupNameByPipeline(pipelineConfig.name()));
    }

    public boolean canEditPipeline(String pipelineName,
                                   Username username,
                                   LocalizedOperationResult result,
                                   String groupName) {
        if (!doesPipelineExist(pipelineName, result)) {
            return false;
        }
        if (!isUserAdminOfGroup(username.getUsername(), groupName)) {
            result.forbidden(forbiddenToEditPipeline(pipelineName), forbiddenForPipeline(pipelineName));
            return false;
        }
        return true;
    }

    public boolean doesPipelineExist(String pipelineName, LocalizedOperationResult result) {
        if (!getCurrentConfig().hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            result.notFound(EntityType.Pipeline.notFoundMessage(pipelineName), general(forPipeline(pipelineName)));
            return false;
        }
        return true;
    }

    public CruiseConfig currentCruiseConfig() {
        return getCurrentConfig();
    }

    public EnvironmentsConfig getEnvironments() {
        return cruiseConfig().getEnvironments();
    }

    @Override
    public CruiseConfig getCurrentConfig() {
        return cruiseConfig();
    }

    public CruiseConfig getConfigForEditing() {
        return goConfigDao.loadForEditing();
    }

    public CruiseConfig getMergedConfigForEditing() {
        return goConfigDao.loadMergedForEditing();
    }

    CruiseConfig cruiseConfig() {
        return goConfigDao.load();
    }

    public StageConfig stageConfigNamed(String pipelineName, String stageName) {
        return getCurrentConfig().stageConfigByName(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
    }

    public boolean hasPipelineNamed(final CaseInsensitiveString pipelineName) {
        return getCurrentConfig().hasPipelineNamed(pipelineName);
    }

    public PipelineConfig editablePipelineConfigNamed(final String name) {
        return getMergedConfigForEditing().pipelineConfigByName(new CaseInsensitiveString(name));
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

    public void updateConfig(EntityConfigUpdateCommand<?> command, Username currentUser) {
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
        return timeout != null;
    }

    @TestOnly
    public ConfigSaveState updateServerConfig(final MailHost mailHost,
                                              final String md5,
                                              final String artifactsDir,
                                              final Double purgeStart,
                                              final Double purgeUpto,
                                              final String jobTimeout,
                                              final String siteUrl,
                                              final String secureSiteUrl) {
        final List<ConfigSaveState> result = new ArrayList<>();
        result.add(updateConfig(
                new GoConfigDao.NoOverwriteCompositeConfigCommand(md5,
                        goConfigDao.mailHostUpdater(mailHost),
                        serverConfigUpdater(artifactsDir, purgeStart, purgeUpto, jobTimeout, siteUrl, secureSiteUrl))));
        //should not reach here with empty result
        return result.get(0);
    }

    @TestOnly
    private UpdateConfigCommand serverConfigUpdater(final String artifactsDir,
                                                    final Double purgeStart,
                                                    final Double purgeUpto,
                                                    final String jobTimeout,
                                                    final String siteUrl,
                                                    final String secureSiteUrl) {
        return cruiseConfig -> {
            ServerConfig server = cruiseConfig.server();
            server.setArtifactsDir(artifactsDir);
            server.setPurgeLimits(purgeStart, purgeUpto);
            server.setJobTimeout(jobTimeout);
            server.setSiteUrl(siteUrl);
            server.setSecureSiteUrl(secureSiteUrl);
            return cruiseConfig;
        };
    }

    public void addEnvironment(EnvironmentConfig environmentConfig) {
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
        List<CaseInsensitiveString> users = new ArrayList<>();
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

    public void accept(PipelineConfigVisitor visitor) {
        getCurrentConfig().accept(visitor);
    }

    public void accept(PipelineGroupVisitor visitor) {
        getCurrentConfig().accept(visitor);
    }

    public String findGroupNameByPipeline(final CaseInsensitiveString pipelineName) {
        return getCurrentConfig().getGroups().findGroupNameByPipeline(pipelineName);
    }

    public PipelineConfigs findGroupByPipeline(CaseInsensitiveString pipelineName) {
        return getCurrentConfig().getGroups().findGroupByPipeline(pipelineName);
    }

    public MailHost getMailHost() {
        return serverConfig().mailHost();
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
            throw new RecordNotFoundException(format("Job '%s' not found in pipeline '%s' stage '%s'", identifier.getJobName(), identifier.getPipelineName(), identifier.getStageName()));
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
        return cruiseConfig().getAllPipelineConfigs();
    }

    /* NOTE: this is called from rails environments controller to build a list of pipelines which user can assign in environment.
       We don't want user to select or unselect any pipeline which is already selected in a remote configuration repository.
     */
    public List<PipelineConfig> getAllLocalPipelineConfigs() {
        return getCurrentConfig().getAllLocalPipelineConfigs(true);
    }

    public List<PipelineConfig> getAllPipelineConfigsForEditForUser(Username username) {
        List<PipelineConfig> pipelineConfigs = new ArrayList<>();

        List<String> groupsForUser = getConfigForEditing().getGroupsForUser(username.getUsername(), rolesForUser(username.getUsername()));

        for (String groupName : groupsForUser) {
            pipelineConfigs.addAll(getAllPipelinesForEditInGroup(groupName).getPipelines());
        }

        return pipelineConfigs;
    }

    public String adminEmail() {
        return getCurrentConfig().adminEmail();
    }

    public Set<MaterialConfig> getSchedulableMaterials() {
        return getCurrentConfig().getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos();
    }

    public Set<MaterialConfig> getSchedulableSCMMaterials() {
        HashSet<MaterialConfig> scmMaterials = new HashSet<>();

        for (MaterialConfig materialConfig : getSchedulableMaterials()) {
            if (!(materialConfig instanceof DependencyMaterialConfig)) {
                scmMaterials.add(materialConfig);
            }
        }

        return scmMaterials;
    }

    public Set<DependencyMaterialConfig> getSchedulableDependencyMaterials() {
        HashSet<DependencyMaterialConfig> dependencyMaterials = new HashSet<>();

        for (MaterialConfig materialConfig : getSchedulableMaterials()) {
            if (materialConfig instanceof DependencyMaterialConfig) {
                dependencyMaterials.add((DependencyMaterialConfig) materialConfig);
            }
        }

        return dependencyMaterials;
    }

    public Stage scheduleStage(String pipelineName, String stageName, SchedulingContext context) {
        PipelineConfig pipelineConfig = getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        return instanceFactory.createStageInstance(pipelineConfig, new CaseInsensitiveString(stageName), context, getCurrentConfig().getMd5(), clock);
    }

    public MaterialConfig findMaterial(final CaseInsensitiveString pipeline, String pipelineUniqueFingerprint) {
        MaterialConfigs materialConfigs = materialConfigsFor(pipeline);
        for (MaterialConfig materialConfig : materialConfigs) {
            if (pipelineUniqueFingerprint.equals(materialConfig.getPipelineUniqueFingerprint())) {
                return materialConfig;
            }
        }
        LOGGER.error("material with fingerprint [{}] not found in pipeline [{}]", pipelineUniqueFingerprint, pipeline);
        return null;
    }

    public List<CaseInsensitiveString> pipelinesWithMaterial(String fingerprint) {
        List<CaseInsensitiveString> pipelineNames = new ArrayList<>();
        getAllPipelineConfigs().forEach(pipeline -> pipeline.materialConfigs().stream().filter(materialConfig -> materialConfig.getFingerprint().equals(fingerprint)).findFirst().ifPresent(expectedMaterialConfig -> pipelineNames.add(pipeline.name())));
        return pipelineNames;
    }

    public List<PackageDefinition> getPackages() {
        List<PackageDefinition> packages = new ArrayList<>();
        for (PackageRepository repository : this.getCurrentConfig().getPackageRepositories()) {
            packages.addAll(repository.getPackages());
        }
        return packages;
    }

    public PackageDefinition findPackage(String packageId) {
        PackageDefinition packageDefinition = null;
        for (PackageRepository repository : this.getCurrentConfig().getPackageRepositories()) {
            for (PackageDefinition pkg : repository.getPackages()) {
                if (packageId.equals(pkg.getId())) {
                    packageDefinition = pkg;
                    break;
                }
            }
        }
        return packageDefinition;
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
        return getCurrentConfig().isPipelineLockable(pipelineName);
    }

    public boolean isUnlockableWhenFinished(String pipelineName) {
        return getCurrentConfig().isPipelineUnlockableWhenFinished(pipelineName);
    }

    public GoConfigDao.CompositeConfigCommand modifyRolesCommand(List<String> users,
                                                                 List<TriStateSelection> roleSelections) {
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

    public List<String> getResourceList() {
        List<String> resources = new ArrayList<>();
        for (ResourceConfig res : getCurrentConfig().getAllResources()) {
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

    public PipelineConfigs getAllPipelinesForEditInGroup(String group) {
        return getConfigForEditing().pipelines(group);
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

    public XmlPartialSaver<Object> groupSaver(String groupName) {
        return new XmlPartialPipelineGroupSaver(groupName);
    }

    public XmlPartialSaver<CruiseConfig> fileSaver(final boolean shouldUpgrade) {
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

    public boolean shouldFetchMaterials(String pipelineName, String stageName) {
        return stageConfigNamed(pipelineName, stageName).isFetchMaterials();
    }

    public boolean isUserAdminOfGroup(final CaseInsensitiveString userName, String groupName) {
        if (!isSecurityEnabled()) {
            return true;
        }
        PipelineConfigs group = null;
        if (groupName != null) {
            group = getCurrentConfig().findGroup(groupName);
        }
        return isUserAdmin(new Username(userName)) || isUserAdminOfGroup(userName, group);
    }

    public boolean isUserAdminOfGroup(final CaseInsensitiveString userName, PipelineConfigs group) {
        return group != null && group.isUserAnAdmin(userName, rolesForUser(userName));
    }

    public boolean isUserAdmin(Username username) {
        return isAdministrator(CaseInsensitiveString.str(username.getUsername()));
    }

    private boolean isUserTemplateAdmin(Username username) {
        return getCurrentConfig().getTemplates().canViewAndEditTemplate(username.getUsername(), rolesForUser(username.getUsername()));
    }

    public GoConfigRevision getConfigAtVersion(String version) {
        GoConfigRevision goConfigRevision = null;
        try {
            goConfigRevision = configRepository.getRevision(version);
        } catch (Exception e) {
            LOGGER.info("[Go Config Service] Could not fetch cruise config xml at version={}", version, e);
        }
        return goConfigRevision;
    }

    public List<PipelineConfig> pipelinesForFetchArtifacts(String pipelineName) {
        return currentCruiseConfig().pipelinesForFetchArtifacts(pipelineName);
    }

    private boolean isValidGroup(String groupName, CruiseConfig cruiseConfig, HttpLocalizedOperationResult result) {
        if (!cruiseConfig.hasPipelineGroup(groupName)) {
            result.notFound(EntityType.PipelineGroup.notFoundMessage(groupName), HealthStateType.general(HealthStateScope.forGroup(groupName)));
            return false;
        }
        return true;
    }

    private boolean isAdminOfGroup(String toGroupName, Username username, HttpLocalizedOperationResult result) {
        if (!isUserAdminOfGroup(username.getUsername(), toGroupName)) {
            result.forbidden(EntityType.PipelineGroup.forbiddenToEdit(toGroupName, username.getUsername()), forbidden());
            return false;
        }
        return true;
    }

    @Deprecated
    public GoConfigHolder getConfigHolder() {
        return goConfigDao.loadConfigHolder();
    }

    @TestOnly
    public CruiseConfig loadCruiseConfigForEdit(Username username, HttpLocalizedOperationResult result) {
        if (!isUserAdmin(username) && !isUserTemplateAdmin(username)) {
            result.forbidden(LocalizedMessage.forbiddenToEdit(), HealthStateType.forbidden());
        }
        return clonedConfigForEdit();
    }

    public CruiseConfig clonedConfigForEdit() {
        return cloner.deepClone(getConfigForEditing());
    }

    public CruiseConfig preprocessedCruiseConfigForPipelineUpdate(PipelineConfigCommand command) {
        CruiseConfig config = clonedConfigForEdit();
        command.update(config);
        config.setPartials(cachedGoPartials.lastValidPartials());
        MagicalGoConfigXmlLoader.preprocess(config);
        command.encrypt(config);
        return config;
    }

    public ConfigForEdit<PipelineConfigs> loadGroupForEditing(String groupName,
                                                              Username username,
                                                              HttpLocalizedOperationResult result) {
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
            result.badRequest("Historical configuration is not available for this stage run.");
        } catch (Exception e) {
            result.internalServerError("Could not retrieve config changes for this revision.");
        }
        return null;
    }

    public boolean isPipelineEditable(String pipelineName) {
        return isPipelineEditable(new CaseInsensitiveString(pipelineName));
    }

    public boolean isPipelineEditable(CaseInsensitiveString pipelineName) {
        PipelineConfig pipelineConfig;
        try {
            pipelineConfig = pipelineConfigNamed(pipelineName);
        } catch (RecordNotFoundException e) {
            return false;
        }
        return isOriginLocal(pipelineConfig.getOrigin());
    }

    private boolean isOriginLocal(ConfigOrigin origin) {
        // when null we assume that it comes from file or UI
        return origin == null || origin.isLocal();
    }

    public SCMs getSCMs() {
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

    public Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> getPackageUsageInPipelines() {
        return groups().getPackageUsageInPipelines();
    }

    public ElasticConfig getElasticConfig() {
        return cruiseConfig().getElasticConfig();
    }

    public Long elasticJobStarvationThreshold() {
        return getElasticConfig().getJobStarvationTimeout();
    }

    public ArtifactStores artifactStores() {
        return cruiseConfig().getArtifactStores();
    }

    public CruiseConfig validateCruiseConfig(CruiseConfig cruiseConfig) {
        return xmlLoader.validateCruiseConfig(cruiseConfig);
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

    @TestOnly
    public void forceNotifyListeners() {
        goConfigDao.reloadListeners();
    }

    public ConfigElementImplementationRegistry getRegistry() {
        return registry;
    }

    public PipelineConfig findPipelineByName(CaseInsensitiveString pipelineName) {
        List<PipelineConfig> pipelineConfigs = getAllPipelineConfigs()
                .stream()
                .filter((pipelineConfig) -> pipelineConfig.getName().equals(pipelineName))
                .toList();
        if (!pipelineConfigs.isEmpty()) {
            return pipelineConfigs.get(0);
        }
        return null;
    }

    public SecretConfig getSecretConfigById(String secretConfigId) {
        return this.cruiseConfig().getSecretConfigs().find(secretConfigId);
    }

    public abstract class XmlPartialSaver<T> {
        protected final SAXReader reader;
        private final ConfigElementImplementationRegistry registry;
        private String md5;

        protected XmlPartialSaver(ConfigElementImplementationRegistry registry) {
            this.registry = registry;
            reader = new SAXReader();
            reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        }

        protected ConfigSaveState updatePartial(String xmlPartial, final String md5) throws Exception {
            LOGGER.debug("[Config Save] Updating partial");
            Document document = documentRoot();
            Element root = document.getRootElement();

            Element configElement = ((Element) root.selectSingleNode(getXpath()));
            List<Node> nodes = configElement.getParent().content();
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

            ConfigSaveState configSaveState = saveConfigNewFlow(deserializedConfig, md5);

            LOGGER.debug("[Config Save] Finished saving XML");
            return configSaveState;
        }

        private ConfigSaveState saveConfigNewFlow(CruiseConfig cruiseConfig, String md5) {
            LOGGER.debug("[Config Save] Updating config using the new flow");
            return goConfigDao.updateFullConfig(new FullConfigUpdateCommand(cruiseConfig, md5));
        }

        protected Document documentRoot() throws Exception {
            CruiseConfig cruiseConfig = goConfigDao.loadForEditing();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new MagicalGoConfigXmlWriter(configCache, registry).write(cruiseConfig, out, true);
            Document document = reader.read(new StringReader(out.toString()));
            Map<String, String> map = new HashMap<>();
            map.put("go", MagicalGoConfigXmlWriter.XML_NS);
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
                return fromConflict(String.format("%s - %s", INVALID_CRUISE_CONFIG_XML, jsonException.getMessage()));
            } catch (ConfigMergePreValidationException e) {
                return mergePreValidationError(e.getMessage());
            } catch (Exception e) {
                if (e.getCause() instanceof ConfigMergePostValidationException) {
                    return mergePostValidationError(e.getCause().getMessage());
                }
                if (e.getCause() instanceof ConfigMergeException) {
                    return mergeConflict(e.getCause().getMessage());
                }
                return fromConflict(e.getMessage());
            }
        }

        private GoConfigValidity checkValidity() {
            try {
                valid();
                return GoConfigValidity.valid();
            } catch (Exception e) {
                return invalid(e.getMessage());
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

        XmlPartialFileSaver(final boolean shouldUpgrade,
                            final ConfigElementImplementationRegistry registry) {
            super(registry);
            this.shouldUpgrade = shouldUpgrade;
        }

        @Override
        protected ConfigSaveState updatePartial(String xmlFile, final String md5) throws Exception {
            if (shouldUpgrade) {
                xmlFile = upgrader.upgradeIfNecessary(xmlFile);
            }
            return saveConfig(xmlFile, md5);
        }

        @Override
        public String asXml() {
            return configAsXml(valid());
        }

        @Override
        protected CruiseConfig valid() {
            return configForEditing();
        }
    }

    private class XmlPartialPipelineGroupSaver extends XmlPartialSaver<Object> {
        private final String groupName;

        public XmlPartialPipelineGroupSaver(String groupName) {
            super(registry);
            this.groupName = groupName;
        }

        @Override
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
}
