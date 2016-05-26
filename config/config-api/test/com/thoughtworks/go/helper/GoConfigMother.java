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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.security.GoCipher;

import java.io.File;
import java.util.Arrays;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static java.util.Arrays.asList;

public class GoConfigMother {
    public Role createRole(String roleName, String... users) {
        return new Role(new CaseInsensitiveString(roleName), toRoleUsers(users));
    }

    private RoleUser[] toRoleUsers(String[] users) {
        RoleUser[] roleUsers = new RoleUser[users.length];
        for (int i = 0; i < users.length; i++) {
            roleUsers[i] = new RoleUser(new CaseInsensitiveString(users[i]));
        }
        return roleUsers;
    }

    public void addRole(CruiseConfig cruiseConfig, Role role) {
        cruiseConfig.server().security().addRole(role);
    }

    public void addAdminUserForPipelineGroup(CruiseConfig cruiseConfig, String user, String groupName) {
        PipelineConfigs group = cruiseConfig.getGroups().findGroup(groupName);
        group.getAuthorization().getAdminsConfig().add(new AdminUser(new CaseInsensitiveString(user)));
    }

    public GoConfigMother addAdminRoleForPipelineGroup(CruiseConfig config, String roleName, String groupName) {
        PipelineConfigs group = config.getGroups().findGroup(groupName);
        group.getAuthorization().getAdminsConfig().add(new AdminRole(new CaseInsensitiveString(roleName)));
        return this;
    }

    public void addRoleAsSuperAdmin(CruiseConfig cruiseConfig, String rolename) {
        AdminsConfig adminsConfig = cruiseConfig.server().security().adminsConfig();
        adminsConfig.addRole(new AdminRole(new CaseInsensitiveString(rolename)));
    }

    public static void enableSecurityWithPasswordFile(CruiseConfig cruiseConfig) {
        cruiseConfig.server().security().modifyPasswordFile(new PasswordFileConfig("password_file_path"));
    }

    public static CruiseConfig addUserAsSuperAdmin(CruiseConfig config, String adminName) {
        config.server().security().adminsConfig().add(new AdminUser(new CaseInsensitiveString(adminName)));
        return config;
    }

