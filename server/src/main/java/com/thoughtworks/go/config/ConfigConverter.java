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

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.plugin.configrepo.contract.*;
import com.thoughtworks.go.plugin.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.*;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper to transform config repo classes to config-api classes
 */
public class ConfigConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigConverter.class);

    private final GoCipher cipher;
    private final CachedGoConfig cachedGoConfig;
    private Cloner cloner = new Cloner();
    private SCMs newSCMs = new SCMs();

    public ConfigConverter(GoCipher goCipher, CachedGoConfig cachedGoConfig) {
        this.cipher = goCipher;
        this.cachedGoConfig = cachedGoConfig;
    }

    public PartialConfig toPartialConfig(CRParseResult crPartialConfig, PartialConfigLoadContext context) {
        newSCMs = new SCMs();
        PartialConfig partialConfig = new PartialConfig();
        for (CREnvironment crEnvironment : crPartialConfig.getEnvironments()) {
            EnvironmentConfig environment = toEnvironmentConfig(crEnvironment);
            partialConfig.getEnvironments().add(environment);
        }
        Map<String, List<CRPipeline>> pipesByGroup = groupPipelinesByGroupName(crPartialConfig.getPipelines());
        for (Map.Entry<String, List<CRPipeline>> crPipelineGroup : pipesByGroup.entrySet()) {
            BasicPipelineConfigs pipelineConfigs = toBasicPipelineConfigs(crPipelineGroup, context);
            partialConfig.getGroups().add(pipelineConfigs);
        }
        partialConfig.setScms(newSCMs);
        return partialConfig;
    }

    public Map<String, List<CRPipeline>> groupPipelinesByGroupName(Collection<CRPipeline> pipelines) {
        Map<String, List<CRPipeline>> map = new HashMap<>();
        for (CRPipeline pipe : pipelines) {
            String key = pipe.getGroup();
            if (map.get(key) == null) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(pipe);
        }
        return map;
    }

    public BasicPipelineConfigs toBasicPipelineConfigs(Map.Entry<String, List<CRPipeline>> crPipelineGroup, PartialConfigLoadContext context) {
        String name = crPipelineGroup.getKey();
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs();
        pipelineConfigs.setGroup(name);
        for (CRPipeline crPipeline : crPipelineGroup.getValue()) {
            pipelineConfigs.add(toPipelineConfig(crPipeline, context));
        }
        return pipelineConfigs;
    }

    public BasicEnvironmentConfig toEnvironmentConfig(CREnvironment crEnvironment) {
        BasicEnvironmentConfig basicEnvironmentConfig =
                new BasicEnvironmentConfig(new CaseInsensitiveString(crEnvironment.getName()));
        for (String pipeline : crEnvironment.getPipelines()) {
            basicEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipeline));
        }
        for (String agent : crEnvironment.getAgents()) {
            basicEnvironmentConfig.addAgent(agent);
        }
        for (CREnvironmentVariable var : crEnvironment.getEnvironmentVariables()) {
            basicEnvironmentConfig.getVariables().add(toEnvironmentVariableConfig(var));
        }

        return basicEnvironmentConfig;
    }

    public EnvironmentVariableConfig toEnvironmentVariableConfig(CREnvironmentVariable crEnvironmentVariable) {
        if (crEnvironmentVariable.hasEncryptedValue()) {
            // encrypted value is not null or empty string
            return new EnvironmentVariableConfig(cipher, crEnvironmentVariable.getName(), crEnvironmentVariable.getEncryptedValue());
        } else if (!crEnvironmentVariable.hasValue() && "".equals(crEnvironmentVariable.getEncryptedValue())) {
            // encrypted value is an empty string - user wants an empty, but secure value, possibly to override at trigger-time
            String encryptedValue = null;
            try {
                encryptedValue = cipher.encrypt("");
            } catch (CryptoException e) {
                throw new RuntimeException("Encryption of empty secure variable failed", e);
            }
            return new EnvironmentVariableConfig(cipher, crEnvironmentVariable.getName(), encryptedValue);
        } else {
            String value = crEnvironmentVariable.getValue();
            if (StringUtils.isBlank(value))
                value = "";
            return new EnvironmentVariableConfig(crEnvironmentVariable.getName(), value);
        }
    }

    public PluggableTask toPluggableTask(CRPluggableTask pluggableTask) {
        PluginConfiguration pluginConfiguration = toPluginConfiguration(pluggableTask.getPluginConfiguration());
        Configuration configuration = toConfiguration(pluggableTask.getConfiguration());
        PluggableTask task = new PluggableTask(pluginConfiguration, configuration);
        setCommonTaskMembers(task, pluggableTask);
        return task;
    }

    private void setCommonTaskMembers(AbstractTask task, CRTask crTask) {
        CRTask crTaskOnCancel = crTask.getOnCancel();
        if (crTaskOnCancel != null)
            task.setCancelTask(toAbstractTask(crTaskOnCancel));
        task.runIfConfigs = toRunIfConfigs(crTask.getRunIf());
    }

    private RunIfConfigs toRunIfConfigs(CRRunIf runIf) {
        if (runIf == null)
            return new RunIfConfigs(RunIfConfig.PASSED);

        switch (runIf) {
            case any:
                return new RunIfConfigs(RunIfConfig.ANY);
            case passed:
                return new RunIfConfigs(RunIfConfig.PASSED);
            case failed:
                return new RunIfConfigs(RunIfConfig.FAILED);
            default:
                throw new RuntimeException(
                        String.format("unknown run if condition '%s'", runIf));
        }
    }

    public AbstractTask toAbstractTask(CRTask crTask) {
        if (crTask == null)
            throw new ConfigConvertionException("task cannot be null");

        if (crTask instanceof CRPluggableTask)
            return toPluggableTask((CRPluggableTask) crTask);
        else if (crTask instanceof CRBuildTask) {
            return toBuildTask((CRBuildTask) crTask);
        } else if (crTask instanceof CRExecTask) {
            return toExecTask((CRExecTask) crTask);
        } else if (crTask instanceof CRFetchArtifactTask) {
            return toFetchTask((CRFetchArtifactTask) crTask);
        } else if (crTask instanceof CRFetchPluggableArtifactTask) {
            return toFetchPluggableArtifactTask((CRFetchPluggableArtifactTask) crTask);
        } else
            throw new RuntimeException(
                    String.format("unknown type of task '%s'", crTask));
    }

    public FetchPluggableArtifactTask toFetchPluggableArtifactTask(CRFetchPluggableArtifactTask crTask) {
        Configuration configuration = toConfiguration(crTask.getConfiguration());
        FetchPluggableArtifactTask fetchPluggableArtifactTask = new FetchPluggableArtifactTask(new CaseInsensitiveString(crTask.getPipeline() == null ? "" : crTask.getPipeline()),
                new CaseInsensitiveString(crTask.getStage()),
                new CaseInsensitiveString(crTask.getJob()), crTask.getArtifactId(), configuration);
        setCommonTaskMembers(fetchPluggableArtifactTask, crTask);
        return fetchPluggableArtifactTask;
    }

    public FetchTask toFetchTask(CRFetchArtifactTask crTask) {
        FetchTask fetchTask = new FetchTask(
                new CaseInsensitiveString(crTask.getPipeline() == null ? "" : crTask.getPipeline()),
                new CaseInsensitiveString(crTask.getStage()),
                new CaseInsensitiveString(crTask.getJob()),
                crTask.getSource(),
                crTask.getDestination());

        if (crTask.sourceIsDirectory()) {
            fetchTask.setSrcdir(crTask.getSource());
            fetchTask.setSrcfile(null);
        }
        setCommonTaskMembers(fetchTask, crTask);
        return fetchTask;
    }

    public ExecTask toExecTask(CRExecTask crTask) {
        ExecTask execTask = new ExecTask(crTask.getCommand(), toArgList(crTask.getArguments()), crTask.getWorkingDirectory());
        execTask.setTimeout(crTask.getTimeout());

        setCommonTaskMembers(execTask, crTask);
        return execTask;
    }

    private Arguments toArgList(List<String> args) {
        Arguments arguments = new Arguments();
        if (args != null)
            for (String arg : args) {
                arguments.add(new Argument(arg));
            }
        return arguments;
    }

    public BuildTask toBuildTask(CRBuildTask crBuildTask) {
        BuildTask buildTask;
        switch (crBuildTask.getType()) {
            case rake:
                buildTask = new RakeTask();
                break;
            case ant:
                buildTask = new AntTask();
                break;
            case nant:
                buildTask = new NantTask();
                break;
            default:
                throw new RuntimeException(
                        String.format("unknown type of build task '%s'", crBuildTask.getType()));
        }
        setCommonBuildTaskMembers(buildTask, crBuildTask);
        setCommonTaskMembers(buildTask, crBuildTask);
        return buildTask;
    }

    private void setCommonBuildTaskMembers(BuildTask buildTask, CRBuildTask crBuildTask) {
        buildTask.buildFile = crBuildTask.getBuildFile();
        buildTask.target = crBuildTask.getTarget();
        buildTask.workingDirectory = crBuildTask.getWorkingDirectory();
    }

    private Configuration toConfiguration(Collection<CRConfigurationProperty> properties) {
        Configuration configuration = new Configuration();
        if (properties != null) {
            for (CRConfigurationProperty p : properties) {
                if (p.getValue() != null)
                    configuration.addNewConfigurationWithValue(p.getKey(), p.getValue(), false);
                else
                    configuration.addNewConfigurationWithValue(p.getKey(), p.getEncryptedValue(), true);
            }
        }
        return configuration;
    }

    public PluginConfiguration toPluginConfiguration(CRPluginConfiguration pluginConfiguration) {
        return new PluginConfiguration(pluginConfiguration.getId(), pluginConfiguration.getVersion());
    }

    public DependencyMaterialConfig toDependencyMaterialConfig(CRDependencyMaterial crDependencyMaterial) {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(
                new CaseInsensitiveString(crDependencyMaterial.getPipeline()),
                new CaseInsensitiveString(crDependencyMaterial.getStage()));
        setCommonMaterialMembers(dependencyMaterialConfig, crDependencyMaterial);
        return dependencyMaterialConfig;
    }

    private void setCommonMaterialMembers(AbstractMaterialConfig materialConfig, CRMaterial crMaterial) {
        materialConfig.setName(toMaterialName(crMaterial.getName()));
    }

    public MaterialConfig toMaterialConfig(CRMaterial crMaterial, PartialConfigLoadContext context) {
        if (crMaterial == null)
            throw new ConfigConvertionException("material cannot be null");

        if (crMaterial instanceof CRDependencyMaterial)
            return toDependencyMaterialConfig((CRDependencyMaterial) crMaterial);
        else if (crMaterial instanceof CRScmMaterial) {
            CRScmMaterial crScmMaterial = (CRScmMaterial) crMaterial;
            return toScmMaterialConfig(crScmMaterial);
        } else if (crMaterial instanceof CRPluggableScmMaterial) {
            CRPluggableScmMaterial crPluggableScmMaterial = (CRPluggableScmMaterial) crMaterial;
            return toPluggableScmMaterialConfig(crPluggableScmMaterial);
        } else if (crMaterial instanceof CRPackageMaterial) {
            CRPackageMaterial crPackageMaterial = (CRPackageMaterial) crMaterial;
            return toPackageMaterial(crPackageMaterial);
        } else if (crMaterial instanceof CRConfigMaterial) {
            CRConfigMaterial crConfigMaterial = (CRConfigMaterial) crMaterial;
            MaterialConfig repoMaterial = cloner.deepClone(context.configMaterial());
            if (StringUtils.isNotEmpty(crConfigMaterial.getName()))
                repoMaterial.setName(new CaseInsensitiveString(crConfigMaterial.getName()));
            if (StringUtils.isNotEmpty(crConfigMaterial.getDestination()))
                setDestination(repoMaterial, crConfigMaterial.getDestination());
            if (crConfigMaterial.getFilter() != null && !crConfigMaterial.getFilter().isEmpty()) {
                if (repoMaterial instanceof ScmMaterialConfig) {
                    ScmMaterialConfig scmMaterialConfig = (ScmMaterialConfig) repoMaterial;
                    scmMaterialConfig.setFilter(toFilter(crConfigMaterial.getFilter().getList()));
                    scmMaterialConfig.setInvertFilter(crConfigMaterial.getFilter().isWhitelist());
                } else //must be a pluggable SCM
                {
                    PluggableSCMMaterialConfig pluggableSCMMaterial = (PluggableSCMMaterialConfig) repoMaterial;
                    pluggableSCMMaterial.setFilter(toFilter(crConfigMaterial.getFilter().getList()));
                    if (crConfigMaterial.getFilter().isWhitelist())
                        throw new ConfigConvertionException("Plugable SCMs do not support whitelisting");
                }
            }
            return repoMaterial;
        } else
            throw new ConfigConvertionException(
                    String.format("unknown material type '%s'", crMaterial));
    }

    private void setDestination(MaterialConfig repoMaterial, String destination) {
        if (repoMaterial instanceof ScmMaterialConfig) {
            ((ScmMaterialConfig) repoMaterial).setFolder(destination);
        } else if (repoMaterial instanceof PluggableSCMMaterialConfig) {
            ((PluggableSCMMaterialConfig) repoMaterial).setFolder(destination);
        } else
            LOGGER.warn("Unknown material type {}", repoMaterial.getTypeForDisplay());
    }

    public PackageMaterialConfig toPackageMaterial(CRPackageMaterial crPackageMaterial) {
        PackageDefinition packageDefinition = getPackageDefinition(crPackageMaterial.getPackageId());
        return new PackageMaterialConfig(toMaterialName(crPackageMaterial.getName()), crPackageMaterial.getPackageId(), packageDefinition);
    }

    private PackageDefinition getPackageDefinition(String packageId) {
        PackageRepository packageRepositoryHaving = this.cachedGoConfig.currentConfig().getPackageRepositories().findPackageRepositoryHaving(packageId);
        if (packageRepositoryHaving == null)
            throw new ConfigConvertionException(
                    String.format("Failed to find package repository with package id '%s'", packageId));
        return packageRepositoryHaving.findPackage(packageId);
    }

    private PluggableSCMMaterialConfig toPluggableScmMaterialConfig(CRPluggableScmMaterial crPluggableScmMaterial) {
        String id = crPluggableScmMaterial.getScmId();
        CRPluginConfiguration pluginConfig = crPluggableScmMaterial.getPluginConfiguration();
        SCM scmConfig;

        if (pluginConfig == null) {
            scmConfig = getSCMs().find(id);
        } else {
            scmConfig = new SCM(id, toPluginConfiguration(pluginConfig), toConfiguration(crPluggableScmMaterial.getConfiguration()));
            scmConfig.ensureIdExists();
            scmConfig.setName(crPluggableScmMaterial.getName());
            if (getSCMs().findDuplicate(scmConfig) == null) {
                SCM duplicate = newSCMs.findDuplicate(scmConfig);
                if (duplicate == null) {
                    newSCMs.add(scmConfig);
                } else {
                    scmConfig = duplicate;
                }
            } else {
                scmConfig = getSCMs().findDuplicate(scmConfig);
            }
        }

        if (scmConfig == null)
            throw new ConfigConvertionException(
                    String.format("Failed to find referenced scm '%s'", id));

        return new PluggableSCMMaterialConfig(toMaterialName(crPluggableScmMaterial.getName()),
                scmConfig, crPluggableScmMaterial.getDestination(),
                toFilter(crPluggableScmMaterial.getFilterList()));
    }

    private SCMs getSCMs() {
        return this.cachedGoConfig.currentConfig().getSCMs();
    }

    private ScmMaterialConfig toScmMaterialConfig(CRScmMaterial crScmMaterial) {
        String materialName = crScmMaterial.getName();
        if (crScmMaterial instanceof CRGitMaterial) {
            CRGitMaterial git = (CRGitMaterial) crScmMaterial;
            String gitBranch = git.getBranch();
            if (StringUtils.isBlank(gitBranch))
                gitBranch = GitMaterialConfig.DEFAULT_BRANCH;
            GitMaterialConfig gitConfig = new GitMaterialConfig(git.getUrl(), gitBranch, git.isShallowClone());
            setCommonMaterialMembers(gitConfig, crScmMaterial);
            setCommonScmMaterialMembers(gitConfig, git);
            return gitConfig;
        } else if (crScmMaterial instanceof CRHgMaterial) {
            CRHgMaterial hg = (CRHgMaterial) crScmMaterial;
            HgMaterialConfig hgConfig = new HgMaterialConfig(new HgUrlArgument(hg.getUrl()),
                    hg.getUsername(), hg.getPassword(), hg.getBranch(), hg.isAutoUpdate(), toFilter(crScmMaterial), false, hg.getDestination(),
                    toMaterialName(materialName));
            setCommonMaterialMembers(hgConfig, crScmMaterial);
            setCommonScmMaterialMembers(hgConfig, hg);
            return hgConfig;
        } else if (crScmMaterial instanceof CRP4Material) {
            CRP4Material crp4Material = (CRP4Material) crScmMaterial;
            P4MaterialConfig p4MaterialConfig = new P4MaterialConfig(crp4Material.getPort(), crp4Material.getView(), cipher);
            p4MaterialConfig.setUseTickets(crp4Material.isUseTickets());
            setCommonMaterialMembers(p4MaterialConfig, crScmMaterial);
            setCommonScmMaterialMembers(p4MaterialConfig, crp4Material);
            return p4MaterialConfig;
        } else if (crScmMaterial instanceof CRSvnMaterial) {
            CRSvnMaterial crSvnMaterial = (CRSvnMaterial) crScmMaterial;
            SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig(
                    crSvnMaterial.getUrl(), crSvnMaterial.getUsername(), crSvnMaterial.isCheckExternals(), cipher);
            setCommonMaterialMembers(svnMaterialConfig, crScmMaterial);
            setCommonScmMaterialMembers(svnMaterialConfig, crSvnMaterial);
            return svnMaterialConfig;
        } else if (crScmMaterial instanceof CRTfsMaterial) {
            CRTfsMaterial crTfsMaterial = (CRTfsMaterial) crScmMaterial;
            TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(cipher,
                    new UrlArgument(crTfsMaterial.getUrl()),
                    crTfsMaterial.getUsername(),
                    crTfsMaterial.getDomain(),
                    crTfsMaterial.getProject());
            setCommonMaterialMembers(tfsMaterialConfig, crTfsMaterial);
            setCommonScmMaterialMembers(tfsMaterialConfig, crTfsMaterial);
            return tfsMaterialConfig;
        } else
            throw new ConfigConvertionException(
                    String.format("unknown scm material type '%s'", crScmMaterial));
    }

    private CaseInsensitiveString toMaterialName(String materialName) {
        if (StringUtils.isBlank(materialName))
            return null;
        return new CaseInsensitiveString(materialName);
    }

    private void setCommonScmMaterialMembers(ScmMaterialConfig scmMaterialConfig, CRScmMaterial crScmMaterial) {
        scmMaterialConfig.setFolder(crScmMaterial.getDestination());
        scmMaterialConfig.setAutoUpdate(crScmMaterial.isAutoUpdate());
        scmMaterialConfig.setFilter(toFilter(crScmMaterial));
        scmMaterialConfig.setInvertFilter(crScmMaterial.isWhitelist());

        scmMaterialConfig.setUserName(crScmMaterial.getUsername());

        if (crScmMaterial.getEncryptedPassword() != null) {
            scmMaterialConfig.setEncryptedPassword(crScmMaterial.getEncryptedPassword());
        } else {
            scmMaterialConfig.setPassword(crScmMaterial.getPassword());
        }
    }

    private Filter toFilter(CRScmMaterial crScmMaterial) {
        List<String> filterList = crScmMaterial.getFilterList();
        return toFilter(filterList);
    }

    private Filter toFilter(List<String> filterList) {
        Filter filter = new Filter();
        if (filterList == null)
            return filter;
        for (String pattern : filterList) {
            filter.add(new IgnoredFiles(pattern));
        }
        return filter;
    }

    public JobConfig toJobConfig(CRJob crJob) {
        JobConfig jobConfig = new JobConfig(crJob.getName());
        if (crJob.getEnvironmentVariables() != null)
            for (CREnvironmentVariable crEnvironmentVariable : crJob.getEnvironmentVariables()) {
                jobConfig.getVariables().add(toEnvironmentVariableConfig(crEnvironmentVariable));
            }

        List<CRTask> crTasks = crJob.getTasks();
        Tasks tasks = jobConfig.getTasks();
        if (crTasks != null)
            for (CRTask crTask : crTasks) {
                tasks.add(toAbstractTask(crTask));
            }

        Tabs tabs = jobConfig.getTabs();
        if (crJob.getTabs() != null)
            for (CRTab crTab : crJob.getTabs()) {
                tabs.add(toTab(crTab));
            }

        ResourceConfigs resourceConfigs = jobConfig.resourceConfigs();
        if (crJob.getResources() != null)
            for (String crResource : crJob.getResources()) {
                resourceConfigs.add(new ResourceConfig(crResource));
            }

        if (crJob.getElasticProfileId() != null)
            jobConfig.setElasticProfileId(crJob.getElasticProfileId());

        ArtifactConfigs artifactConfigs = jobConfig.artifactConfigs();
        if (crJob.getArtifacts() != null) {
            for (CRArtifact crArtifact : crJob.getArtifacts()) {
                artifactConfigs.add(toArtifactConfig(crArtifact));
            }
        }

        ArtifactPropertiesConfig artifactPropertiesConfig = jobConfig.getProperties();
        if (crJob.getProperties() != null)
            for (CRPropertyGenerator crPropertyGenerator : crJob.getProperties()) {
                artifactPropertiesConfig.add(new ArtifactPropertyConfig(
                        crPropertyGenerator.getName(), crPropertyGenerator.getSource(), crPropertyGenerator.getXpath()));
            }

        if (crJob.isRunOnAllAgents())
            jobConfig.setRunOnAllAgents(true);
        else {
            Integer count = crJob.getRunInstanceCount();
            if (count != null)
                jobConfig.setRunInstanceCount(count);
            // else null - meaning simple job
        }

        if (crJob.getTimeout() != 0)
            jobConfig.setTimeout(Integer.toString(crJob.getTimeout()));
        //else null - means default server-wide timeout

        return jobConfig;
    }

    public ArtifactConfig toArtifactConfig(CRArtifact crArtifact) {
        switch (crArtifact.getType()) {
            case build:
                CRBuiltInArtifact crBuildArtifact = (CRBuiltInArtifact) crArtifact;
                return new BuildArtifactConfig(crBuildArtifact.getSource(), crBuildArtifact.getDestination());
            case test:
                CRBuiltInArtifact crTestArtifact = (CRBuiltInArtifact) crArtifact;
                return new TestArtifactConfig(crTestArtifact.getSource(), crTestArtifact.getDestination());
            case external:
                CRPluggableArtifact crPluggableArtifact = (CRPluggableArtifact) crArtifact;
                Configuration configuration = toConfiguration(crPluggableArtifact.getConfiguration());
                ConfigurationProperty[] configProperties = new ConfigurationProperty[configuration.size()];
                return new PluggableArtifactConfig(crPluggableArtifact.getId(), crPluggableArtifact.getStoreId(), configuration.toArray(configProperties));

            default:
                throw new RuntimeException(String.format("Unsupported CR Artifact Type: %s.", crArtifact.getType()));
        }
    }

    private Tab toTab(CRTab crTab) {
        return new Tab(crTab.getName(), crTab.getPath());
    }

    public StageConfig toStage(CRStage crStage) {
        Approval approval = toApproval(crStage.getApproval());
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString(crStage.getName()), crStage.isFetchMaterials(),
                crStage.isCleanWorkingDirectory(), approval, crStage.isNeverCleanupArtifacts(), toJobConfigs(crStage.getJobs()));
        EnvironmentVariablesConfig environmentVariableConfigs = stageConfig.getVariables();
        for (CREnvironmentVariable crEnvironmentVariable : crStage.getEnvironmentVariables()) {
            environmentVariableConfigs.add(toEnvironmentVariableConfig(crEnvironmentVariable));
        }
        return stageConfig;
    }

    public Approval toApproval(CRApproval crApproval) {
        if (crApproval == null)
            return Approval.automaticApproval();

        Approval approval;
        if (crApproval.getType() == CRApprovalCondition.manual)
            approval = Approval.manualApproval();
        else
            approval = Approval.automaticApproval();

        AuthConfig authConfig = approval.getAuthConfig();
        for (String user : crApproval.getUsers()) {
            authConfig.add(new AdminUser(new CaseInsensitiveString(user)));
        }
        for (String user : crApproval.getRoles()) {
            authConfig.add(new AdminRole(new CaseInsensitiveString(user)));
        }

        return approval;
    }

    private JobConfigs toJobConfigs(Collection<CRJob> jobs) {
        JobConfigs jobConfigs = new JobConfigs();
        for (CRJob crJob : jobs) {
            jobConfigs.add(toJobConfig(crJob));
        }
        return jobConfigs;
    }

    public PipelineConfig toPipelineConfig(CRPipeline crPipeline, PartialConfigLoadContext context) {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        for (CRMaterial crMaterial : crPipeline.getMaterials()) {
            materialConfigs.add(toMaterialConfig(crMaterial, context));
        }

        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(crPipeline.getName()), materialConfigs);

        if (crPipeline.hasTemplate()) {
            pipelineConfig.setTemplateName(new CaseInsensitiveString(crPipeline.getTemplate()));
        } else {
            for (CRStage crStage : crPipeline.getStages()) {
                pipelineConfig.add(toStage(crStage));
            }
        }

        if (crPipeline.getLabelTemplate() != null)
            pipelineConfig.setLabelTemplate(crPipeline.getLabelTemplate());

        CRTrackingTool crTrackingTool = crPipeline.getTrackingTool();
        if (crTrackingTool != null) {
            pipelineConfig.setTrackingTool(toTrackingTool(crTrackingTool));
        }
        CRMingle crMingle = crPipeline.getMingle();
        if (crMingle != null) {
            pipelineConfig.setMingleConfig(toMingleConfig(crMingle));
        }

        CRTimer crTimer = crPipeline.getTimer();
        if (crTimer != null) {
            pipelineConfig.setTimer(toTimerConfig(crTimer));
        }

        EnvironmentVariablesConfig variables = pipelineConfig.getVariables();
        for (CREnvironmentVariable crEnvironmentVariable : crPipeline.getEnvironmentVariables()) {
            variables.add(toEnvironmentVariableConfig(crEnvironmentVariable));
        }

        ParamsConfig params = pipelineConfig.getParams();
        for (CRParameter crParameter : crPipeline.getParameters()) {
            params.add(toParamConfig(crParameter));
        }

        pipelineConfig.setLockBehaviorIfNecessary(crPipeline.getLockBehavior());
        pipelineConfig.setDisplayOrderWeight(crPipeline.getDisplayOrderWeight());

        return pipelineConfig;
    }

    private ParamConfig toParamConfig(CRParameter crParameter) {
        return new ParamConfig(crParameter.getName(), crParameter.getValue());
    }

    public TimerConfig toTimerConfig(CRTimer crTimer) {
        String spec = crTimer.getSpec();
        if (StringUtils.isBlank(spec))
            throw new RuntimeException("timer schedule is not specified");
        return new TimerConfig(spec, crTimer.isOnlyOnChanges());
    }

    private MingleConfig toMingleConfig(CRMingle crMingle) {
        return new MingleConfig(crMingle.getBaseUrl(), crMingle.getProjectIdentifier(), crMingle.getMqlGroupingConditions());
    }

    private TrackingTool toTrackingTool(CRTrackingTool crTrackingTool) {
        return new TrackingTool(crTrackingTool.getLink(), crTrackingTool.getRegex());
    }

    CRPipeline pipelineConfigToCRPipeline(PipelineConfig pipelineConfig, String groupName) {
        CRPipeline crPipeline = new CRPipeline();
        crPipeline.setGroup(groupName);

        crPipeline.setName(pipelineConfig.name().toString());
        for (StageConfig stage : pipelineConfig.getStages()) {
            crPipeline.addStage(stageToCRStage(stage));
        }

        for (ParamConfig param : pipelineConfig.getParams()) {
            crPipeline.addParameter(paramToCRParam(param));
        }

        for (MaterialConfig material : pipelineConfig.materialConfigs()) {
            crPipeline.addMaterial(materialToCRMaterial(material));
        }

        for (EnvironmentVariableConfig envVar : pipelineConfig.getVariables()) {
            crPipeline.addEnvironmentVariable(environmentVariableConfigToCREnvironmentVariable(envVar));
        }

        if (pipelineConfig.getTemplateName() != null)
            crPipeline.setTemplate(pipelineConfig.getTemplateName().toString());

        crPipeline.setTrackingTool(trackingToolToCRTrackingTool(pipelineConfig.getTrackingTool()));
        crPipeline.setTimer(timerConfigToCRTimer(pipelineConfig.getTimer()));
        crPipeline.setLockBehavior(pipelineConfig.getLockBehavior());

        if (pipelineConfig.getMingleConfig().isDefined()) {
            crPipeline.setMingle(mingleToCRMingle(pipelineConfig.getMingleConfig()));
        }

        crPipeline.setLabelTemplate(pipelineConfig.getLabelTemplate());
        crPipeline.setDisplayOrderWeight(pipelineConfig.getDisplayOrderWeight());

        return crPipeline;
    }

    CRStage stageToCRStage(StageConfig stageConfig) {
        CRStage crStage = new CRStage(stageConfig.name().toString());

        for (JobConfig job : stageConfig.getJobs()) {
            crStage.addJob(jobToCRJob(job));
        }

        for (EnvironmentVariableConfig var : stageConfig.getVariables()) {
            crStage.addEnvironmentVariable(environmentVariableConfigToCREnvironmentVariable(var));
        }

        crStage.setApproval(approvalToCRApproval(stageConfig.getApproval()));
        crStage.setFetchMaterials(stageConfig.isFetchMaterials());
        crStage.setNeverCleanupArtifacts(stageConfig.isArtifactCleanupProhibited());
        crStage.setCleanWorkingDirectory(stageConfig.isCleanWorkingDir());

        return crStage;
    }

    private CRApproval approvalToCRApproval(Approval approval) {
        CRApproval crApproval = new CRApproval();
        for (AdminUser user : approval.getAuthConfig().getUsers()) {
            crApproval.addAuthorizedUser(user.getName().toString());
        }

        for (AdminRole role : approval.getAuthConfig().getRoles()) {
            crApproval.addAuthorizedRole(role.getName().toString());
        }

        if (approval.getType().equals(Approval.SUCCESS)) {
            crApproval.setApprovalCondition(CRApprovalCondition.success);
        } else {
            crApproval.setApprovalCondition(CRApprovalCondition.manual);

        }

        return crApproval;
    }

    CRJob jobToCRJob(JobConfig jobConfig) {
        CRJob job = new CRJob();
        job.setName(jobConfig.name().toString());
        job.setResources(jobConfig.resourceConfigs().resourceNames());
        job.setElasticProfileId(jobConfig.getElasticProfileId());

        for (EnvironmentVariableConfig var : jobConfig.getVariables()) {
            job.addEnvironmentVariable(environmentVariableConfigToCREnvironmentVariable(var));
        }

        for (ArtifactPropertyConfig prop : jobConfig.getProperties()) {
            job.addProperty(new CRPropertyGenerator(prop.getName(), prop.getSrc(), prop.getXpath()));
        }

        for (Tab tab : jobConfig.getTabs()) {
            job.addTab(new CRTab(tab.getName(), tab.getPath()));
        }

        for (ArtifactConfig artifactConfig : jobConfig.artifactConfigs()) {
            job.addArtifact(artifactConfigToCRArtifact(artifactConfig));
        }

        if (jobConfig.isRunOnAllAgents()) {
            job.setRunOnAllAgents(jobConfig.isRunOnAllAgents());
        } else {
            job.setRunInstanceCount(jobConfig.getRunInstanceCountValue());
        }

        for (Task task : jobConfig.tasks()) {
            job.addTask(taskToCRTask(task));
        }

        if (jobConfig.getTimeout() != null) {
            job.setTimeout(Integer.valueOf(jobConfig.getTimeout()));
        }

        return job;
    }

    CRTask taskToCRTask(Task task) {
        if (task == null)
            throw new ConfigConvertionException("task cannot be null");

        if (task instanceof PluggableTask)
            return pluggableTaskToCRPluggableTask((PluggableTask) task);
        else if (task instanceof BuildTask) {
            return buildTaskToCRBuildTask((BuildTask) task);
        } else if (task instanceof ExecTask) {
            return execTasktoCRExecTask((ExecTask) task);
        } else if (task instanceof FetchTask) {
            return fetchTaskToCRFetchTask((FetchTask) task);
        } else if (task instanceof FetchPluggableArtifactTask) {
            return fetchPluggableArtifactTaskToCRFetchPluggableTask((FetchPluggableArtifactTask) task);
        } else
            throw new RuntimeException(
                    String.format("unknown type of task '%s'", task));
    }

    private CRFetchPluggableArtifactTask fetchPluggableArtifactTaskToCRFetchPluggableTask(FetchPluggableArtifactTask task) {
        List<CRConfigurationProperty> configuration = configurationToCRConfiguration(task.getConfiguration());
        CRFetchPluggableArtifactTask crTask = new CRFetchPluggableArtifactTask(
                null, null, null, task.getStage().toString(),
                task.getJob().toString(), task.getArtifactId(), configuration);
        crTask.setPipeline(task.getPipelineName().toString());
        commonCRTaskMembers(crTask, task);
        return crTask;
    }

    private CRFetchArtifactTask fetchTaskToCRFetchTask(FetchTask task) {
        CRFetchArtifactTask fetchTask = new CRFetchArtifactTask(null, null, null, task.getStage().toString(), task.getJob().toString(), task.getSrc(), null, false);

        fetchTask.setDestination(task.getDest());
        fetchTask.setPipeline(task.getPipelineName().toString());
        fetchTask.setSourceIsDirectory(!task.isSourceAFile());

        commonCRTaskMembers(fetchTask, task);
        return fetchTask;
    }

    private CRPluggableTask pluggableTaskToCRPluggableTask(PluggableTask pluggableTask) {
        CRPluginConfiguration pluginConfiguration = new CRPluginConfiguration(pluggableTask.getPluginConfiguration().getId(), pluggableTask.getPluginConfiguration().getVersion());
        List<CRConfigurationProperty> configuration = configurationToCRConfiguration(pluggableTask.getConfiguration());
        CRPluggableTask task = new CRPluggableTask(null, null, pluginConfiguration, configuration);
        commonCRTaskMembers(task, pluggableTask);
        return task;
    }

    private CRExecTask execTasktoCRExecTask(ExecTask task) {
        CRExecTask crExecTask = new CRExecTask(null, null, task.getCommand(), null, 0);
        crExecTask.setTimeout(task.getTimeout());
        crExecTask.setWorkingDirectory(task.workingDirectory());

        Arguments arguments;
        if (task.getArgs().isEmpty()) {
            arguments = task.getArgList();
        } else {
            arguments = new Arguments();
            for (String arg : CommandLine.translateCommandLine(task.getArgs())) {
                arguments.add(new Argument(arg));
            }
        }

        crExecTask.setArguments(arguments.stream()
                .map(Argument::getValue)
                .collect(Collectors.toList())
        );

        commonCRTaskMembers(crExecTask, task);
        return crExecTask;
    }

    private CRBuildTask buildTaskToCRBuildTask(BuildTask buildTask) {
        CRBuildTask crBuildTask;
        if (buildTask instanceof RakeTask) {
            crBuildTask = CRBuildTask.rake();
        } else if (buildTask instanceof AntTask) {
            crBuildTask = CRBuildTask.ant();
        } else if (buildTask instanceof NantTask) {
            crBuildTask = CRBuildTask.nant(((NantTask) buildTask).getNantPath());
        } else {
            throw new RuntimeException(
                    String.format("unknown type of build task '%s'", buildTask));
        }
        crBuildTask.setBuildFile(buildTask.getBuildFile());
        crBuildTask.setTarget(buildTask.getTarget());
        crBuildTask.setWorkingDirectory(buildTask.workingDirectory());
        commonCRTaskMembers(crBuildTask, buildTask);
        return crBuildTask;
    }

    private void commonCRTaskMembers(CRTask crTask, AbstractTask task) {
        Task taskOnCancel = task.cancelTask();
        if (taskOnCancel != null && !(taskOnCancel instanceof KillAllChildProcessTask))
            crTask.setOnCancel(taskToCRTask(taskOnCancel));
        crTask.setRunIf(crRunIfs(task.runIfConfigs));
    }

    private CRRunIf crRunIfs(RunIfConfigs runIfs) {
        if (runIfs == null || runIfs.isEmpty())
            return CRRunIf.passed;
        RunIfConfig runIf = runIfs.first();
        if (runIf.equals(RunIfConfig.ANY)) {
            return CRRunIf.any;
        } else if (runIf.equals(RunIfConfig.PASSED)) {
            return CRRunIf.passed;
        } else if (runIf.equals(RunIfConfig.FAILED)) {
            return CRRunIf.failed;
        } else {
            throw new RuntimeException(
                    String.format("unknown run if condition '%s'", runIf));
        }
    }

    private List<CRConfigurationProperty> configurationToCRConfiguration(Configuration config) {
        ArrayList<CRConfigurationProperty> properties = new ArrayList();
        if (config != null) {
            for (ConfigurationProperty p : config) {
                CRConfigurationProperty crProp = new CRConfigurationProperty(p.getKey().getName());
                if (p.isSecure())
                    crProp.setEncryptedValue(p.getEncryptedValue());
                else
                    crProp.setValue(p.getValue());
                properties.add(crProp);
            }
        }
        return properties;
    }

    private CRArtifact artifactConfigToCRArtifact(ArtifactConfig artifactConfig) {
        if (artifactConfig instanceof BuildArtifactConfig) {
            BuildArtifactConfig buildArtifact = (BuildArtifactConfig) artifactConfig;
            return new CRBuiltInArtifact(buildArtifact.getSource(), buildArtifact.getDestination(), CRArtifactType.build);
        } else if (artifactConfig instanceof TestArtifactConfig) {
            TestArtifactConfig testArtifact = (TestArtifactConfig) artifactConfig;
            return new CRBuiltInArtifact(testArtifact.getSource(), testArtifact.getDestination(), CRArtifactType.test);
        } else if (artifactConfig instanceof PluggableArtifactConfig) {
            PluggableArtifactConfig pluggableArtifact = (PluggableArtifactConfig) artifactConfig;
            List<CRConfigurationProperty> crConfigurationProperties = configurationToCRConfiguration(pluggableArtifact.getConfiguration());
            return new CRPluggableArtifact(pluggableArtifact.getId(), pluggableArtifact.getStoreId(), crConfigurationProperties);
        } else {
            throw new RuntimeException(String.format("Unsupported Artifact Type: %s.", artifactConfig.getArtifactType()));
        }
    }

    private CRMingle mingleToCRMingle(MingleConfig mingleConfig) {
        CRMingle crMingle = new CRMingle();
        crMingle.setBaseUrl(mingleConfig.getBaseUrl());
        crMingle.setProjectIdentifier(mingleConfig.getProjectIdentifier());
        crMingle.setMqlGroupingConditions(mingleConfig.getQuotedMql());
        return crMingle;
    }

    private CRTrackingTool trackingToolToCRTrackingTool(TrackingTool trackingTool) {
        if (trackingTool == null)
            return null;
        return new CRTrackingTool(trackingTool.getLink(), trackingTool.getRegex());
    }

    private CRParameter paramToCRParam(ParamConfig paramConfig) {
        return new CRParameter(paramConfig.getName(), paramConfig.getValue());
    }

    private CRTimer timerConfigToCRTimer(TimerConfig timerConfig) {
        if (timerConfig == null)
            return null;
        String spec = timerConfig.getTimerSpec();
        if (StringUtils.isBlank(spec))
            throw new RuntimeException("timer schedule is not specified");
        return new CRTimer(spec, timerConfig.shouldTriggerOnlyOnChanges());
    }

    CREnvironmentVariable environmentVariableConfigToCREnvironmentVariable(EnvironmentVariableConfig environmentVariableConfig) {
        if (environmentVariableConfig.isSecure()) {
            return new CREnvironmentVariable(environmentVariableConfig.getName(), null, environmentVariableConfig.getEncryptedValue());
        } else {
            String value = environmentVariableConfig.getValue();
            if (StringUtils.isBlank(value))
                value = "";
            return new CREnvironmentVariable(environmentVariableConfig.getName(), value);
        }
    }

    private CRPluggableScmMaterial pluggableScmMaterialConfigToCRPluggableScmMaterial(PluggableSCMMaterialConfig pluggableScmMaterialConfig) {
        SCMs scms = getSCMs();
        String id = pluggableScmMaterialConfig.getScmId();
        SCM scmConfig = scms.find(id);
        if (scmConfig == null)
            throw new ConfigConvertionException(
                    String.format("Failed to find referenced scm '%s'", id));

        return new CRPluggableScmMaterial(pluggableScmMaterialConfig.getName().toString(),
                id, pluggableScmMaterialConfig.getFolder(),
                pluggableScmMaterialConfig.filter().ignoredFileNames());
    }

    private CRPackageMaterial packageMaterialToCRPackageMaterial(PackageMaterialConfig packageMaterialConfig) {
        return new CRPackageMaterial(packageMaterialConfig.getName().toString(), packageMaterialConfig.getPackageId());
    }

    private CRDependencyMaterial dependencyMaterialConfigToCRDependencyMaterial(DependencyMaterialConfig dependencyMaterialConfig) {
        CRDependencyMaterial crDependencyMaterial = new CRDependencyMaterial(
                dependencyMaterialConfig.getPipelineName().toString(),
                dependencyMaterialConfig.getStageName().toString());
        if (dependencyMaterialConfig.getName() != null)
            crDependencyMaterial.setName(dependencyMaterialConfig.getName().toString());
        return crDependencyMaterial;
    }

    private CRScmMaterial scmMaterialToCRScmMaterial(ScmMaterialConfig scmConfig) {
        String name = null;
        if (scmConfig.getName() != null) {
            name = scmConfig.getName().toString();
        }

        if (scmConfig instanceof GitMaterialConfig)
            return gitMaterialToCRGitMaterial(name, (GitMaterialConfig) scmConfig);

        else if (scmConfig instanceof HgMaterialConfig)
            return hgMaterialToCRHgMaterial(name, (HgMaterialConfig) scmConfig);

        else if (scmConfig instanceof P4MaterialConfig)
            return p4MaterialToCRP4Material(name, (P4MaterialConfig) scmConfig);

        else if (scmConfig instanceof SvnMaterialConfig)
            return svnMaterialToCRSvnMaterial(name, (SvnMaterialConfig) scmConfig);

        else if (scmConfig instanceof TfsMaterialConfig)
            return tfsMaterialToCRTfsMaterial(name, (TfsMaterialConfig) scmConfig);

        else
            throw new ConfigConvertionException(
                    String.format("unknown scm material type '%s'", scmConfig));
    }

    private CRHgMaterial hgMaterialToCRHgMaterial(String materialName, HgMaterialConfig hgMaterialConfig) {
        CRHgMaterial crHgMaterial = new CRHgMaterial(materialName, hgMaterialConfig.getFolder(), hgMaterialConfig.isAutoUpdate(), hgMaterialConfig.isInvertFilter(), hgMaterialConfig.getUserName(), hgMaterialConfig.filter().ignoredFileNames(), hgMaterialConfig.getUrl());
        crHgMaterial.setEncryptedPassword(hgMaterialConfig.getEncryptedPassword());
        return crHgMaterial;
    }

    private CRGitMaterial gitMaterialToCRGitMaterial(String materialName, GitMaterialConfig gitMaterialConfig) {
        CRGitMaterial crGitMaterial = new CRGitMaterial(materialName, gitMaterialConfig.getFolder(), gitMaterialConfig.isAutoUpdate(), gitMaterialConfig.isInvertFilter(), gitMaterialConfig.getUserName(), gitMaterialConfig.filter().ignoredFileNames(), gitMaterialConfig.getUrl(), gitMaterialConfig.getBranch(), gitMaterialConfig.isShallowClone());
        crGitMaterial.setEncryptedPassword(gitMaterialConfig.getEncryptedPassword());
        return crGitMaterial;

    }

    private CRP4Material p4MaterialToCRP4Material(String materialName, P4MaterialConfig p4MaterialConfig) {
        CRP4Material crP4Material = new CRP4Material(materialName, p4MaterialConfig.getFolder(), p4MaterialConfig.isAutoUpdate(), p4MaterialConfig.isInvertFilter(), p4MaterialConfig.getUsername(), p4MaterialConfig.filter().ignoredFileNames(), p4MaterialConfig.getServerAndPort(), p4MaterialConfig.getView(), p4MaterialConfig.getUseTickets());

        if (p4MaterialConfig.getEncryptedPassword() != null) {
            crP4Material.setEncryptedPassword(p4MaterialConfig.getEncryptedPassword());
        }

        return crP4Material;
    }

    private CRSvnMaterial svnMaterialToCRSvnMaterial(String materialName, SvnMaterialConfig svnMaterial) {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial(materialName, svnMaterial.getFolder(), svnMaterial.isAutoUpdate(), svnMaterial.isInvertFilter(), svnMaterial.getUsername(), svnMaterial.filter().ignoredFileNames(), svnMaterial.getUrl(), svnMaterial.isCheckExternals());
        crSvnMaterial.setEncryptedPassword(svnMaterial.getEncryptedPassword());
        return crSvnMaterial;
    }

    private CRTfsMaterial tfsMaterialToCRTfsMaterial(String materialName, TfsMaterialConfig tfsMaterialConfig) {
        CRTfsMaterial crTfsMaterial = new CRTfsMaterial(materialName,
                tfsMaterialConfig.getFolder(),
                tfsMaterialConfig.isAutoUpdate(),
                tfsMaterialConfig.isInvertFilter(), tfsMaterialConfig.getUsername(), tfsMaterialConfig.filter().ignoredFileNames(), tfsMaterialConfig.getUrl(),
                tfsMaterialConfig.getProjectPath(),
                tfsMaterialConfig.getDomain()
        );

        if (tfsMaterialConfig.getEncryptedPassword() != null) {
            crTfsMaterial.setEncryptedPassword(tfsMaterialConfig.getEncryptedPassword());
        }

        return crTfsMaterial;
    }

    CRMaterial materialToCRMaterial(MaterialConfig materialConfig) {
        if (materialConfig == null)
            throw new ConfigConvertionException("material cannot be null");

        if (materialConfig instanceof DependencyMaterialConfig) {
            return dependencyMaterialConfigToCRDependencyMaterial((DependencyMaterialConfig) materialConfig);
        } else if (materialConfig instanceof ScmMaterialConfig) {
            ScmMaterialConfig scmMaterialConfig = (ScmMaterialConfig) materialConfig;
            return scmMaterialToCRScmMaterial(scmMaterialConfig);
        } else if (materialConfig instanceof PluggableSCMMaterialConfig) {
            PluggableSCMMaterialConfig pluggableSCMMaterialConfig = (PluggableSCMMaterialConfig) materialConfig;
            return pluggableScmMaterialConfigToCRPluggableScmMaterial(pluggableSCMMaterialConfig);
        } else if (materialConfig instanceof PackageMaterialConfig) {
            PackageMaterialConfig packageMaterial = (PackageMaterialConfig) materialConfig;
            return packageMaterialToCRPackageMaterial(packageMaterial);
        } else {
            throw new ConfigConvertionException(
                    String.format("unknown material type '%s'", materialConfig));
        }
    }
}
