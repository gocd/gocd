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

package com.thoughtworks.go.util;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistrar;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.registry.NoPluginsInstalled;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @understands how to edit the cruise config file for testing
 */
public class GoConfigFileHelper {

    private final File configFile;
    private final String originalXml;

    public GoConfigMother goConfigMother = new GoConfigMother();
    private File passwordFile = null;
    private GoConfigDao goConfigDao;
    private CachedGoConfig cachedGoConfig;
    private SystemEnvironment sysEnv;
    private String originalConfigDir;

    public GoConfigFileHelper() {
        this(ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS);
    }

    public GoConfigFileHelper(GoPartialConfig partials) {
        this(ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS, partials);
    }

    public GoConfigFileHelper(GoConfigDao goConfigDao) {
        this(ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS, goConfigDao);
    }

    private GoConfigFileHelper(String xml, GoConfigDao goConfigDao) {
        new SystemEnvironment().setProperty(SystemEnvironment.ENFORCE_SERVER_IMMUTABILITY, "N");
        this.originalXml = xml;
        assignFileDao(goConfigDao);
        try {
            File dir = TestFileUtil.createTempFolder("server-config-dir");
            this.configFile = new File(dir, "cruise-config.xml");
            configFile.deleteOnExit();
            sysEnv = new SystemEnvironment();
            sysEnv.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
            initializeConfigFile();
        } catch (IOException e) {
            throw bomb("Error creating config file", e);
        }
    }

    private void assignFileDao(GoConfigDao goConfigDao) {
        this.goConfigDao = goConfigDao;
        try {
            Field field = GoConfigDao.class.getDeclaredField("cachedConfigService");
            field.setAccessible(true);
            this.cachedGoConfig = (CachedGoConfig) field.get(goConfigDao);
        } catch (Exception e) {
            bomb(e);
        }
    }