    public void addUserAsViewerOfPipelineGroup(CruiseConfig cruiseConfig, String userName, String groupName) {
        PipelineConfigs group = cruiseConfig.getGroups().findGroup(groupName);
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString(userName)));
    }

    public void addRoleAsViewerOfPipelineGroup(CruiseConfig cruiseConfig, String roleName, String groupName) {
        PipelineConfigs group = cruiseConfig.getGroups().findGroup(groupName);
        group.getAuthorization().getViewConfig().add(new AdminRole(new CaseInsensitiveString(roleName)));
    }

    public PipelineConfig addPipeline(CruiseConfig cruiseConfig,
                                      String pipelineName, String stageName, String... buildNames) {
        return addPipelineWithGroup(cruiseConfig, DEFAULT_GROUP, pipelineName, stageName, buildNames);
    }

    public PipelineConfig addPipelineWithGroup(CruiseConfig cruiseConfig, String groupName, String pipelineName, String stageName, String... buildNames) {
        return addPipelineWithGroup(cruiseConfig, groupName, pipelineName,
                new MaterialConfigs(MaterialConfigsMother.mockMaterialConfigs("file:///tmp/foo")),
                stageName, buildNames);
    }

    public PipelineConfig addPipelineWithGroup(CruiseConfig cruiseConfig, String groupName, String pipelineName, MaterialConfigs materialConfigs, String stageName, String... buildNames) {
        return addPipelineWithGroupAndTimer(cruiseConfig, groupName, pipelineName, materialConfigs, stageName, null, buildNames);
    }

    /*
        Used in rspecs
     */
    public CruiseConfig cruiseConfigWithPipelineUsingTwoMaterials() throws Exception {
        final CruiseConfig config = defaultCruiseConfig();
        addPipelineWithGroup(config, "group1", "pipeline1", MaterialConfigsMother.multipleMaterialConfigs(), "stage", "job");
        return config;
    }

    /*
        Used in rspecs
     */
    public PipelineConfig addPipelineWithTemplate(CruiseConfig cruiseConfig, String pipelineName, String templateName,
                                                  String stageName, String... buildNames) {
        PipelineTemplateConfig templateConfig = new PipelineTemplateConfig(new CaseInsensitiveString(templateName), StageConfigMother.custom(stageName, defaultBuildPlans(buildNames)));
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(pipelineName), MaterialConfigsMother.mockMaterialConfigs("file:///tmp/foo"));
        pipelineConfig.setTemplateName(new CaseInsensitiveString(templateName));
        cruiseConfig.addTemplate(templateConfig);
        cruiseConfig.addPipeline(DEFAULT_GROUP, pipelineConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipelineWithGroupAndTimer(CruiseConfig cruiseConfig, String groupName, String pipelineName, MaterialConfigs materialConfigs, String stageName, TimerConfig timer,
                                                       String... buildNames) {
        String cronSpec = timer == null ? null : timer.getTimerSpec();
        boolean shouldTriggerOnlyOnMaterialChanges = timer != null && timer.shouldTriggerOnlyOnChanges();

        StageConfig stageConfig = StageConfigMother.custom(stageName, defaultBuildPlans(buildNames));
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(pipelineName), PipelineLabel.COUNT_TEMPLATE, cronSpec, shouldTriggerOnlyOnMaterialChanges, materialConfigs,
                asList(stageConfig));
        pipelineConfig.setOrigin(new FileConfigOrigin());
        cruiseConfig.addPipeline(groupName, pipelineConfig);
        return pipelineConfig;
    }

    public PipelineConfig addPipeline(CruiseConfig cruiseConfig, String pipelineName, String stageName, MaterialConfigs materialConfigs, String... buildNames) {
        StageConfig stageConfig = StageConfigMother.custom(stageName, defaultBuildPlans(buildNames));
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs, stageConfig);
        pipelineConfig.setOrigin(new FileConfigOrigin());
        cruiseConfig.addPipeline(DEFAULT_GROUP, pipelineConfig);
        return pipelineConfig;
    }

    public PipelineConfig addStageToPipeline(CruiseConfig cruiseConfig, String pipelineName, String stageName, String... buildNames) {
        StageConfig stageConfig = StageConfigMother.custom(stageName, defaultBuildPlans(buildNames));
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        pipelineConfig.add(stageConfig);
        return pipelineConfig;
    }

    public PipelineConfig addStageToPipeline(CruiseConfig cruiseConfig, String pipelineName, String stageName,
                                             int stageindex, String... buildNames) {
        StageConfig stageConfig = StageConfigMother.custom(stageName, defaultBuildPlans(buildNames));
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        pipelineConfig.add(stageindex, stageConfig);
        return pipelineConfig;
    }

    public void setDependencyOn(CruiseConfig cruiseConfig, PipelineConfig pipelineConfig, String dependsOnPipeline,
                                String dependsOnStage) {
        PipelineConfig config = cruiseConfig.pipelineConfigByName(pipelineConfig.name());
        config.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString(dependsOnPipeline), new CaseInsensitiveString(dependsOnStage)));
    }

    public BasicCruiseConfig cruiseConfigWithTwoPipelineGroups() throws Exception {
        final BasicCruiseConfig config = cruiseConfigWithOnePipelineGroup();
        addPipelineWithGroup(config, "group2", "pipeline2", "stage", "job");
        return config;
    }

    public BasicCruiseConfig cruiseConfigWithOnePipelineGroup() throws Exception {
        final BasicCruiseConfig config = defaultCruiseConfig();
        addPipelineWithGroup(config, "group1", "pipeline1", "stage", "job");
        return config;
    }

    public static BasicCruiseConfig defaultCruiseConfig() {
        try {
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            ServerConfig serverConfig = new ServerConfig("artifactsDir", new SecurityConfig());
            cruiseConfig.setServerConfig(serverConfig);
            return cruiseConfig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JobConfigs defaultBuildPlans(String... planNames) {
        JobConfigs plans = new JobConfigs();
        for (String name : planNames) {
            plans.add(defaultBuildPlan(name));
        }
        return plans;
    }

    private static JobConfig defaultBuildPlan(String name) {
        return new JobConfig(new CaseInsensitiveString(name), new Resources(), new ArtifactPlans());
    }

    public static BasicCruiseConfig cruiseConfigWithMailHost(MailHost mailHost) {
        final BasicCruiseConfig config = new BasicCruiseConfig();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setMailHost(mailHost);
        config.setServerConfig(serverConfig);
        return config;
    }

    public static BasicCruiseConfig configWithPipelines(String... names) {
        final BasicCruiseConfig config = new BasicCruiseConfig();
        GoConfigMother mother = new GoConfigMother();
        for (String name : names) {
            mother.addPipeline(config, name, "stage", "job");
        }
        return config;
    }

    public static CruiseConfig configWithPackageRepo(String... ids) throws Exception {
        final CruiseConfig config = new BasicCruiseConfig();
        PackageConfigurations configuration = new PackageConfigurations();
        configuration.addConfiguration(new PackageConfiguration("key1"));
        configuration.addConfiguration(new PackageConfiguration("key2").with(PackageConfiguration.SECURE, true));
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-1", configuration);
        for (String id : ids) {
            PackageRepository packageRepository = new PackageRepository();
            packageRepository.setId(id);
            packageRepository.setName("name"+id);
            packageRepository.setPluginConfiguration(new PluginConfiguration("plugin-1", "1.0.0"));
            packageRepository.setPackages(new Packages(PackageDefinitionMother.create(id + "-pkg-1", packageRepository), PackageDefinitionMother.create(id + "-pkg-2", packageRepository)));
            GoCipher cipher = new GoCipher();
            ConfigurationProperty p1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"));
            ConfigurationProperty p2 = new ConfigurationProperty(new ConfigurationKey("key2"), null, new EncryptedConfigurationValue(cipher.encrypt("value2")), cipher);
            packageRepository.setConfiguration(new Configuration(p1, p2));
            config.setPackageRepositories(new PackageRepositories(packageRepository));
        }
        return config;
    }

    public static PipelineConfig createPipelineConfigWithMaterialConfig(MaterialConfig... materialConfigs) {
        return createPipelineConfigWithMaterialConfig("pipeline", materialConfigs);
    }

    public static PipelineConfig createPipelineConfigWithMaterialConfig(String pipelineName, MaterialConfig... materialConfigs) {
        CruiseConfig config = new BasicCruiseConfig();
        MaterialConfigs toAdd = new MaterialConfigs();
        toAdd.addAll(Arrays.asList(materialConfigs));
        return new GoConfigMother().addPipeline(config, pipelineName, "stage", toAdd, "job");
    }

    public static PipelineConfig createPipelineConfig(Filter filter, ScmMaterialConfig... materialConfigs) {
        for (ScmMaterialConfig scmMaterialConfig : materialConfigs) {
            scmMaterialConfig.setFilter(filter);
        }
        return createPipelineConfigWithMaterialConfig(materialConfigs);
    }

    public static CruiseConfig pipelineHavingJob(String pipelineName, String stageName, String jobPlanName, String filePath, String directoryPath) {
        CruiseConfig config = new BasicCruiseConfig();
        config.server().setArtifactsDir("logs");
        JobConfig job = new JobConfig(jobPlanName);
        String workingDir = new File("testdata/" + CruiseConfig.WORKING_BASE_DIR + stageName).getPath();
        AntTask task = new AntTask();
        task.setWorkingDirectory(workingDir);
        job.addTask(task);

        final ArtifactPlan artifactFile = new ArtifactPlan();
        artifactFile.setSrc(filePath);
        job.artifactPlans().add(artifactFile);

        ArtifactPlan artifactDir = new ArtifactPlan();
        artifactFile.setSrc(directoryPath);
        job.artifactPlans().add(artifactDir);

        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(pipelineName), new MaterialConfigs(new SvnMaterialConfig("file:///foo", null, null, false)), new StageConfig(
                new CaseInsensitiveString(stageName), new JobConfigs(job)));
        config.addPipeline(BasicPipelineConfigs.DEFAULT_GROUP, pipelineConfig);
        return config;
    }

    public CruiseConfig addApprovalForStage(CruiseConfig cruiseConfig, String pipelineName, String stageName, String roleName) {
        Approval stageApproval = cruiseConfig.stageConfigByName(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName)).getApproval();
        stageApproval.addAdmin(new AdminRole(new CaseInsensitiveString(roleName)));
        return cruiseConfig;
    }

    public static CruiseConfig simpleDiamond() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        PipelineConfig pipeline1 = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(MaterialConfigsMother.gitMaterialConfig("g1")));
        PipelineConfig pipeline2 = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(MaterialConfigsMother.gitMaterialConfig("g1")));
        PipelineConfig pipeline3 = PipelineConfigMother.pipelineConfig("p3",
                new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig("p1", "stage-1-1"), MaterialConfigsMother.dependencyMaterialConfig("p2", "stage-1-1")));

        cruiseConfig.addPipeline("group-1", pipeline1);
        cruiseConfig.addPipeline("group-1", pipeline2);
        cruiseConfig.addPipeline("group-1", pipeline3);
        return cruiseConfig;
    }

    public static CruiseConfig configWithConfigRepo() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(
                new GitMaterialConfig("https://github.com/tomzo/gocd-indep-config-part.git"),"myplugin"
        )));
        return cruiseConfig;
    }
}
