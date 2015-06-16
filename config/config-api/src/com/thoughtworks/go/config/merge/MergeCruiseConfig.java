package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.feature.EnterpriseFeature;
import com.thoughtworks.go.licensing.Edition;
import com.thoughtworks.go.licensing.LicenseValidity;
import com.thoughtworks.go.util.DFSCycleDetector;
import com.thoughtworks.go.util.Node;
import com.thoughtworks.go.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

/**
 * Composite of main CruiseConfig and parts.
 */
public class MergeCruiseConfig implements CruiseConfig {

    private BasicCruiseConfig main;
    private List<PartialConfig> parts = new ArrayList<PartialConfig>();

    private PipelineGroups groups = new PipelineGroups();
    private EnvironmentsConfig environments = new EnvironmentsConfig();

    private ConcurrentHashMap<CaseInsensitiveString, PipelineConfig> pipelineNameToConfigMap = new ConcurrentHashMap<CaseInsensitiveString, PipelineConfig>();
    private List<PipelineConfig> allPipelineConfigs;

    private ConfigErrors errors = new ConfigErrors();

    public  MergeCruiseConfig(BasicCruiseConfig main, PartialConfig... parts){
        this.main = main;
        for (PartialConfig part : parts) {
            this.parts.add(part);
        }

        groups = mergePipelineConfigs();
        environments = mergeEnvironmentConfigs();
    }

    private EnvironmentsConfig mergeEnvironmentConfigs() {
        EnvironmentsConfig environments = new EnvironmentsConfig();

        //first add environment configs from main
        List<EnvironmentConfig> allEnvConfigs = new ArrayList<EnvironmentConfig>();
        for(EnvironmentConfig envConfig : this.main.getEnvironments())
        {
            allEnvConfigs.add(envConfig);
        }
        // then add from each part
        for (PartialConfig part : this.parts) {
            for(EnvironmentConfig partPipesConf : part.getEnvironments())
            {
                allEnvConfigs.add(partPipesConf);
            }
        }

        // lets group them by environment name
        Map<CaseInsensitiveString, List<EnvironmentConfig>> map = new HashMap<CaseInsensitiveString, List<EnvironmentConfig>>();
        for(EnvironmentConfig env : allEnvConfigs)
        {
            CaseInsensitiveString key = env.name();
            if (map.get(key) == null) {
                map.put(key, new ArrayList<EnvironmentConfig>());
            }
            map.get(key).add(env);
        }
        for(List<EnvironmentConfig> oneEnv : map.values())
        {
            if(oneEnv.size() == 1)
                environments.add(oneEnv.get(0));
            else
                environments.add(new MergeEnvironmentConfig(oneEnv));
        }

        return environments;
    }

    private PipelineGroups mergePipelineConfigs() {
        PipelineGroups groups = new PipelineGroups();

        // first add pipeline configs from main part
        List<PipelineConfigs> allPipelineConfigs = new ArrayList<PipelineConfigs>();
        for(PipelineConfigs partPipesConf : this.main.getGroups())
        {
            allPipelineConfigs.add(partPipesConf);
        }
        // then add from each part
        for (PartialConfig part : this.parts) {
            for(PipelineConfigs partPipesConf : part.getGroups())
            {
                allPipelineConfigs.add(partPipesConf);
            }
        }
        //there may be duplicated names and conflicts in general in the PipelineConfigs

        // lets group them by 'pipeline group' name
        Map<String, List<PipelineConfigs>> map = new HashMap<String, List<PipelineConfigs>>();
        for (PipelineConfigs pipes : allPipelineConfigs) {
            String key = pipes.getGroup();
            if (map.get(key) == null) {
                map.put(key, new ArrayList<PipelineConfigs>());
            }
            map.get(key).add(pipes);
        }
        for(List<PipelineConfigs> oneGroup : map.values())
        {
            if(oneGroup.size() == 1)
                groups.add(oneGroup.get(0));
            else
                groups.add(new MergePipelineConfigs(oneGroup));
        }

        return groups;
    }

    public boolean isPipelineDefinedInMain(CaseInsensitiveString pipelineName)
    {
        return this.main.hasPipelineNamed(pipelineName);
    }