    /**
     * Creates config dao that accesses single file
     */
    public static GoConfigDao createTestingDao() {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        try {

            MaintenanceModeService maintenanceModeService = new MaintenanceModeService(new TimeProvider());
            ServerHealthService serverHealthService = new ServerHealthService();
            ConfigRepository configRepository = new ConfigRepository(systemEnvironment);
            configRepository.initialize();
            ConfigCache configCache = new ConfigCache();
            ConfigElementImplementationRegistry configElementImplementationRegistry = ConfigElementImplementationRegistryMother.withNoPlugins();
            CachedGoPartials cachedGoPartials = new CachedGoPartials(serverHealthService);
            FullConfigSaveNormalFlow normalFlow = new FullConfigSaveNormalFlow(configCache, configElementImplementationRegistry, systemEnvironment, new TimeProvider(), configRepository, cachedGoPartials);
            GoFileConfigDataSource dataSource = new GoFileConfigDataSource(new DoNotUpgrade(), configRepository, systemEnvironment, new TimeProvider(),
                    configCache, configElementImplementationRegistry, serverHealthService, cachedGoPartials, null, normalFlow);
            GoConfigMigration goConfigMigration = new GoConfigMigration(new TimeProvider(), configElementImplementationRegistry);
            GoConfigMigrator goConfigMigrator = new GoConfigMigrator(goConfigMigration, new SystemEnvironment(), configCache, configElementImplementationRegistry, normalFlow, configRepository, serverHealthService);
            FileUtils.writeStringToFile(dataSource.fileLocation(), ConfigFileFixture.configWithSecurity(""), UTF_8);
            goConfigMigrator.migrate();
            CachedGoConfig cachedConfigService = new CachedGoConfig(serverHealthService, dataSource, cachedGoPartials, null, maintenanceModeService);
            cachedConfigService.loadConfigIfNull();
            return new GoConfigDao(cachedConfigService);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates config dao that has custom remote configuration parts provided by partialConfig argument
     */
    public static GoConfigDao createTestingDao(GoPartialConfig partialConfig) {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        try {
            MaintenanceModeService maintenanceModeService = new MaintenanceModeService(new TimeProvider());
            ServerHealthService serverHealthService = new ServerHealthService();
            ConfigRepository configRepository = new ConfigRepository(systemEnvironment);
            configRepository.initialize();
            ConfigCache configCache = new ConfigCache();
            FullConfigSaveNormalFlow normalFlow = new FullConfigSaveNormalFlow(configCache, com.thoughtworks.go.util.ConfigElementImplementationRegistryMother.withNoPlugins(), systemEnvironment, new TimeProvider(), configRepository, new CachedGoPartials(serverHealthService));
            GoFileConfigDataSource dataSource = new GoFileConfigDataSource(new DoNotUpgrade(), configRepository, systemEnvironment, new TimeProvider(),
                    configCache, com.thoughtworks.go.util.ConfigElementImplementationRegistryMother.withNoPlugins(),
                    serverHealthService, new CachedGoPartials(serverHealthService), null, normalFlow);
            ConfigElementImplementationRegistry configElementImplementationRegistry = new ConfigElementImplementationRegistry(new NoPluginsInstalled());
            new ConfigElementImplementationRegistrar(configElementImplementationRegistry).initialize();
            GoConfigMigration goConfigMigration = new GoConfigMigration(new TimeProvider(), configElementImplementationRegistry);
            GoConfigMigrator goConfigMigrator = new GoConfigMigrator(goConfigMigration, new SystemEnvironment(), configCache, configElementImplementationRegistry, normalFlow, configRepository, serverHealthService);
            goConfigMigrator.migrate();

            CachedGoPartials cachedGoPartials = new CachedGoPartials(serverHealthService);
            CachedGoConfig cachedConfigService = new CachedGoConfig(serverHealthService, dataSource, cachedGoPartials, null, maintenanceModeService);
            cachedConfigService.loadConfigIfNull();
            return new GoConfigDao(cachedConfigService);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GoConfigFileHelper(File configFile) {
        assignFileDao(createTestingDao());
        this.configFile = configFile.getAbsoluteFile();
        try {
            saveFullConfig(FileUtils.readFileToString(this.configFile, UTF_8), true);
            this.originalXml = FileUtils.readFileToString(this.configFile, "UTF-8");
        } catch (Exception e) {
            throw bomb("Error reading config file", e);
        }
        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, this.configFile.getAbsolutePath());
    }

    public String getCurrentXml() throws IOException {
        return FileUtils.readFileToString(this.configFile, UTF_8);
    }

    public void saveFullConfig(String configFileContent, boolean shouldMigrate) throws Exception {
        if (shouldMigrate) {
            configFileContent = ConfigMigrator.migrate(configFileContent);
        }
        ConfigElementImplementationRegistry registry = new ConfigElementImplementationRegistry(new NoPluginsInstalled());
        new ConfigElementImplementationRegistrar(registry).initialize();
        MagicalGoConfigXmlLoader magicalGoConfigXmlLoader = new MagicalGoConfigXmlLoader(new ConfigCache(), registry);
        CruiseConfig configToBeWritten = magicalGoConfigXmlLoader.deserializeConfig(configFileContent);
        cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(configToBeWritten, cachedGoConfig.loadForEditing().getMd5()));
    }

    public GoConfigFileHelper(String xml) {
        this(xml, createTestingDao());
    }

    public GoConfigFileHelper(String xml, GoPartialConfig partials) {
        this(xml, createTestingDao(partials));
    }

    public GoConfigDao getGoConfigDao() {
        return goConfigDao;
    }

    public CachedGoConfig getCachedGoConfig() {
        return cachedGoConfig;
    }

    public void setArtifactsDir(String artifactsDir) {
        CruiseConfig cruiseConfig = loadForEdit();
        cruiseConfig.server().setArtifactsDir(artifactsDir);
        writeConfigFile(cruiseConfig);
    }

    public GoConfigFileHelper usingCruiseConfigDao(GoConfigDao goConfigDao) {
        assignFileDao(goConfigDao);
        return this;
    }

    public void usingEmptyConfigFileWithLicenseAllowsTwoAgents() {
        writeToFileAndDB(ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS);
    }

    private void writeToFileAndDB(String configContent) {
        writeXmlToConfigFile(loadAndMigrate(configContent));
        writeConfigFile(load());
    }

    public static GoConfigFileHelper usingEmptyConfigFileWithLicenseAllowsUnlimitedAgents() {
        return new GoConfigFileHelper(ConfigFileFixture.DEFAULT_XML_WITH_UNLIMITED_AGENTS);
    }


    public void writeXmlToConfigFile(String xml) {
        try {
            FileUtils.writeStringToFile(configFile, xml, UTF_8);
            goConfigDao.forceReload();
        } catch (Exception e) {
            throw bomb("Error writing config file: " + configFile.getAbsolutePath(), e);
        }
    }

    public void onSetUp() throws IOException {
        initializeConfigFile();
        goConfigDao.forceReload();
        writeConfigFile(load());
        originalConfigDir = sysEnv.getConfigDir();
        File configDir = configFile.getParentFile();
        sysEnv.setProperty(SystemEnvironment.CONFIG_DIR_PROPERTY, configDir.getAbsolutePath());
    }

    public void initializeConfigFile() throws IOException {
        FileUtils.deleteQuietly(passwordFile);
        writeXmlToConfigFile(ConfigMigrator.migrate(originalXml));
    }

    public void onTearDown() {
        sysEnv.setProperty(SystemEnvironment.CONFIG_DIR_PROPERTY, originalConfigDir);
        FileUtils.deleteQuietly(configFile);
        try {
            saveFullConfig(originalXml, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName) {
        return addPipeline(pipelineName, stageName, "unit", "functional");
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, Subversion repository) {
        return addPipeline(pipelineName, stageName, repository, "unit", "functional");
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, String... buildNames) {
        return addPipelineWithGroup(BasicPipelineConfigs.DEFAULT_GROUP, pipelineName, stageName, buildNames);
    }

    public PipelineTemplateConfig addTemplate(String pipelineName, String stageName) {
        return addTemplate(pipelineName, new Authorization(), stageName);
    }

    public PipelineTemplateConfig addTemplate(String templateName, String paramName, String stageName) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate(templateName, new Authorization(), StageConfigMother.stageConfigWithParams(stageName, paramName));
        cruiseConfig.getTemplates().add(templateConfig);
        writeConfigFile(cruiseConfig);
        return templateConfig;
    }

    public PipelineTemplateConfig addTemplate(String pipelineName, Authorization authorization, String stageName) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineTemplateConfig templateConfig = PipelineTemplateConfigMother.createTemplate(pipelineName, authorization, StageConfigMother.manualStage(stageName));
        cruiseConfig.getTemplates().add(templateConfig);
        writeConfigFile(cruiseConfig);
        return templateConfig;
    }

    public PipelineConfig addPipelineWithGroup(String groupName, String pipelineName, String stageName, String... buildNames) {
        return addPipelineWithGroup(groupName, pipelineName, new SvnCommand(null, "svn:///user:pass@tmp/foo"), stageName, buildNames);
    }

    public PipelineConfig addPipelineWithTemplate(String groupName, String pipelineName, String templateName) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(pipelineName), MaterialConfigsMother.mockMaterialConfigs("svn:///user:pass@tmp/foo"));
        pipelineConfig.setTemplateName(new CaseInsensitiveString(templateName));
        cruiseConfig.findGroup(groupName).add(pipelineConfig);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipelineWithGroup(String groupName, String pipelineName, SvnCommand svnCommand, String stageName, String... buildNames) {
        return addPipelineWithGroupAndTimer(groupName, pipelineName, new MaterialConfigs(MaterialConfigsMother.mockMaterialConfigs(svnCommand.getUrlForDisplay())), stageName, null, buildNames);
    }

    public PipelineConfig addPipelineWithGroup(String groupName, String pipelineName, MaterialConfigs materialConfigs, String stageName, String... buildNames) {
        return addPipelineWithGroupAndTimer(groupName, pipelineName, materialConfigs, stageName, null, buildNames);
    }

    public PipelineConfig addPipelineWithGroupAndTimer(String groupName, String pipelineName, MaterialConfigs materialConfigs, String stageName, TimerConfig timer, String... buildNames) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = goConfigMother.addPipelineWithGroupAndTimer(cruiseConfig, groupName, pipelineName, materialConfigs, stageName, timer, buildNames);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipeline(String groupName, PipelineConfig pipelineConfig) {
        CruiseConfig cruiseConfig = loadForEdit();
        cruiseConfig.addPipeline(groupName, pipelineConfig);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public void addSecurityAuthConfig(SecurityAuthConfig securityAuthConfig) {
        CruiseConfig config = loadForEdit();
        config.server().security().securityAuthConfigs().add(securityAuthConfig);
        writeConfigFile(config);
    }

    public interface Updater<T> {
        void update(T t);
    }

    public PipelineConfig updatePipeline(CaseInsensitiveString pipelineName, Updater<PipelineConfig> updater) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.getPipelineConfigByName(pipelineName);
        updater.update(pipelineConfig);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipeline(PipelineConfig pipelineConfig) {
        return addPipelineToGroup(pipelineConfig, "quux-group");
    }

    public PipelineConfig addPipelineToGroup(PipelineConfig pipelineConfig, final String groupName) {
        CruiseConfig cruiseConfig = loadForEdit();
        cruiseConfig.addPipeline(groupName, pipelineConfig);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipelineWithGroup(String groupName, String pipelineName, MaterialConfigs materialConfigs, TrackingTool trackingTool, String stageName, String... jobs) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = goConfigMother.addPipelineWithGroup(cruiseConfig, groupName, pipelineName,
                materialConfigs,
                stageName,
                jobs);
        pipelineConfig.setTrackingTool(trackingTool);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, Subversion repository, String... buildNames) {
        return addPipeline(pipelineName, stageName, svn(repository.getUrl().originalArgument(), repository.getUserName(), repository.getPassword(), repository.isCheckExternals()), buildNames);
    }

    public void updateArtifactRoot(String path) {
        CruiseConfig cruiseConfig = loadForEdit();
        cruiseConfig.server().updateArtifactRoot(path);
        writeConfigFile(cruiseConfig);
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, Subversion repository, Filter filter, String... buildNames) throws Exception {
        return addPipeline(pipelineName, stageName, svn(repository.getUrl().originalArgument(), repository.getUserName(), repository.getPassword(), repository.isCheckExternals()), filter, buildNames);
    }

    private PipelineConfig addPipeline(String pipelineName, String stageName, SvnMaterialConfig svnMaterialConfig, Filter filter,
                                       String... buildNames) throws Exception {
        svnMaterialConfig.setFilter(filter);
        return addPipeline(pipelineName, stageName, svnMaterialConfig, buildNames);
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, MaterialConfig materialConfig, String... buildNames) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, pipelineName, stageName, new MaterialConfigs(materialConfig), buildNames);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, MaterialConfig materialConfig, TrackingTool trackingTool, String... jobs) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, pipelineName, stageName, new MaterialConfigs(materialConfig), jobs);
        pipelineConfig.setTrackingTool(trackingTool);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }


    public PipelineConfig addPipeline(String pipelineName, String stageName, MaterialConfigs materialConfigs, String... buildNames) throws Exception {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = goConfigMother.addPipeline(cruiseConfig, pipelineName, stageName, materialConfigs, buildNames);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addStageToPipeline(String pipelineName, String stageName) throws Exception {
        return addStageToPipeline(pipelineName, stageName, "unit");
    }

    public PipelineConfig addStageToPipeline(String pipelineName, String stageName, String... buildNames)
            throws Exception {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = goConfigMother.addStageToPipeline(cruiseConfig, pipelineName, stageName,
                buildNames);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public PipelineConfig addStageToPipeline(String pipelineName, StageConfig stageConfig) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        pipelineConfig.add(stageConfig);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public void addEnvironmentVariableToPipeline(String pipelineName, EnvironmentVariablesConfig envVars) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        pipelineConfig.setVariables(envVars);
        writeConfigFile(cruiseConfig);
    }

    public void addEnvironmentVariableToStage(String pipelineName, String stageName, EnvironmentVariablesConfig envVars) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(stageName));
        stageConfig.setVariables(envVars);
        writeConfigFile(cruiseConfig);
    }

    public void addEnvironmentVariableToJob(String pipelineName, String stageName, String jobName, EnvironmentVariablesConfig envVars) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(stageName));
        JobConfig jobConfig = stageConfig.jobConfigByConfigName(new CaseInsensitiveString(jobName));
        jobConfig.setVariables(envVars);
        writeConfigFile(cruiseConfig);
    }

    public PipelineConfig addStageToPipeline(String pipelineName, String stageName, int stageindex,
                                             String... buildNames) throws Exception {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = goConfigMother.addStageToPipeline(
                cruiseConfig, pipelineName, stageName, stageindex, buildNames);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public StageConfig removeStage(String pipelineName, String stageName) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(stageName));
        pipelineConfig.remove(stageConfig);
        writeConfigFile(cruiseConfig);
        return stageConfig;
    }

    public void removePipeline(String pipelineName) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfigs groups = removePipeline(pipelineName, cruiseConfig);
        if (groups.isEmpty()) {
            cruiseConfig.getGroups().remove(groups);
        }
        writeConfigFile(cruiseConfig);
    }

    public PipelineConfigs removePipeline(String pipelineName, CruiseConfig cruiseConfig) {
        String groupName = cruiseConfig.getGroups().findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));
        PipelineConfigs groups = cruiseConfig.getGroups().findGroup(groupName);
        groups.remove(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName)));
        return groups;
    }

    public StageConfig addJob(String pipelineName, String stageName, String jobName) {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString(jobName), new ResourceConfigs(), new ArtifactTypeConfigs());
        return addJobToStage(pipelineName, stageName, jobConfig);
    }

    public StageConfig addJobToStage(String pipelineName, String stageName, JobConfig jobConfig) {
        return pushJobIntoStage(pipelineName, stageName, jobConfig, false);
    }

    public void replaceAllJobsInStage(String pipelineName, String stageName, JobConfig jobConfig) {
        pushJobIntoStage(pipelineName, stageName, jobConfig, true);
    }

    private StageConfig pushJobIntoStage(String pipelineName, String stageName, JobConfig jobConfig, boolean clearExistingJobs) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(stageName));
        if (clearExistingJobs) {
            stageConfig.allBuildPlans().clear();
        }
        stageConfig.allBuildPlans().add(jobConfig);
        writeConfigFile(cruiseConfig);
        return stageConfig;
    }

    public PipelineConfig addPipelineWithInvalidMaterial(String pipelineName, String stageName) {
        CruiseConfig cruiseConfig = loadForEdit();
        StageConfig stageConfig = StageConfigMother.custom(stageName, defaultBuildPlans("buildName"));
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(pipelineName), invalidRepositoryMaterialConfigs(), stageConfig);
        cruiseConfig.addPipeline(DEFAULT_GROUP, pipelineConfig);
        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public File getConfigFile() {
        return configFile;
    }


    private MaterialConfig invalidSvnMaterialConfig() {
        return svn("invalid://invalid url", "user", "password", false);
    }

    private MaterialConfigs invalidRepositoryMaterialConfigs() {
        return new MaterialConfigs(invalidSvnMaterialConfig());
    }

    private static JobConfigs defaultBuildPlans(String... planNames) {
        JobConfigs plans = new JobConfigs();
        for (String name : planNames) {
            plans.add(defaultBuildPlan(name));
        }
        return plans;
    }

    private static JobConfig defaultBuildPlan(String name) {
        return new JobConfig(new CaseInsensitiveString(name), new ResourceConfigs(), new ArtifactTypeConfigs());
    }


    public CruiseConfig load() {
        try {
            goConfigDao.forceReload();
            return new GoConfigCloner().deepClone(goConfigDao.loadForEditing());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CruiseConfig loadForEdit() {
        try {
            goConfigDao.forceReload();
            return new GoConfigCloner().deepClone(goConfigDao.loadForEditing());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CruiseConfig currentConfig() {
        return load();
    }


    public void addConfigRepo(ConfigRepoConfig configRepoConfig) {
        CruiseConfig cruiseConfig = loadForEdit();
        cruiseConfig.getConfigRepos().add(configRepoConfig);
        writeConfigFile(cruiseConfig);
    }

    public void setConfigRepos(ConfigReposConfig configRepos) {
        CruiseConfig cruiseConfig = loadForEdit();
        cruiseConfig.setConfigRepos(configRepos);
        writeConfigFile(cruiseConfig);
    }

    public void makeJobRunOnAllAgents(String pipeline, String stageName, String jobName) {
        CruiseConfig cruiseConfig = currentConfig();
        cruiseConfig.jobConfigByName(pipeline, stageName, jobName, true).setRunOnAllAgents(true);
        writeConfigFile(cruiseConfig);

    }

    public void addSecurity(SecurityConfig securityConfig) {
        CruiseConfig config = loadForEdit();
        config.server().useSecurity(securityConfig);
        writeConfigFile(config);
    }

    public void turnOffSecurity() {
        addSecurity(new SecurityConfig());
    }

    public void addBogusSecurity(boolean anonymous) {
        final SecurityConfig securityConfig = new SecurityConfig(new AdminsConfig());

        final SecurityAuthConfig securityAuthConfig = new SecurityAuthConfig("ldap", "cd.go.authorization.ldap");
        final SecurityAuthConfig passwordFile = new SecurityAuthConfig("file", "cd.go.authentication.passwordfile");

        securityConfig.securityAuthConfigs().add(securityAuthConfig);
        securityConfig.securityAuthConfigs().add(passwordFile);
        addSecurity(securityConfig);
    }

    public File addSecurityWithPasswordFile() throws IOException {
        final SecurityAuthConfig authConfig = new SecurityAuthConfig("file", "cd.go.authentication.passwordfile",
                create("PasswordFilePath", false, addPasswordFile().getAbsolutePath())
        );
        SecurityConfig securityConfig = new SecurityConfig(new AdminsConfig());
        securityConfig.securityAuthConfigs().add(authConfig);
        addSecurity(securityConfig);
        return passwordFile;
    }

    public void enableSecurity() {
        addSecurityAuthConfig(new SecurityAuthConfig(UUID.randomUUID().toString(), "plugin_id"));
    }

    @Deprecated
    public File turnOnSecurity() throws IOException {
        return addSecurityWithPasswordFile();
    }

    public void addSecurityWithAdminConfig() throws Exception {
        enableSecurity();
        addAdmins("admin1");
    }

    private File addPasswordFile() throws IOException {
        passwordFile = TestFileUtil.createTempFile("password.properties");
        passwordFile.deleteOnExit();
        final String nonAdmin = "jez=ThmbShxAtJepX80c2JY1FzOEmUk=\n"; //in plain text: badger
        final String admin1 = "admin1=W6ph5Mm5Pz8GgiULbPgzG37mj9g=\n"; //in plain text: password
        FileUtils.writeStringToFile(passwordFile, nonAdmin + admin1, UTF_8);
        return passwordFile;
    }

    public void addMailHost(MailHost mailHost) {
        CruiseConfig config = loadForEdit();
        config.server().updateMailHost(mailHost);
        writeConfigFile(config);

    }

    public void addRole(Role role) {
        CruiseConfig config = loadForEdit();
        config.server().security().addRole(role);
        writeConfigFile(config);
    }

    public void replaceMaterialWithHgRepoForPipeline(String pipelinename, String hgUrl) {
        replaceMaterialForPipeline(pipelinename, MaterialConfigsMother.hgMaterialConfig(hgUrl));
    }

    public PipelineConfig replaceMaterialForPipeline(String pipelinename, MaterialConfig materialConfig) {
        return replaceMaterialConfigForPipeline(pipelinename, new MaterialConfigs(materialConfig));
    }

    public PipelineConfig replaceMaterialConfigForPipeline(String pipelinename, MaterialConfig materialConfig) {
        return replaceMaterialConfigForPipeline(pipelinename, new MaterialConfigs(materialConfig));
    }

    public PipelineConfig setMaterialConfigForPipeline(String pipelinename, MaterialConfig... materialConfigs) {
        return addMaterialConfigForPipeline(pipelinename, materialConfigs);
    }

    private PipelineConfig addMaterialConfigForPipeline(String pipelinename, MaterialConfig... materialConfigs) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelinename));
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(materialConfigs));

        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    private PipelineConfig replaceMaterialConfigForPipeline(String pipelinename, MaterialConfigs materialConfigs) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelinename));
        pipelineConfig.setMaterialConfigs(materialConfigs);

        writeConfigFile(cruiseConfig);
        return pipelineConfig;
    }

    public void requireApproval(String pipelineName, String stageName) {
        CruiseConfig cruiseConfig = loadForEdit();
        cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName)).findBy(new CaseInsensitiveString(stageName)).updateApproval(Approval.manualApproval());
        writeConfigFile(cruiseConfig);
    }

    public void setDependencyOn(PipelineConfig product, String pipelineName, String stageName) {
        CruiseConfig cruiseConfig = loadForEdit();
        goConfigMother.setDependencyOn(cruiseConfig, product, pipelineName, stageName);
        writeConfigFile(cruiseConfig);
    }

    public void writeConfigFile(CruiseConfig cruiseConfig) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            getXml(cruiseConfig, buffer);
            saveFullConfig(new String(buffer.toByteArray()), false);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    public void getXml(CruiseConfig cruiseConfig, ByteArrayOutputStream buffer) throws Exception {
        new MagicalGoConfigXmlWriter(new ConfigCache(), com.thoughtworks.go.util.ConfigElementImplementationRegistryMother.withNoPlugins()).write(cruiseConfig, buffer, false);
    }

    public void configureStageAsAutoApproval(String pipelineName, String stage) {
        updateApproval(pipelineName, stage, Approval.automaticApproval());
    }

    public void configureStageAsManualApproval(String pipelineName, String stage) {
        updateApproval(pipelineName, stage, Approval.manualApproval());
    }

    public void configureStageAsManualApproval(String pipelineName, String stage, boolean allowOnlyOnSuccess) {
        Approval manualApproval = Approval.manualApproval();
        manualApproval.setAllowOnlyOnSuccess(allowOnlyOnSuccess);
        updateApproval(pipelineName, stage, manualApproval);
    }

    public void addAuthorizedUserForStage(String pipelineName, String stageName, String... users) {
        configureStageAsManualApproval(pipelineName, stageName);
        CruiseConfig cruiseConfig = loadForEdit();
        StageConfig stageConfig = cruiseConfig.stageConfigByName(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
        Approval approval = stageConfig.getApproval();
        for (String user : users) {
            approval.getAuthConfig().add(new AdminUser(new CaseInsensitiveString(user)));
        }
        writeConfigFile(cruiseConfig);
    }

    public void addAuthorizedUserForPipelineGroup(String user) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfigs group = cruiseConfig.getGroups().first();
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString(user)));
        writeConfigFile(cruiseConfig);
    }

    public void addAuthorizedUserForPipelineGroup(String user, String groupName) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfigs group = cruiseConfig.getGroups().findGroup(groupName);
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString(user)));
        writeConfigFile(cruiseConfig);
    }

    private void updateApproval(String pipelineName, String ftStage, Approval manualApproval) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        StageConfig config = pipelineConfig.findBy(new CaseInsensitiveString(ftStage));
        config.updateApproval(manualApproval);
        writeConfigFile(cruiseConfig);
    }

    public boolean isSecurityEnabled() {
        CruiseConfig cruiseConfig = loadForEdit();
        return cruiseConfig.server().isSecurityEnabled();
    }

    public static CruiseConfig load(String content) {
        try {
            return new GoConfigFileHelper(content).currentConfig();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadAndMigrate(String originalContent) {
        GoConfigFileHelper helper = new GoConfigFileHelper(originalContent);
        try {
            return FileUtils.readFileToString(helper.getConfigFile(), UTF_8);
        } catch (IOException e) {
            throw bomb(e);
        }
    }

    public void setAdminPermissionForGroup(String groupName, String user) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfigs group = cruiseConfig.getGroups().findGroup(groupName);
        group.getAuthorization().getAdminsConfig().add(new AdminUser(new CaseInsensitiveString(user)));
        writeConfigFile(cruiseConfig);
    }

    public void setViewPermissionForGroup(String groupName, String username) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfigs group = cruiseConfig.getGroups().findGroup(groupName);
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString(username)));
        writeConfigFile(cruiseConfig);
    }

    public void setOperatePermissionForGroup(String groupName, String... userNames) {
        CruiseConfig cruiseConfig = loadForEdit();
        Admin[] admins = AdminUserMother.adminUsers(userNames);
        for (Admin admin : admins) {
            cruiseConfig.getGroups().findGroup(groupName).getAuthorization().getOperationConfig().add(admin);
        }
        writeConfigFile(cruiseConfig);
    }

    public void setOperatePermissionForStage(String pipelineName, String stageName, String username) {
        CruiseConfig cruiseConfig = loadForEdit();
        StageConfig stageConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName)).findBy(new CaseInsensitiveString(stageName));
        stageConfig.updateApproval(new Approval(new AuthConfig(new AdminUser(new CaseInsensitiveString(username)))));
        writeConfigFile(cruiseConfig);
    }

    public void setPipelineLabelTemplate(String pipelineName, String labelTemplate) {
        CruiseConfig config = loadForEdit();
        config.pipelineConfigByName(new CaseInsensitiveString(pipelineName)).setLabelTemplate(labelTemplate);
        writeConfigFile(config);
    }

    public void setupMailHost() {
        CruiseConfig config = loadForEdit();
        config.server().setMailHost(
                new MailHost("10.18.3.171", 25, "cruise2", "password", true, false, "cruise2@cruise.com", "admin@cruise.com"));
        writeConfigFile(config);
    }

    public EnvironmentConfig getEnvironment(String env) {
        CruiseConfig config = load();
        return config.getEnvironments().find(new CaseInsensitiveString(env));
    }

    public void addPipelineToEnvironment(String env, String pipelineName) {
        CruiseConfig config = loadForEdit();
        config.getEnvironments().addPipelinesToEnvironment(env, pipelineName);
        writeConfigFile(config);
    }

    public void setRunOnAllAgents(String pipelineName, String stageName, String jobName, boolean runOnAllAgents) {
        CruiseConfig config = loadForEdit();
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        pipelineConfig.findBy(new CaseInsensitiveString(stageName)).jobConfigByInstanceName(jobName, true).setRunOnAllAgents(runOnAllAgents);
        writeConfigFile(config);
    }

    public void setRunMultipleInstance(String pipelineName, String stageName, String jobName, Integer runInstanceCount) {
        CruiseConfig config = loadForEdit();
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        pipelineConfig.findBy(new CaseInsensitiveString(stageName)).jobConfigByInstanceName(jobName, true).setRunInstanceCount(runInstanceCount);
        writeConfigFile(config);
    }

    public void addResourcesFor(String pipelineName, String stageName, String jobName, String... resources) {
        CruiseConfig config = loadForEdit();
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        for (String resource : resources) {
            pipelineConfig.findBy(new CaseInsensitiveString(stageName)).jobConfigByConfigName(new CaseInsensitiveString(jobName)).addResourceConfig(resource);
        }
        writeConfigFile(config);
    }

    public void addAssociatedEntitiesForAJob(String pipelineName, String stageName, String jobName, ResourceConfigs resourceConfigs,
                                             ArtifactTypeConfigs artifactConfigs) {
        CruiseConfig config = loadForEdit();
        JobConfig jobConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName)).findBy(new CaseInsensitiveString(stageName)).jobConfigByConfigName(new CaseInsensitiveString(jobName));
        ReflectionUtil.setField(jobConfig, "resourceConfigs", resourceConfigs);
        ReflectionUtil.setField(jobConfig, "artifactConfigs", artifactConfigs);
        writeConfigFile(config);
    }

    public PipelineConfig addMaterialToPipeline(String pipelineName, MaterialConfig materialConfig) {
        CruiseConfig config = loadForEdit();
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        for (MaterialConfig materialConfig1 : new MaterialConfig[]{materialConfig}) {
            pipelineConfig.addMaterialConfig(materialConfig1);
        }
        writeConfigFile(config);
        return pipelineConfig;
    }

    public PipelineConfig removeMaterialFromPipeline(String pipelineName, MaterialConfig materialConfig) {
        CruiseConfig config = loadForEdit();
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        pipelineConfig.removeMaterialConfig(materialConfig);
        writeConfigFile(config);
        return pipelineConfig;
    }

    public PipelineConfig changeStagenameForToPipeline(String pipelineName, String oldStageName, String newStageName) {
        CruiseConfig config = loadForEdit();
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));

        StageConfig stage = pipelineConfig.getStage(new CaseInsensitiveString(oldStageName));
        int index = pipelineConfig.indexOf(stage);

        stage = StageConfigMother.custom(newStageName, stage.isFetchMaterials(), stage.isCleanWorkingDir(), stage.getJobs(), stage.getApproval());
        pipelineConfig.set(index, stage);

        writeConfigFile(config);
        return pipelineConfig;
    }

    public void blockPipelineGroupExceptFor(String pipelineGroupName, String roleName) {
        CruiseConfig config = loadForEdit();
        PipelineConfigs configs = config.getGroups().findGroup(pipelineGroupName);
        Authorization authorization = new Authorization(new OperationConfig(new AdminRole(new CaseInsensitiveString(roleName))), new ViewConfig(new AdminRole(new CaseInsensitiveString(roleName))));
        configs.setAuthorization(authorization);
        writeConfigFile(config);
    }

    public void addAdmins(String... adminNames) {
        CruiseConfig cruiseConfig = loadForEdit();
        AdminsConfig adminsConfig = cruiseConfig.server().security().adminsConfig();
        for (String adminName : adminNames) {
            adminsConfig.add(new AdminUser(new CaseInsensitiveString(adminName)));
        }
        writeConfigFile(cruiseConfig);
    }

    public void addAdminRoles(String... roleNames) {
        CruiseConfig cruiseConfig = loadForEdit();
        AdminsConfig adminsConfig = cruiseConfig.server().security().adminsConfig();
        for (String roleName : roleNames) {
            adminsConfig.add(new AdminRole(new CaseInsensitiveString(roleName)));
        }
        writeConfigFile(cruiseConfig);
    }

    public void lockPipeline(String name) {
        CruiseConfig config = loadForEdit();
        PipelineConfig pipeline = config.pipelineConfigByName(new CaseInsensitiveString(name));
        pipeline.lockExplicitly();
        writeConfigFile(config);
    }

    public void addEnvironments(String... environmentNames) {
        CruiseConfig config = loadForEdit();
        for (String environmentName : environmentNames) {
            config.addEnvironment(environmentName);
        }
        writeConfigFile(config);
    }

    public void addEnvironments(List<String> environmentNames) {
        addEnvironments(environmentNames.toArray(new String[environmentNames.size()]));
    }

    public void addEnvironmentVariablesToEnvironment(String environmentName, String variableName, String variableValue) {
        CruiseConfig config = loadForEdit();
        EnvironmentConfig env = config.getEnvironments().named(new CaseInsensitiveString(environmentName));
        env.addEnvironmentVariable(variableName, variableValue);
        writeConfigFile(config);
    }

    public void deleteConfigFile() {
        configFile.delete();
    }

    public static EnvironmentVariablesConfig env(String name, String value) {
        return EnvironmentVariablesConfigMother.env(name, value);
    }

    public static EnvironmentVariablesConfig env(String[] names, String[] values) {
        return EnvironmentVariablesConfigMother.env(names, values);
    }

    public void setBaseUrls(SiteUrl siteUrl, SecureSiteUrl secureSiteUrl) {
        CruiseConfig config = loadForEdit();

        config.setServerConfig(
                new ServerConfig(config.server().security(), config.server().mailHost(), siteUrl, secureSiteUrl));
        writeConfigFile(config);
    }

    public void removeJob(String pipelineName, String stageName, String jobName) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString(stageName));
        JobConfig job = stageConfig.getJobs().getJob(new CaseInsensitiveString(jobName));
        stageConfig.getJobs().remove(job);
        writeConfigFile(cruiseConfig);
    }

    public void addParamToPipeline(String pipeline, String paramName, String paramValue) {
        CruiseConfig cruiseConfig = loadForEdit();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipeline));
        pipelineConfig.addParam(new ParamConfig(paramName, paramValue));
        writeConfigFile(cruiseConfig);
    }

    public void addPackageDefinition(PackageMaterialConfig packageMaterialConfig) {
        CruiseConfig config = loadForEdit();
        PackageRepository repository = packageMaterialConfig.getPackageDefinition().getRepository();
        config.getPackageRepositories().add(repository);
        writeConfigFile(config);
    }

    public void addSCMConfig(SCM scmConfig) {
        CruiseConfig config = loadForEdit();
        config.getSCMs().add(scmConfig);
        writeConfigFile(config);
    }

    public NoOverwriteUpdateConfigCommand addPipelineCommand(final String oldMd5, final String pipelineName, final String stageName, final String jobName) {
        return new NoOverwriteUpdateConfigCommand() {
            @Override
            public String unmodifiedMd5() {
                return oldMd5;
            }

            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.addPipeline("g1", PipelineConfigMother.pipelineConfig(pipelineName, StageConfigMother.custom(stageName, jobName)));
                return cruiseConfig;
            }
        };
    }

    public NoOverwriteUpdateConfigCommand addPipelineCommand(final String oldMd5, final PipelineConfig pipelineConfig) {
        return new NoOverwriteUpdateConfigCommand() {
            @Override
            public String unmodifiedMd5() {
                return oldMd5;
            }

            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.addPipeline("g1", pipelineConfig);
                return cruiseConfig;
            }
        };
    }

    public UpdateConfigCommand changeJobNameCommand(final String md5, final String pipelineName, final String stageName, final String oldJobName, final String newJobName) {
        return new NoOverwriteUpdateConfigCommand() {
            @Override
            public String unmodifiedMd5() {
                return md5;
            }

            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                JobConfig job = cruiseConfig.findJob(pipelineName, stageName, oldJobName);
                ReflectionUtil.setField(job, "jobName", new CaseInsensitiveString(newJobName));
                return cruiseConfig;
            }
        };

    }


    /*public void addPipelineGroup(String groupName) {
        CruiseConfig config = loadForEdit();
        config.addGroup(groupName);
        writeConfigFile(config);
    }*/

    public static class AdminUserMother {

        public static Admin[] adminUsers(String... userNames) {
            Admin[] result = new Admin[userNames.length];
            for (int i = 0; i < userNames.length; i++) {
                String userName = userNames[i];
                result[i] = new AdminUser(new CaseInsensitiveString(userName));
            }
            return result;
        }
    }

    public static void clearConfigVersions() throws IOException {
        FileUtils.deleteDirectory(new SystemEnvironment().getConfigRepoDir());
    }

    public static void withServerIdImmutability(Runnable fn) {
        try {
            SystemEnvironment.enforceServerImmutability.set(true);
            fn.run();
        } finally {
            SystemEnvironment.enforceServerImmutability.set(false);
        }
    }
}
