package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.plugin.access.configrepo.contract.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;

import java.util.Collection;
import java.util.List;

/**
 * Helper to transform config repo classes to config-api classes
 */
public class ConfigConverter {

    private final GoCipher cipher;
    private final CachedFileGoConfig cachedFileGoConfig;

    public ConfigConverter(GoCipher goCipher,CachedFileGoConfig cachedFileGoConfig)
    {
        this.cipher = goCipher;
        this.cachedFileGoConfig = cachedFileGoConfig;
    }

    public PartialConfig toPartialConfig(CRPartialConfig crPartialConfig) {
        PartialConfig partialConfig = new PartialConfig();
        for(CREnvironment crEnvironment : crPartialConfig.getEnvironments())
        {
            EnvironmentConfig environment = toEnvironmentConfig(crEnvironment);
            partialConfig.getEnvironments().add(environment);
        }
        for(CRPipelineGroup crPipelineGroup : crPartialConfig.getGroups())
        {
            BasicPipelineConfigs pipelineConfigs = toBasicPipelineConfigs(crPipelineGroup);
            partialConfig.getGroups().add(pipelineConfigs);
        }
        return partialConfig;
    }

    public BasicPipelineConfigs toBasicPipelineConfigs(CRPipelineGroup crPipelineGroup) {
        String name = crPipelineGroup.getName();
        BasicPipelineConfigs pipelineConfigs = new BasicPipelineConfigs();
        pipelineConfigs.setGroup(name);
        for(CRPipeline crPipeline : crPipelineGroup.getPipelines())
        {
            pipelineConfigs.add(toPipelineConfig(crPipeline));
        }
        return pipelineConfigs;
    }