    //TODO validations of illegal references

    //TODO confusing methods, how should these behave?
    @Override
    public void setEnvironments(EnvironmentsConfig environments) {

    }
    @Override
    public void setGroup(PipelineGroups pipelineGroups) {

    }

    @Override
    public void makePipelineUseTemplate(CaseInsensitiveString pipelineName, CaseInsensitiveString templateName) {

    }

    @Override
    public void copyErrorsTo(CruiseConfig to) {

    }

    @Override
    public void updateGroup(PipelineConfigs pipelineConfigs, String groupName) {
    }

    @Override
    public ConfigOrigin getOrigin() {
        return null;
    }

    //TODO could this be removed?
    @Override
    public void initializeServer() {
        this.main.initializeServer();
    }

    @Override
    public String getMd5() {
        return this.main.getMd5();
    }

    @Override
    public int schemaVersion() {
        return this.main.schemaVersion();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        areThereCyclicDependencies();
    }

    @Override
    public Hashtable<CaseInsensitiveString, Node> getDependencyTable() {
        final Hashtable<CaseInsensitiveString, Node> hashtable = new Hashtable<CaseInsensitiveString, Node>();
        this.accept(new PiplineConfigVisitor() {
            public void visit(PipelineConfig pipelineConfig) {
                hashtable.put(pipelineConfig.name(), pipelineConfig.getDependenciesAsNode());
            }
        });
        return hashtable;
    }

    private void areThereCyclicDependencies() {
        final DFSCycleDetector dfsCycleDetector = new DFSCycleDetector();
        final Hashtable<CaseInsensitiveString, Node> dependencyTable = getDependencyTable();
        List<PipelineConfig> pipelineConfigs = this.getAllPipelineConfigs();
        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            try {
                dfsCycleDetector.topoSort(pipelineConfig.name(), dependencyTable);
            } catch (Exception e) {
                addToErrorsBaseOnMaterialsIfDoesNotExist(e.getMessage(), pipelineConfig.materialConfigs(), pipelineConfigs);
            }
        }
    }

    private void addToErrorsBaseOnMaterialsIfDoesNotExist(String errorMessage, MaterialConfigs materialConfigs, List<PipelineConfig> pipelineConfigs) {
        for (PipelineConfig config : pipelineConfigs) {
            if (config.materialConfigs().errors().getAll().contains(errorMessage)) {
                return;
            }
        }
        materialConfigs.addError("base", errorMessage);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public EnvironmentVariablesConfig variablesFor(String pipelineName) {
        EnvironmentVariablesConfig pipelineVariables = pipelineConfigByName(new CaseInsensitiveString(pipelineName)).getVariables();
        EnvironmentConfig environment = this.environments.findEnvironmentForPipeline(new CaseInsensitiveString(pipelineName));
        return environment != null ? environment.getVariables().overrideWith(pipelineVariables) : pipelineVariables;
    }

    @Override
    public EnvironmentConfig addEnvironment(String environmentName) {
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        this.addEnvironment(environmentConfig);
        return environmentConfig;
    }

    @Override
    public void addEnvironment(BasicEnvironmentConfig config) {
        //validate at global scope
        this.environments.validateNotADuplicate(config);
        // but append to main config
        this.main.addEnvironment(config);
        //TODO add rather than reconstruct
        this.environments = mergeEnvironmentConfigs();
    }


    @Override
    public StageConfig stageConfigByName(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName) {
        StageConfig stageConfig = pipelineConfigByName(pipelineName).findBy(stageName);
        StageNotFoundException.bombIfNull(stageConfig, pipelineName, stageName);
        return stageConfig;
    }

    @Override
    public JobConfig findJob(String pipelineName, String stageName, String jobName) {
        return pipelineConfigByName(new CaseInsensitiveString(pipelineName))
                .findBy(new CaseInsensitiveString(stageName))
                .jobConfigByConfigName(new CaseInsensitiveString(jobName));
    }

    @Override
    public PipelineConfig pipelineConfigByName(final CaseInsensitiveString name) {
        if (pipelineNameToConfigMap.containsKey(name)) {
            return pipelineNameToConfigMap.get(name);
        }
        PipelineConfig pipelineConfig = getPipelineConfigByName(name);
        if (pipelineConfig == null) {
            throw new PipelineNotFoundException("Pipeline '" + name + "' not found.");
        }
        pipelineNameToConfigMap.putIfAbsent(pipelineConfig.name(), pipelineConfig);

        return pipelineConfig;
    }

    @Override
    public boolean hasStageConfigNamed(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, boolean ignoreCase) {
        PipelineConfig pipelineConfig = getPipelineConfigByName(pipelineName);
        if (pipelineConfig == null) {
            return false;
        }
        return pipelineConfig.findBy(stageName) != null;
    }

    @Override
    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        for(PipelineConfigs pipes : this.groups)
        {
            for(PipelineConfig conf : pipes)
            {
                if(conf.name().equals(pipelineName))
                    return conf;
            }
        }
        return null;
    }

    @Override
    public boolean hasPipelineNamed(CaseInsensitiveString pipelineName) {
        PipelineConfig pipelineConfig = getPipelineConfigByName(pipelineName);
        return pipelineConfig != null;
    }

    @Override
    public boolean hasNextStage(final CaseInsensitiveString pipelineName, final CaseInsensitiveString lastStageName) {
        PipelineConfig pipelineConfig = getPipelineConfigByName(pipelineName);
        if (pipelineConfig == null) {
            return false;
        }
        return pipelineConfig.nextStage(lastStageName) != null;
    }

    @Override
    public boolean hasPreviousStage(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        PipelineConfig pipelineConfig = getPipelineConfigByName(pipelineName);
        if (pipelineConfig == null) {
            return false;
        }
        return pipelineConfig.previousStage(stageName) != null;
    }

    @Override
    public StageConfig nextStage(final CaseInsensitiveString pipelineName, final CaseInsensitiveString lastStageName) {
        StageConfig stageConfig = pipelineConfigByName(pipelineName).nextStage(lastStageName);
        bombIfNull(stageConfig, "Build stage after '" + lastStageName + "' not found.");
        return stageConfig;
    }

    @Override
    public StageConfig previousStage(final CaseInsensitiveString pipelineName, final CaseInsensitiveString lastStageName) {
        StageConfig stageConfig = pipelineConfigByName(pipelineName).previousStage(lastStageName);
        bombIfNull(stageConfig, "Build stage after '" + lastStageName + "' not found.");
        return stageConfig;
    }

    @Override
    public JobConfig jobConfigByName(String pipelineName, String stageName, String jobInstanceName, boolean ignoreCase) {
        JobConfig jobConfig = stageConfigByName(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName)).jobConfigByInstanceName(jobInstanceName,
                ignoreCase);
        bombIfNull(jobConfig,
                String.format("Job [%s] is not found in pipeline [%s] stage [%s].", jobInstanceName,
                        pipelineName, stageName));
        return jobConfig;
    }

    @Override
    public EnvironmentsConfig getEnvironments() {
        return environments;
    }

    @Override
    public Map<String, List<Authorization.PrivilegeType>> groupsAffectedByDeletionOfRole(String roleName) {
        Map<String, List<Authorization.PrivilegeType>> result = new HashMap<String, List<Authorization.PrivilegeType>>();
        for (PipelineConfigs group : groups) {
            final List<Authorization.PrivilegeType> privileges = group.getAuthorization().privilagesOfRole(new CaseInsensitiveString(roleName));
            if (privileges.size() > 0) {
                result.put(group.getGroup(), privileges);
            }
        }
        return result;
    }

    @Override
    public Set<Pair<PipelineConfig, StageConfig>> stagesWithPermissionForRole(String roleName) {
        Set<Pair<PipelineConfig, StageConfig>> result = new HashSet<Pair<PipelineConfig, StageConfig>>();
        for (PipelineConfig pipelineConfig : allPipelines()) {
            result.addAll(pipelineConfig.stagesWithPermissionForRole(new CaseInsensitiveString(roleName)));
        }
        return result;
    }


    @Override
    public List<PipelineConfig> allPipelines() {
        return this.getAllPipelineConfigs();
    }

    @Override
    public PipelineConfigs pipelines(String groupName) {
        PipelineGroups pipelineGroups = this.getGroups();
        for (PipelineConfigs pipelineGroup : pipelineGroups) {
            if (pipelineGroup.isNamed(groupName)) {
                return pipelineGroup;
            }
        }
        throw new RuntimeException("");
    }

    @Override
    public boolean hasBuildPlan(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName, String buildName, boolean ignoreCase) {
        if (!hasStageConfigNamed(pipelineName, stageName, ignoreCase)) {
            return false;
        }
        StageConfig stageConfig = stageConfigByName(pipelineName, stageName);
        return stageConfig != null && stageConfig.jobConfigByInstanceName(buildName, ignoreCase) != null;
    }

    @Override
    public boolean requiresApproval(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        PipelineConfig pipelineConfig = getPipelineConfigByName(pipelineName);
        if (pipelineConfig == null) {
            return false;
        }
        final StageConfig stageConfig = pipelineConfig.findBy(stageName);
        return stageConfig != null && stageConfig.requiresApproval();
    }

    @Override
    public void accept(JobConfigVisitor visitor) {
        for (PipelineConfig pipelineConfig : this.getAllPipelineConfigs()) {
            for (StageConfig stageConfig : pipelineConfig) {
                for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
                    visitor.visit(pipelineConfig, stageConfig, jobConfig);
                }
            }
        }
    }

    @Override
    public void accept(TaskConfigVisitor visitor) {
        for (PipelineConfig pipelineConfig : this.getAllPipelineConfigs()) {
            for (StageConfig stageConfig : pipelineConfig) {
                for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
                    for (Task task : jobConfig.tasks()) {
                        if (!(task instanceof NullTask)) {
                            visitor.visit(pipelineConfig, stageConfig, jobConfig, task);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void accept(final PiplineConfigVisitor visitor) {
        accept(new PipelineGroupVisitor() {
            public void visit(PipelineConfigs group) {
                group.accept(visitor);
            }
        });
    }

    @Override
    public PipelineGroups getGroups() {
        return groups;
    }

    @Override
    public void addPipeline(String groupName, PipelineConfig pipelineConfig) {
        groups.addPipeline(groupName, pipelineConfig);
    }

    @Override
    public void addPipelineWithoutValidation(String groupName, PipelineConfig pipelineConfig) {
        groups.addPipelineWithoutValidation(sanitizedGroupName(groupName), pipelineConfig);

    }

    @Override
    public void update(String groupName, String pipelineName, PipelineConfig pipeline) {
        if (groups.isEmpty()) {
            PipelineConfigs configs = new BasicPipelineConfigs();
            configs.add(pipeline);
            groups.add(configs);
        }
        groups.update(groupName, pipelineName, pipeline);
    }

    @Override
    public boolean exist(int pipelineIndex) {
        return pipelineIndex < this.getAllPipelineConfigs().size();
    }

    @Override
    public boolean hasPipeline() {
        return !this.getAllPipelineConfigs().isEmpty();
    }

    @Override
    public PipelineConfig find(String groupName, int pipelineIndex) {
        return groups.findPipeline(groupName, pipelineIndex);
    }

    @Override
    public int numberOfPipelines() {
        return this.getAllPipelineConfigs().size();
    }

    @Override
    public int numbersOfPipeline(String groupName) {
        return pipelines(groupName).size();
    }

    @Override
    public void groups(List<String> allGroup) {
        for (PipelineConfigs group : groups) {
            group.add(allGroup);
        }
    }

    @Override
    public boolean exist(String groupName, String pipelineName) {
        PipelineConfigs configs = groups.findGroup(groupName);
        PipelineConfig pipelineConfig = configs.findBy(new CaseInsensitiveString(pipelineName));
        return pipelineConfig != null;
    }

    @Override
    public List<Task> tasksForJob(String pipelineName, String stageName, String jobName) {
        return jobConfigByName(pipelineName, stageName, jobName, true).tasks();
    }

    @Override
    public boolean isInFirstGroup(CaseInsensitiveString pipelineName) {
        if (groups.isEmpty()) {
            throw new IllegalStateException("No pipeline group defined yet!");
        }
        return groups.first().hasPipeline(pipelineName);
    }

    @Override
    public boolean hasMultiplePipelineGroups() {
        return groups.size() > 1;
    }

    @Override
    public void accept(PipelineGroupVisitor visitor) {
        groups.accept(visitor);
    }

    @Override
    public boolean hasPipelineGroup(String groupName) {
        return groups.hasGroup(groupName);
    }

    @Override
    public PipelineConfigs findGroup(String groupName) {
        return groups.findGroup(groupName);
    }


    @Override
    public List<PipelineConfig> getAllPipelineConfigs() {
        if (allPipelineConfigs == null) {
            List<PipelineConfig> configs = new ArrayList<PipelineConfig>();
            PipelineGroups groups = getGroups();
            for (PipelineConfigs group : groups) {
                configs.addAll(group);
            }
            allPipelineConfigs = configs;
        }
        return allPipelineConfigs;
    }

    @Override
    public List<CaseInsensitiveString> getAllPipelineNames() {
        List<CaseInsensitiveString> names = new ArrayList<CaseInsensitiveString>();
        for (PipelineConfig config : getAllPipelineConfigs()) {
            names.add(config.name());
        }
        return names;
    }


    @Override
    public Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelines() {
        return getUniqueMaterials(true);
    }

    @Override
    public Set<MaterialConfig> getAllUniqueMaterials() {
        return getUniqueMaterials(false);
    }

    private Set<MaterialConfig> getUniqueMaterials(boolean ignoreManualPipelines) {
        Set<MaterialConfig> materialConfigs = new HashSet<MaterialConfig>();
        Set<Map> uniqueMaterials = new HashSet<Map>();
        for (PipelineConfig pipelineConfig : this.getAllPipelineConfigs()) {
            for (MaterialConfig materialConfig : pipelineConfig.materialConfigs()) {
                if (!uniqueMaterials.contains(materialConfig.getSqlCriteria())) {
                    boolean shouldSkipPolling = !materialConfig.isAutoUpdate();
                    boolean scmOrPackageMaterial = !(materialConfig instanceof DependencyMaterialConfig);
                    if (ignoreManualPipelines && scmOrPackageMaterial && shouldSkipPolling) {
                        continue;
                    }
                    materialConfigs.add(materialConfig);
                    uniqueMaterials.add(materialConfig.getSqlCriteria());
                }
            }
        }
        return materialConfigs;
    }

    @Override
    public Set<StageConfig> getStagesUsedAsMaterials(PipelineConfig pipelineConfig) {
        Set<String> stagesUsedAsMaterials = new HashSet<String>();
        for (MaterialConfig materialConfig : getAllUniqueMaterials()) {
            if (materialConfig instanceof DependencyMaterialConfig) {
                DependencyMaterialConfig dep = (DependencyMaterialConfig) materialConfig;
                stagesUsedAsMaterials.add(dep.getPipelineName() + "|" + dep.getStageName());
            }
        }
        Set<StageConfig> stages = new HashSet<StageConfig>();
        for (StageConfig stage : pipelineConfig) {
            if (stagesUsedAsMaterials.contains(pipelineConfig.name() + "|" + stage.name())) {
                stages.add(stage);
            }
        }
        return stages;
    }


    @Override
    public Boolean isPipelineLocked(String pipelineName) {
        PipelineConfig pipelineConfig = pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        if (pipelineConfig.hasExplicitLock()) {
            return pipelineConfig.explicitLock();
        }
        return false;
    }

    @Override
    public Set<Resource> getAllResources() {
        final HashSet<Resource> resources = new HashSet<Resource>();
        accept(new JobConfigVisitor() {
            public void visit(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig) {
                resources.addAll(jobConfig.resources());
            }
        });
        for (AgentConfig agent : this.agents()) {
            resources.addAll(agent.getResources());
        }
        return resources;
    }


    @Override
    public Iterable<PipelineConfig> getDownstreamPipelines(String pipelineName) {
        ArrayList<PipelineConfig> configs = new ArrayList<PipelineConfig>();
        for (PipelineConfig pipelineConfig : this.getAllPipelineConfigs()) {
            if (pipelineConfig.dependsOn(new CaseInsensitiveString(pipelineName))) {
                configs.add(pipelineConfig);
            }
        }
        return configs;
    }

    @Override
    public boolean hasVariableInScope(String pipelineName, String variableName) {
        EnvironmentConfig environmentConfig = environments.findEnvironmentForPipeline(new CaseInsensitiveString(pipelineName));
        if (environmentConfig != null) {
            if (environmentConfig.hasVariable(variableName)) {
                return true;
            }
        }
        return pipelineConfigByName(new CaseInsensitiveString(pipelineName)).hasVariableInScope(variableName);
    }

    @Override
    public boolean isGroupAdministrator(final CaseInsensitiveString userName) {
        final List<Role> roles = server().security().memberRoleFor(userName);
        FindPipelineGroupAdminstrator finder = new FindPipelineGroupAdminstrator(userName, roles);
        groups.accept(finder);
        return finder.isGroupAdmin;
    }

    @Override
    public List<ConfigErrors> getAllErrors() {
        return getAllErrors(this);
    }

    private List<ConfigErrors> getAllErrors(Validatable v) {
        final List<ConfigErrors> allErrors = new ArrayList<ConfigErrors>();
        new GoConfigGraphWalker(v).walk(new ErrorCollectingHandler(allErrors) {
            @Override
            public void handleValidation(Validatable validatable, ValidationContext context) {
                // do nothing here
            }
        });
        return allErrors;
    }

    @Override
    public List<ConfigErrors> getAllErrorsExceptFor(Validatable skipValidatable) {
        List<ConfigErrors> all = getAllErrors();
        if (skipValidatable != null) {
            all.removeAll(getAllErrors(skipValidatable));
        }
        return all;
    }

    @Override
    public List<ConfigErrors> validateAfterPreprocess() {
        final List<ConfigErrors> allErrors = new ArrayList<ConfigErrors>();
        new GoConfigGraphWalker(this).walk(new ErrorCollectingHandler(allErrors) {
            @Override
            public void handleValidation(Validatable validatable, ValidationContext context) {
                validatable.validate(context);
            }
        });
        return allErrors;
    }


    @Override
    public PipelineConfigs findGroupOfPipeline(PipelineConfig pipelineConfig) {
        String groupName = getGroups().findGroupNameByPipeline(pipelineConfig.name());
        return findGroup(groupName);
    }

    @Override
    public PipelineConfig findPipelineUsingThisPipelineAsADependency(String pipelineName) {
        List<PipelineConfig> configs = getAllPipelineConfigs();
        for (PipelineConfig config : configs) {
            DependencyMaterialConfig materialConfig = config.materialConfigs().findDependencyMaterial(new CaseInsensitiveString(pipelineName));
            if (materialConfig != null) {
                return config;
            }
        }
        return null;
    }

    @Override
    public Map<String, List<PipelineConfig>> generatePipelineVsDownstreamMap() {
        List<PipelineConfig> pipelineConfigs = getAllPipelineConfigs();
        Map<String, List<PipelineConfig>> result = new HashMap<String, List<PipelineConfig>>();

        for (PipelineConfig currentPipeline : pipelineConfigs) {
            String currentPipelineName = currentPipeline.name().toString();
            if (!result.containsKey(currentPipelineName)) {
                result.put(currentPipelineName, new ArrayList<PipelineConfig>());
            }

            for (MaterialConfig materialConfig : currentPipeline.materialConfigs()) {
                if (materialConfig instanceof DependencyMaterialConfig) {
                    String pipelineWhichTriggersMe = ((DependencyMaterialConfig) materialConfig).getPipelineName().toString();
                    if (!result.containsKey(pipelineWhichTriggersMe)) {
                        result.put(pipelineWhichTriggersMe, new ArrayList<PipelineConfig>());
                    }
                    result.get(pipelineWhichTriggersMe).add(currentPipeline);
                }
            }
        }
        return result;
    }

    @Override
    public List<PipelineConfig> pipelinesForFetchArtifacts(String pipelineName) {
        PipelineConfig currentPipeline = pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        List<PipelineConfig> pipelinesForFetchArtifact = currentPipeline.allFirstLevelUpstreamPipelines(this);
        pipelinesForFetchArtifact.add(currentPipeline);
        return pipelinesForFetchArtifact;
    }

    @Override
    public Map<CaseInsensitiveString, List<CaseInsensitiveString>> templatesWithPipelinesForUser(String username) {
        HashMap<CaseInsensitiveString, List<CaseInsensitiveString>> templateToPipelines = new HashMap<CaseInsensitiveString, List<CaseInsensitiveString>>();
        for (PipelineTemplateConfig template : getTemplates()) {
            if (isAdministrator(username) || template.getAuthorization().getAdminsConfig().isAdmin(new AdminUser(new CaseInsensitiveString(username)), null)) {
                templateToPipelines.put(template.name(), new ArrayList<CaseInsensitiveString>());
            }
        }
        for (PipelineConfig pipelineConfig : getAllPipelineConfigs()) {
            CaseInsensitiveString name = pipelineConfig.getTemplateName();
            if (pipelineConfig.hasTemplate() && templateToPipelines.containsKey(name)) {
                templateToPipelines.get(name).add(pipelineConfig.name());
            }
        }
        return templateToPipelines;
    }

    @Override
    public boolean isArtifactCleanupProhibited(String pipelineName, String stageName) {
        if (!hasStageConfigNamed(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName), true)) {
            return false;
        }
        StageConfig stageConfig = stageConfigByName(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
        return stageConfig.isArtifactCleanupProhibited();
    }

    @Override
    public MaterialConfig materialConfigFor(String fingerprint) {
        for (MaterialConfig materialConfig : getUniqueMaterialConfigs(false)) {
            if (materialConfig.getFingerprint().equals(fingerprint)) {
                return materialConfig;
            }
        }
        return null;
    }
    private Set<MaterialConfig> getUniqueMaterialConfigs(boolean ignoreManualPipelines) {
        Set<MaterialConfig> materialConfigs = new HashSet<MaterialConfig>();
        Set<Map> uniqueMaterials = new HashSet<Map>();
        for (PipelineConfig pipelineConfig : this.getAllPipelineConfigs()) {
            for (MaterialConfig materialConfig : pipelineConfig.materialConfigs()) {
                if (!uniqueMaterials.contains(materialConfig.getSqlCriteria())) {
                    if (ignoreManualPipelines && !materialConfig.isAutoUpdate() && materialConfig instanceof ScmMaterialConfig) {
                        continue;
                    }
                    materialConfigs.add(materialConfig);
                    uniqueMaterials.add(materialConfig.getSqlCriteria());
                }
            }
        }
        return materialConfigs;
    }

    @Override
    public boolean canDeletePackageRepository(PackageRepository repository) {
        return groups.canDeletePackageRepository(repository);
    }

    @Override
    public boolean canDeletePluggableSCMMaterial(SCM scmConfig) {
        return groups.canDeletePluggableSCMMaterial(scmConfig);
    }

    // simple passthrough to main configuration

    @Override
    public Agents agents() {
        return this.main.agents();
    }

    @Override
    public ServerConfig server() {
        return this.main.server();
    }

    @Override
    public MailHost mailHost() {
        return this.main.mailHost();
    }

    @Override
    public boolean isSmtpEnabled() {
        return this.main.isSmtpEnabled();
    }

    @Override
    public void setServerConfig(ServerConfig serverConfig) {
        this.main.setServerConfig(serverConfig);
    }

    @Override
    public String adminEmail() {
        return this.main.adminEmail();
    }

    @Override
    public boolean isMailHostConfigured() {
        return this.main.isMailHostConfigured();
    }

    @Override
    public boolean isAdministrator(String username) {
        return this.main.isAdministrator(username);
    }

    @Override
    public boolean doesRoleHaveAdminPrivileges(String rolename) {
        return this.main.doesRoleHaveAdminPrivileges(rolename);
    }

    @Override
    public void removeRole(Role roleToDelete) {
        if (doesAdminConfigContainRole(roleToDelete.getName().toString())) {
            server().security().adminsConfig().removeRole(roleToDelete);
        }
        for (PipelineConfigs group : this.getGroups()) {
            // this can fail if used in remote config
            group.cleanupAllUsagesOfRole(roleToDelete);
        }
        server().security().deleteRole(roleToDelete);
    }

    @Override
    public boolean doesAdminConfigContainRole(String roleToDelete) {
        return this.main.doesAdminConfigContainRole(roleToDelete);
    }

    @Override
    public boolean isLicenseValid() {
        return this.main.isLicenseValid();
    }

    @Override
    public FeatureSupported validateFeature(EnterpriseFeature enterpriseFeature) {
        return this.main.validateFeature(enterpriseFeature);
    }

    @Override
    public Edition validEdition() {
        return this.main.validEdition();
    }

    @Override
    public LicenseValidity licenseValidity() {
        return this.main.licenseValidity();
    }

    @Override
    public boolean isSecurityEnabled() {
        return this.main.isSecurityEnabled();
    }

    @Override
    public TemplatesConfig getTemplates() {
        return this.main.getTemplates();
    }

    @Override
    public PipelineTemplateConfig findTemplate(CaseInsensitiveString templateName) {
        return this.main.findTemplate(templateName);
    }

    @Override
    public void addTemplate(PipelineTemplateConfig pipelineTemplate) {
        this.main.addTemplate(pipelineTemplate);
    }

    @Override
    public PipelineTemplateConfig getTemplateByName(CaseInsensitiveString pipeline) {
        return this.main.getTemplateByName(pipeline);
    }

    @Override
    public void setTemplates(TemplatesConfig templates) {
        this.main.setTemplates(templates);
    }

    @Override
    public Edition edition() {
        return this.main.edition();
    }

    @Override
    public String sanitizedGroupName(String name) {
        return this.main.sanitizedGroupName(name);
    }

    @Override
    public void removePackageRepository(String id) {
        this.main.removePackageRepository(id);
    }

    @Override
    public PackageRepositories getPackageRepositories() {
        return this.main.getPackageRepositories();
    }

    @Override
    public void savePackageRepository(PackageRepository packageRepository) {
        this.main.savePackageRepository(packageRepository);
    }

    @Override
    public void savePackageDefinition(PackageDefinition packageDefinition) {
        this.main.savePackageDefinition(packageDefinition);
    }

    @Override
    public void setPackageRepositories(PackageRepositories packageRepositories) {
        this.main.setPackageRepositories(packageRepositories);
    }

    @Override
    public SCMs getSCMs() {
        return this.main.getSCMs();
    }

    @Override
    public void setSCMs(SCMs scms) {
        this.main.setSCMs(scms);
    }

    @Override
    public ConfigReposConfig getConfigRepos() {
        return this.main.getConfigRepos();
    }

    @Override
    public void setConfigRepos(ConfigReposConfig repos) {
        this.main.setConfigRepos(repos);
    }


    private static abstract class ErrorCollectingHandler implements GoConfigGraphWalker.Handler {
        private final List<ConfigErrors> allErrors;

        public ErrorCollectingHandler(List<ConfigErrors> allErrors) {
            this.allErrors = allErrors;
        }

        public void handle(Validatable validatable, ValidationContext context) {
            handleValidation(validatable, context);
            ConfigErrors configErrors = validatable.errors();

            if (!configErrors.isEmpty()) {
                allErrors.add(configErrors);
            }
        }

        public abstract void handleValidation(Validatable validatable, ValidationContext context);
    }

    private static class FindPipelineGroupAdminstrator implements PipelineGroupVisitor {
        private final CaseInsensitiveString username;
        private final List<Role> roles;
        private boolean isGroupAdmin;

        public FindPipelineGroupAdminstrator(CaseInsensitiveString username, List<Role> roles) {
            this.username = username;
            this.roles = roles;
        }

        public void visit(PipelineConfigs pipelineConfigs) {
            if (pipelineConfigs.getAuthorization().isUserAnAdmin(username, roles)) {
                isGroupAdmin = true;
            }
        }
    }

}