    public BasicEnvironmentConfig toEnvironmentConfig(CREnvironment crEnvironment) {
        BasicEnvironmentConfig basicEnvironmentConfig =
                new BasicEnvironmentConfig(new CaseInsensitiveString(crEnvironment.getName()));
        for(String pipeline : crEnvironment.getPipelines())
        {
            basicEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipeline));
        }
        for(String agent : crEnvironment.getAgents())
        {
            basicEnvironmentConfig.addAgent(agent);
        }
        for(CREnvironmentVariable var : crEnvironment.getEnvironmentVariables())
        {
            basicEnvironmentConfig.getVariables().add(toEnvironmentVariableConfig(var));
        }

        return basicEnvironmentConfig;
    }

    public EnvironmentVariableConfig toEnvironmentVariableConfig(CREnvironmentVariable crEnvironmentVariable) {
        if(crEnvironmentVariable.hasEncryptedValue())
        {
            return new EnvironmentVariableConfig(cipher,crEnvironmentVariable.getName(),crEnvironmentVariable.getEncryptedValue());
        }
        else
        {
            return new EnvironmentVariableConfig(crEnvironmentVariable.getName(),crEnvironmentVariable.getValue());
        }
    }

    public PluggableTask toPluggableTask(CRPluggableTask pluggableTask) {
        PluginConfiguration pluginConfiguration = toPluginConfiguration(pluggableTask.getPluginConfiguration());
        Configuration configuration = toConfiguration(pluggableTask.getConfiguration());
        PluggableTask task = new PluggableTask(pluginConfiguration, configuration);
        setCommonTaskMembers(task,pluggableTask);
        return task;
    }

    private void setCommonTaskMembers(AbstractTask task, CRTask crTask) {
        CRTask crTaskOnCancel = crTask.getOnCancel();
        task.setCancelTask(crTaskOnCancel != null ? toAbstractTask(crTaskOnCancel) : null);
        task.runIfConfigs = toRunIfConfigs(crTask.getRunIf());
    }

    private RunIfConfigs toRunIfConfigs(CRRunIf runIf) {
        switch (runIf)
        {
            case any:
                return new RunIfConfigs(RunIfConfig.ANY);
            case passed:
                return new RunIfConfigs(RunIfConfig.PASSED);
            case failed:
                return new RunIfConfigs(RunIfConfig.FAILED);
            default:
                throw new RuntimeException(
                        String.format("unknown run if condition '%s'",runIf));
        }
    }

    public AbstractTask toAbstractTask(CRTask crTask) {
        if(crTask == null)
            throw new ConfigConvertionException("task cannot be null");

        if(crTask instanceof CRPluggableTask)
            return toPluggableTask((CRPluggableTask)crTask);
        else if(crTask instanceof CRBuildTask) {
            return toBuildTask((CRBuildTask)crTask);
        }
        else if(crTask instanceof CRExecTask)
        {
            return toExecTask((CRExecTask)crTask);
        }
        else if(crTask instanceof CRFetchArtifactTask)
        {
            return toFetchTask((CRFetchArtifactTask)crTask);
        }
        else
            throw new RuntimeException(
                    String.format("unknown type of task '%s'",crTask));
    }

    public FetchTask toFetchTask(CRFetchArtifactTask crTask) {
        FetchTask fetchTask = new FetchTask(
                new CaseInsensitiveString(crTask.getPipelineName()),
                new CaseInsensitiveString(crTask.getStage()),
                new CaseInsensitiveString(crTask.getJob()),
                crTask.getSource(),
                crTask.getDestination());

        if(crTask.sourceIsDirectory()) {
            fetchTask.setSrcdir(crTask.getSource());
            fetchTask.setSrcfile(null);
        }
        setCommonTaskMembers(fetchTask,crTask);
        return fetchTask;
    }

    public ExecTask toExecTask(CRExecTask crTask) {
        ExecTask execTask = new ExecTask(crTask.getCommand(), toArgList(crTask.getArgs()), crTask.getWorkingDirectory());
        execTask.setTimeout(crTask.getTimeout());
        setCommonTaskMembers(execTask, crTask);
        return execTask;
    }

    private Arguments toArgList(List<String> args) {
        Arguments arguments = new Arguments();
        for(String arg : args)
        {
            arguments.add(new Argument(arg));
        }
        return arguments;
    }

    public BuildTask toBuildTask(CRBuildTask crBuildTask) {
        BuildTask buildTask;
        switch (crBuildTask.getType())
        {
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
                        String.format("unknown type of build task '%s'",crBuildTask.getType()));
        }
        setCommonBuildTaskMembers(buildTask,crBuildTask);
        setCommonTaskMembers(buildTask,crBuildTask);
        return buildTask;
    }

    private void setCommonBuildTaskMembers(BuildTask buildTask, CRBuildTask crBuildTask) {
        buildTask.buildFile = crBuildTask.getBuildFile();
        buildTask.target = crBuildTask.getTarget();
        buildTask.workingDirectory = crBuildTask.getWorkingDirectory();
    }


    private Configuration toConfiguration(Collection<CRConfigurationProperty> properties) {
        Configuration configuration = new Configuration();
        for(CRConfigurationProperty p : properties)
        {
            if(p.getValue() != null)
                configuration.addNewConfigurationWithValue(p.getKey(),p.getValue(),false);
            else
                configuration.addNewConfigurationWithValue(p.getKey(),p.getEncryptedValue(),true);
        }
        return configuration;
    }

    public PluginConfiguration toPluginConfiguration(CRPluginConfiguration pluginConfiguration) {
        return new PluginConfiguration(pluginConfiguration.getId(),pluginConfiguration.getVersion());
    }

    public DependencyMaterialConfig toDependencyMaterialConfig(CRDependencyMaterial crDependencyMaterial) {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(
                new CaseInsensitiveString(crDependencyMaterial.getPipelineName()),
                new CaseInsensitiveString(crDependencyMaterial.getStageName()));
        setCommonMaterialMembers(dependencyMaterialConfig,crDependencyMaterial);
        return dependencyMaterialConfig;
    }

    private void setCommonMaterialMembers(AbstractMaterialConfig materialConfig, CRMaterial crMaterial) {
        materialConfig.setName(new CaseInsensitiveString(crMaterial.getName()));
    }

    public MaterialConfig toMaterialConfig(CRMaterial crMaterial) {
        if(crMaterial == null)
            throw new ConfigConvertionException("material cannot be null");

        if(crMaterial instanceof CRDependencyMaterial)
            return toDependencyMaterialConfig((CRDependencyMaterial)crMaterial);
        else if(crMaterial instanceof CRScmMaterial)
        {
            CRScmMaterial crScmMaterial = (CRScmMaterial)crMaterial;
            return toScmMaterialConfig(crScmMaterial);
        }
        else if(crMaterial instanceof CRPluggableScmMaterial)
        {
            CRPluggableScmMaterial crPluggableScmMaterial = (CRPluggableScmMaterial)crMaterial;
            return toPluggableScmMaterialConfig(crPluggableScmMaterial);
        }
        else if(crMaterial instanceof CRPackageMaterial)
        {
            CRPackageMaterial crPackageMaterial = (CRPackageMaterial)crMaterial;
            return toPackageMaterial(crPackageMaterial);
        }
        else
            throw new ConfigConvertionException(
                    String.format("unknown material type '%s'",crMaterial));
    }

    public PackageMaterialConfig toPackageMaterial(CRPackageMaterial crPackageMaterial) {
        PackageDefinition packageDefinition = getPackageDefinition(crPackageMaterial.getPackageId());
        return new PackageMaterialConfig(new CaseInsensitiveString(crPackageMaterial.getName()),crPackageMaterial.getPackageId(),packageDefinition);
    }

    private PackageDefinition getPackageDefinition(String packageId) {
        PackageRepository packageRepositoryHaving = this.cachedFileGoConfig.currentConfig().getPackageRepositories().findPackageRepositoryHaving(packageId);
        if(packageRepositoryHaving == null)
            throw new ConfigConvertionException(
                    String.format("Failed to find package repository with package id '%s'",packageId));
        return packageRepositoryHaving.findPackage(packageId);
    }

    private PluggableSCMMaterialConfig toPluggableScmMaterialConfig(CRPluggableScmMaterial crPluggableScmMaterial) {
        SCMs scms = getSCMs();
        String id = crPluggableScmMaterial.getScmId();
        SCM scmConfig = scms.find(id);
        if(scmConfig == null)
            throw new ConfigConvertionException(
                    String.format("Failed to find referenced scm '%s'",id));

        return new PluggableSCMMaterialConfig(new CaseInsensitiveString(crPluggableScmMaterial.getName()),
                scmConfig,crPluggableScmMaterial.getDirectory(),
                toFilter(crPluggableScmMaterial.getFilter()));
    }

    private SCMs getSCMs() {
        return this.cachedFileGoConfig.currentConfig().getSCMs();
    }

    private ScmMaterialConfig toScmMaterialConfig(CRScmMaterial crScmMaterial) {
        if(crScmMaterial instanceof CRGitMaterial)
        {
            CRGitMaterial git = (CRGitMaterial)crScmMaterial;
            Filter filter = toFilter(crScmMaterial);
            return new GitMaterialConfig(new UrlArgument(git.getUrl()),git.getBranch(),
                    null,git.isAutoUpdate(), filter,crScmMaterial.getFolder(),
                    new CaseInsensitiveString(crScmMaterial.getName()));
        }
        else if(crScmMaterial instanceof CRHgMaterial)
        {
            CRHgMaterial hg = (CRHgMaterial)crScmMaterial;
            return new HgMaterialConfig(new HgUrlArgument(hg.getUrl()),
            hg.isAutoUpdate(), toFilter(crScmMaterial), hg.getFolder(),
                    new CaseInsensitiveString(crScmMaterial.getName()));
        }
        else if(crScmMaterial instanceof CRP4Material)
        {
            CRP4Material crp4Material = (CRP4Material)crScmMaterial;
            P4MaterialConfig p4MaterialConfig = new P4MaterialConfig(crp4Material.getServerAndPort(), crp4Material.getView(), cipher);
            if(crp4Material.getEncryptedPassword() != null)
            {
                p4MaterialConfig.setEncryptedPassword(crp4Material.getEncryptedPassword());
            }
            else
            {
                p4MaterialConfig.setPassword(crp4Material.getPassword());
            }
            p4MaterialConfig.setUserName(crp4Material.getUserName());
            p4MaterialConfig.setUseTickets(crp4Material.getUseTickets());
            setCommonMaterialMembers(p4MaterialConfig, crScmMaterial);
            setCommonScmMaterialMembers(p4MaterialConfig,crp4Material);
            return p4MaterialConfig;
        }
        else if(crScmMaterial instanceof CRSvnMaterial)
        {
            CRSvnMaterial crSvnMaterial = (CRSvnMaterial)crScmMaterial;
            SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig(
                    crSvnMaterial.getUrl(), crSvnMaterial.getUsername(), crSvnMaterial.isCheckExternals(), cipher);
            if(crSvnMaterial.getEncryptedPassword() != null)
            {
                svnMaterialConfig.setEncryptedPassword(crSvnMaterial.getEncryptedPassword());
            }
            else
            {
                svnMaterialConfig.setPassword(crSvnMaterial.getPassword());
            }
            setCommonMaterialMembers(svnMaterialConfig, crScmMaterial);
            setCommonScmMaterialMembers(svnMaterialConfig,crSvnMaterial);
            return svnMaterialConfig;
        }
        else if(crScmMaterial instanceof CRTfsMaterial)
        {
            CRTfsMaterial crTfsMaterial = (CRTfsMaterial)crScmMaterial;
            TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(cipher,
                    new UrlArgument(crTfsMaterial.getUrl()),
                    crTfsMaterial.getUserName(),
                    crTfsMaterial.getDomain(),
                    crTfsMaterial.getProjectPath());
            if(crTfsMaterial.getEncryptedPassword() != null)
            {
                tfsMaterialConfig.setEncryptedPassword(crTfsMaterial.getEncryptedPassword());
            }
            else
            {
                tfsMaterialConfig.setPassword(crTfsMaterial.getPassword());
            }
            setCommonMaterialMembers(tfsMaterialConfig, crTfsMaterial);
            setCommonScmMaterialMembers(tfsMaterialConfig,crTfsMaterial);
            return tfsMaterialConfig;
        }
        else
            throw new ConfigConvertionException(
                    String.format("unknown scm material type '%s'",crScmMaterial));
    }

    private void setCommonScmMaterialMembers(ScmMaterialConfig scmMaterialConfig, CRScmMaterial crScmMaterial) {
        scmMaterialConfig.setFolder(crScmMaterial.getFolder());
        scmMaterialConfig.setAutoUpdate(crScmMaterial.isAutoUpdate());
        scmMaterialConfig.setFilter(toFilter(crScmMaterial));
    }

    private Filter toFilter(CRScmMaterial crScmMaterial) {
        List<String> filterList = crScmMaterial.getFilter();
        return toFilter(filterList);
    }

    private Filter toFilter(List<String> filterList) {
        Filter filter = new Filter();
        for(String pattern : filterList)
        {
            filter.add(new IgnoredFiles(pattern));
        }
        return filter;
    }

    public JobConfig toJobConfig(CRJob crJob) {
        JobConfig jobConfig = new JobConfig(crJob.getName());
        for(CREnvironmentVariable crEnvironmentVariable : crJob.getEnvironmentVariables())
        {
            jobConfig.getVariables().add(toEnvironmentVariableConfig(crEnvironmentVariable));
        }

        List<CRTask> crTasks = crJob.getTasks();
        Tasks tasks = jobConfig.getTasks();
        for(CRTask crTask : crTasks)
        {
            tasks.add(toAbstractTask(crTask));
        }

        Tabs tabs = jobConfig.getTabs();
        for(CRTab crTab : crJob.getTabs())
        {
            tabs.add(toTab(crTab));
        }

        Resources resources = jobConfig.resources();
        for(String crResource : crJob.getResources())
        {
            resources.add(new Resource(crResource));
        }

        ArtifactPlans artifactPlans = jobConfig.artifactPlans();
        for(CRArtifact crArtifact : crJob.getArtifacts())
        {
            artifactPlans.add(toArtifactPlan(crArtifact));
        }

        ArtifactPropertiesGenerators artifactPropertiesGenerators = jobConfig.getProperties();
        for(CRPropertyGenerator crPropertyGenerator : crJob.getArtifactPropertiesGenerators())
        {
            artifactPropertiesGenerators.add(new ArtifactPropertiesGenerator(
                    crPropertyGenerator.getName(),crPropertyGenerator.getSrc(),crPropertyGenerator.getXpath()));
        }

        jobConfig.setRunOnAllAgents(crJob.isRunOnAllAgents());
        jobConfig.setRunInstanceCount(crJob.getRunInstanceCount());
        jobConfig.setTimeout(Integer.toString(crJob.getTimeout()));

        return jobConfig;
    }

    private ArtifactPlan toArtifactPlan(CRArtifact crArtifact) {
        // artifact type is not set here. is this OK?
        return new ArtifactPlan(crArtifact.getSrc(),crArtifact.getDest());
    }

    private Tab toTab(CRTab crTab) {
        return new Tab(crTab.getName(),crTab.getPath());
    }

    public StageConfig toStage(CRStage crStage) {
        Approval approval = toApproval(crStage.getApproval());
        StageConfig stageConfig = new StageConfig(new CaseInsensitiveString(crStage.getName()), crStage.isFetchMaterials(),
                crStage.isCleanWorkingDir(), approval, crStage.isArtifactCleanupProhibited(), toJobConfigs(crStage.getJobs()));
        EnvironmentVariablesConfig environmentVariableConfigs = stageConfig.getVariables();
        for(CREnvironmentVariable crEnvironmentVariable : crStage.getEnvironmentVariables())
        {
            environmentVariableConfigs.add(toEnvironmentVariableConfig(crEnvironmentVariable));
        }
        return stageConfig;
    }

    public Approval toApproval(CRApproval crApproval) {
        Approval approval;
        if(crApproval.getType() == CRApprovalCondition.manual)
            approval = Approval.manualApproval();
        else
            approval = Approval.automaticApproval();

        AuthConfig authConfig = approval.getAuthConfig();
        for(String user : crApproval.getAuthorizedUsers())
        {
            authConfig.add(new AdminUser(new CaseInsensitiveString(user)));
        }
        for(String user : crApproval.getAuthorizedRoles())
        {
            authConfig.add(new AdminRole(new CaseInsensitiveString(user)));
        }

        return approval;
    }

    private JobConfigs toJobConfigs(Collection<CRJob> jobs) {
        JobConfigs jobConfigs = new JobConfigs();
        for(CRJob crJob : jobs)
        {
            jobConfigs.add(toJobConfig(crJob));
        }
        return jobConfigs;
    }

    public PipelineConfig toPipelineConfig(CRPipeline crPipeline) {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        for(CRMaterial crMaterial : crPipeline.getMaterials())
        {
            materialConfigs.add(toMaterialConfig(crMaterial));
        }

        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(crPipeline.getName()),materialConfigs);

        for(CRStage crStage : crPipeline.getStages())
        {
            pipelineConfig.add(toStage(crStage));
        }

        if(crPipeline.getLabelTemplate() != null)
            pipelineConfig.setLabelTemplate(crPipeline.getLabelTemplate());

        CRTrackingTool crTrackingTool = crPipeline.getTrackingTool();
        if(crTrackingTool != null)
        {
            pipelineConfig.setTrackingTool(toTrackingTool(crTrackingTool));
        }
        CRMingle crMingle = crPipeline.getMingle();
        if(crMingle != null)
        {
            pipelineConfig.setMingleConfig(toMingleConfig(crMingle));
        }

        CRTimer crTimer = crPipeline.getTimer();
        if(crTimer != null) {
            pipelineConfig.setTimer(new TimerConfig(crTimer.getTimerSpec(), crTimer.isOnlyOnChanges()));
        }

        EnvironmentVariablesConfig variables = pipelineConfig.getVariables();
        for(CREnvironmentVariable crEnvironmentVariable : crPipeline.getEnvironmentVariables())
        {
            variables.add(toEnvironmentVariableConfig(crEnvironmentVariable));
        }

        pipelineConfig.setLock(crPipeline.isLocked());

        return pipelineConfig;
    }

    private MingleConfig toMingleConfig(CRMingle crMingle) {
        return new MingleConfig(crMingle.getBaseUrl(),crMingle.getProjectId(),crMingle.getMql());
    }

    private TrackingTool toTrackingTool(CRTrackingTool crTrackingTool) {
        return new TrackingTool(crTrackingTool.getLink(), crTrackingTool.getRegex());
    }


    //TODO #1133 convert each config element
}
