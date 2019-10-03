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
import com.thoughtworks.go.config.elastic.ElasticConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.StageNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.merge.MergeConfigOrigin;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.merge.MergePipelineConfigs;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.DFSCycleDetector;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.Node;
import com.thoughtworks.go.util.PipelineDependencyState;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.thoughtworks.go.config.exceptions.EntityType.Pipeline;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static java.util.stream.Collectors.toMap;

/**
 * @understands the configuration for cruise
 */
@ConfigTag("cruise")
public class BasicCruiseConfig implements CruiseConfig {
    @ConfigSubtag
    @SkipParameterResolution
    private ServerConfig serverConfig = new ServerConfig();
    @ConfigSubtag
    @SkipParameterResolution
    private ElasticConfig elasticConfig = new ElasticConfig();
    @ConfigSubtag
    @SkipParameterResolution
    private com.thoughtworks.go.domain.packagerepository.PackageRepositories packageRepositories = new PackageRepositories();
    @ConfigSubtag
    @SkipParameterResolution
    private SCMs scms = new SCMs();
    @ConfigSubtag
    @SkipParameterResolution
    private ConfigReposConfig configRepos = new ConfigReposConfig();
    @ConfigSubtag
    @SkipParameterResolution
    private ArtifactStores artifactStores = new ArtifactStores();
    @ConfigSubtag
    @SkipParameterResolution
    private SecretConfigs secretConfigs = new SecretConfigs();
    @ConfigSubtag(label = "groups")
    private PipelineGroups groups = new PipelineGroups();
    @ConfigSubtag(label = "templates")
    @SkipParameterResolution
    private TemplatesConfig templatesConfig = new TemplatesConfig();
    @ConfigSubtag
    @SkipParameterResolution
    private EnvironmentsConfig environments = new EnvironmentsConfig();

    @IgnoreTraversal
    private CruiseStrategy strategy;

    //This is set reflective by the MagicalGoConfigXmlLoader
    private String md5;
    private ConfigErrors errors = new ConfigErrors();

    @IgnoreTraversal
    private PipelineNameToConfigMap pipelineNameToConfigMap;
    @IgnoreTraversal
    private AllPipelineConfigs allPipelineConfigs;

    @IgnoreTraversal
    private List<PartialConfig> partials = new ArrayList<>();

    @IgnoreTraversal
    private transient AllTemplatesWithAssociatedPipelines allTemplatesWithAssociatedPipelines;

    public BasicCruiseConfig() {
        strategy = new BasicStrategy();
    }

    public BasicCruiseConfig(BasicCruiseConfig main, boolean forEdit, PartialConfig... parts) {
        List<PartialConfig> partList = Arrays.asList(parts);
        createMergedConfig(main, partList, forEdit);
    }

    public BasicCruiseConfig(BasicCruiseConfig main, PartialConfig... parts) {
        List<PartialConfig> partList = Arrays.asList(parts);
        createMergedConfig(main, partList, false);
    }

    public BasicCruiseConfig(BasicCruiseConfig main, List<PartialConfig> parts) {
        createMergedConfig(main, parts, false);
    }

    @Override
    public void merge(List<PartialConfig> partList, boolean forEdit) {
        if (partList.isEmpty()) {
            return;
        }
        partList = removePartialsThatDoNotCorrespondToTheCurrentConfigReposList(partList);

        if (strategy instanceof MergeStrategy)
            throw new RuntimeException("cannot merge partials to already merged configuration");
        MergeStrategy mergeStrategy = new MergeStrategy(partList, forEdit);
        this.strategy = mergeStrategy;
        groups = mergeStrategy.mergePipelineConfigs();
        environments = mergeStrategy.mergeEnvironmentConfigs();
        scms = mergeStrategy.mergeScms();
        resetAllPipelineConfigsCache();
    }

    private List<PartialConfig> removePartialsThatDoNotCorrespondToTheCurrentConfigReposList(List<PartialConfig> partList) {
        List<Object> notToBeMerged = new ArrayList<>();
        for (PartialConfig partialConfig : partList) {
            if (partialConfig.getOrigin() instanceof RepoConfigOrigin) {
                RepoConfigOrigin origin = (RepoConfigOrigin) partialConfig.getOrigin();
                if (!configRepos.hasMaterialWithFingerprint(origin.getMaterial().getFingerprint()))
                    notToBeMerged.add(partialConfig);
            }
        }
        partList = ListUtils.removeAll(partList, notToBeMerged);
        return partList;
    }

    private void resetAllPipelineConfigsCache() {
        allPipelineConfigs = null;
        pipelineNameToConfigMap = null;
        allTemplatesWithAssociatedPipelines = null;
    }

    private void createMergedConfig(BasicCruiseConfig main, List<PartialConfig> partList, boolean forEdit) {
        this.serverConfig = main.serverConfig;
        this.packageRepositories = main.packageRepositories;
        this.scms = main.scms;
        this.templatesConfig = main.templatesConfig;
        this.configRepos = main.configRepos;
        this.groups = main.groups;
        this.environments = main.environments;

        MergeStrategy mergeStrategy = new MergeStrategy(partList, forEdit);
        this.strategy = mergeStrategy;

        groups = mergeStrategy.mergePipelineConfigs();
        environments = mergeStrategy.mergeEnvironmentConfigs();
    }

    // for tests
    public BasicCruiseConfig(PipelineConfigs... groups) {
        for (PipelineConfigs pipelineConfigs : groups) {
            this.groups.add(pipelineConfigs);
        }
        strategy = new BasicStrategy();
    }

    @Override
    @PostConstruct
    public void initializeServer() {
        serverConfig.ensureServerIdExists();
        serverConfig.ensureAgentAutoregisterKeyExists();
        serverConfig.ensureArtifactConfigExists();
    }

    @Override
    public ElasticConfig getElasticConfig() {
        return elasticConfig;
    }

    @Override
    public void setElasticConfig(ElasticConfig elasticConfig) {
        this.elasticConfig = elasticConfig;
    }

    private interface CruiseStrategy {
        ConfigOrigin getOrigin();

        void setOrigins(ConfigOrigin origins);

        List<PipelineConfig> getAllLocalPipelineConfigs(boolean excludeMembersOfRemoteEnvironments);

        List<PartialConfig> getMergedPartials();

        boolean isLocal();
    }

    private class BasicStrategy implements CruiseStrategy {

        private ConfigOrigin origin;

        public BasicStrategy() {
            origin = new FileConfigOrigin();
        }

        @Override
        public ConfigOrigin getOrigin() {
            return origin;
        }

        @Override
        public void setOrigins(ConfigOrigin origins) {
            origin = origins;
            for (EnvironmentConfig env : environments) {
                env.setOrigins(origins);
            }
            for (PipelineConfigs pipes : groups) {
                pipes.setOrigins(origins);
            }
            for (SCM scm : scms) {
                scm.setOrigins(origin);
            }
        }

        @Override
        public List<PartialConfig> getMergedPartials() {
            return new ArrayList<>();
        }

        @Override
        public List<PipelineConfig> getAllLocalPipelineConfigs(boolean excludeMembersOfRemoteEnvironments) {
            return getAllPipelineConfigs();
        }

        @Override
        public boolean isLocal() {
            return true;
        }
    }

    private class MergeStrategy implements CruiseStrategy {

        /*
        Skip validating main configuration when merged. For 2 reasons:
         - partial configurations may not be valid by themselves
         - to not duplicate errors copied in final cruise config (main has references
         to the same instances that are part of merged config - see the constructor)

         Main configuration is still validated within its own scope, explicitly, at the right moment,
         But that is done higher in services.
         */

        //@IgnoreTraversal
        //private BasicCruiseConfig main; // this might be causing cloning troubles
        private boolean forEdit;
        private List<PartialConfig> parts = new ArrayList<>();

        public MergeStrategy(List<PartialConfig> parts, boolean forEdit) {
            this.forEdit = forEdit;
            this.parts.addAll(parts);
        }

        private EnvironmentsConfig mergeEnvironmentConfigs() {
            EnvironmentsConfig environments = new EnvironmentsConfig();

            //first add environment configs from main
            List<EnvironmentConfig> allEnvConfigs = new ArrayList<>();
            for (EnvironmentConfig envConfig : BasicCruiseConfig.this.getEnvironments()) {
                allEnvConfigs.add(envConfig);
            }
            // then add from each part
            for (PartialConfig part : this.parts) {
                for (EnvironmentConfig partPipesConf : part.getEnvironments()) {
                    allEnvConfigs.add(partPipesConf);
                }
            }

            // lets group them by environment name
            Map<CaseInsensitiveString, List<EnvironmentConfig>> map = new LinkedHashMap<>();
            for (EnvironmentConfig env : allEnvConfigs) {
                CaseInsensitiveString key = env.name();
                if (map.get(key) == null) {
                    map.put(key, new ArrayList<>());
                }
                map.get(key).add(env);
            }
            for (List<EnvironmentConfig> oneEnv : map.values()) {
                if (forEdit) {
                    // this cruise configuration may be changed (and cloned) later
                    // if all parts are immutable then we must add a piece for edits
                    if (oneEnv.size() == 1) {
                        EnvironmentConfig sole = oneEnv.get(0);
                        if (sole.isLocal()) {
                            // the sole part is editable anyway
                            environments.add(sole);
                        } else {
                            BasicEnvironmentConfig environmentConfigForEdit = new BasicEnvironmentConfig(sole.name());
                            environmentConfigForEdit.setOrigins(new UIConfigOrigin());
                            environments.add(new MergeEnvironmentConfig(environmentConfigForEdit, sole));
                        }
                    } else {
                        MergeEnvironmentConfig merge = new MergeEnvironmentConfig(oneEnv);
                        if (merge.getFirstEditablePartOrNull() == null) {
                            //no parts to edit, we must add one
                            BasicEnvironmentConfig environmentConfigForEdit = new BasicEnvironmentConfig(merge.name());
                            environmentConfigForEdit.setOrigins(new UIConfigOrigin());
                            merge.add(environmentConfigForEdit);
                        }
                        environments.add(merge);
                    }
                } else {
                    // there will not be any modifications on this config.
                    // just keep all parts in simple form
                    if (oneEnv.size() == 1)
                        environments.add(oneEnv.get(0));
                    else
                        environments.add(new MergeEnvironmentConfig(oneEnv));
                }
            }

            return environments;
        }

        private SCMs mergeScms() {
            SCMs scms = new SCMs();
            scms.addAll(BasicCruiseConfig.this.scms);

            for (PartialConfig part : this.parts) {
                scms.addAll(part.getScms());
            }

            SCMs mergedScms = new SCMs();
            for (SCM scm : scms) {
                if (forEdit) {
                    if (scm.isLocal()) {
                        mergedScms.add(scm);
                    }
                } else {
                    mergedScms.add(scm);
                }
            }
            return mergedScms;
        }

        private PipelineGroups mergePipelineConfigs() {
            PipelineGroups groups = new PipelineGroups();

            // first add pipeline configs from main part
            List<PipelineConfigs> allPipelineConfigs = new ArrayList<>();
            for (PipelineConfigs partPipesConf : BasicCruiseConfig.this.getGroups()) {
                allPipelineConfigs.add(partPipesConf);
            }
            // then add from each part
            for (PartialConfig part : this.parts) {
                for (PipelineConfigs partPipesConf : part.getGroups()) {
                    allPipelineConfigs.add(partPipesConf);
                }
            }
            //there may be duplicated names and conflicts in general in the PipelineConfigs

            // lets group them by 'pipeline group' name
            Map<String, List<PipelineConfigs>> map = new LinkedHashMap<>();
            for (PipelineConfigs pipes : allPipelineConfigs) {
                String key = pipes.getGroup();
                if (map.get(key) == null) {
                    map.put(key, new ArrayList<>());
                }
                map.get(key).add(pipes);
            }

            for (List<PipelineConfigs> oneGroup : map.values()) {
                if (forEdit) {
                    // this cruise configuration may be changed (and cloned) later
                    // if all parts are immutable then we must add a piece for edits
                    if (oneGroup.size() == 1) {
                        PipelineConfigs sole = oneGroup.get(0);
                        if (sole.isLocal()) {
                            // the sole part is editable anyway
                            groups.add(sole);
                        } else {
                            BasicPipelineConfigs pipelineConfigsForEdit = new BasicPipelineConfigs();
                            pipelineConfigsForEdit.setGroup(sole.getGroup());
                            pipelineConfigsForEdit.setOrigins(new UIConfigOrigin());
                            groups.add(new MergePipelineConfigs(pipelineConfigsForEdit, sole));
                        }
                    } else {
                        MergePipelineConfigs merge = new MergePipelineConfigs(oneGroup);
                        if (merge.getFirstEditablePartOrNull() == null) {
                            //no parts to edit, we must add one
                            BasicPipelineConfigs pipelineConfigsForEdit = new BasicPipelineConfigs();
                            pipelineConfigsForEdit.setGroup(merge.getGroup());
                            pipelineConfigsForEdit.setOrigins(new UIConfigOrigin());
                            merge.addPart(pipelineConfigsForEdit);
                        }
                        groups.add(merge);
                    }
                } else {
                    // there will not be any modifications on this config.
                    // just keep all parts in simple form
                    if (oneGroup.size() == 1)
                        groups.add(oneGroup.get(0));
                    else
                        groups.add(new MergePipelineConfigs(oneGroup));
                }
            }

            return groups;
        }

        @Override
        public ConfigOrigin getOrigin() {
            return new MergeConfigOrigin();
        }

        @Override
        public void setOrigins(ConfigOrigin origins) {
            throw bomb("Cannot set origins on merged config");
        }

        @Override
        public List<PartialConfig> getMergedPartials() {
            return this.parts;
        }

        private void verifyUniqueNameInParts(PipelineConfig pipelineConfig) {
            for (PartialConfig part : this.parts) {
                for (PipelineConfigs partGroup : part.getGroups()) {
                    if (partGroup.hasPipeline(pipelineConfig.name())) {
                        throw bomb("Pipeline called '" + pipelineConfig.name() +
                                "' is already defined in configuration repository " +
                                part.getOrigin().displayName());
                    }

                }
            }
        }

        @Override
        public List<PipelineConfig> getAllLocalPipelineConfigs(boolean excludeMembersOfRemoteEnvironments) {
            List<PipelineConfig> locals = new ArrayList<>();

            PipelineGroups localGroups = BasicCruiseConfig.this.groups.getLocal();
            for (PipelineConfigs pipelineConfigs : localGroups) {
                if (pipelineConfigs.getOrigin() instanceof UIConfigOrigin) {
                    //then we have injected this so that UI has a piece to edit
                    // we want to keep it only if there is something added
                    if (!pipelineConfigs.isEmpty()) {
                        for (PipelineConfig pipelineConfig : pipelineConfigs.getPipelines()) {
                            if (excludeMembersOfRemoteEnvironments && BasicCruiseConfig.this.getEnvironments().isPipelineAssociatedWithRemoteEnvironment(pipelineConfig.name()))
                                continue;
                            locals.add(pipelineConfig);
                        }

                    }
                } else {
                    //origin is local file

                    for (PipelineConfig pipelineConfig : pipelineConfigs.getPipelines()) {
                        if (excludeMembersOfRemoteEnvironments && BasicCruiseConfig.this.getEnvironments().isPipelineAssociatedWithRemoteEnvironment(pipelineConfig.name()))
                            continue;
                        locals.add(pipelineConfig);
                    }

                }
            }
            return locals;
        }

        @Override
        public boolean isLocal() {
            return false;
        }
    }

    @Override
    public CruiseConfig cloneForValidation() {
        Cloner cloner = new Cloner();
        BasicCruiseConfig configForValidation = cloner.deepClone(BasicCruiseConfig.this);
        // This needs to be done clear the cached fields else the cloned object will all get them.
        configForValidation.resetAllPipelineConfigsCache();
        return configForValidation;
    }

    @Override
    public boolean canViewAndEditTemplates(CaseInsensitiveString username) {
        return isAdministrator(username.toString()) || getTemplates().canViewAndEditTemplate(username, rolesForUser(username));
    }

    @Override
    public boolean isAuthorizedToEditTemplate(CaseInsensitiveString templateName, CaseInsensitiveString username) {
        if (isAdministrator(username.toString())) {
            return true;
        }

        PipelineTemplateConfig template = getTemplateByName(templateName);
        return isAuthorizedToEditTemplate(template, username);
    }

    @Override
    public boolean isAuthorizedToEditTemplate(PipelineTemplateConfig templateConfig, CaseInsensitiveString username) {
        if (isAdministrator(username.toString())) {
            return true;
        }
        return templateConfig.canBeEditedBy(username, rolesForUser(username));
    }

    @Override
    public boolean isAuthorizedToViewTemplate(CaseInsensitiveString templateName, CaseInsensitiveString username) {
        if (isAdministrator(username.toString())) {
            return true;
        }

        PipelineTemplateConfig template = getTemplateByName(templateName);
        return isAuthorizedToViewTemplate(template, username);
    }

    @Override
    public boolean isAuthorizedToViewTemplate(PipelineTemplateConfig templateConfig, CaseInsensitiveString username) {
        if (isAuthorizedToEditTemplate(templateConfig, username)) {
            return true;
        }
        return templateConfig.getAuthorization().isViewUser(username, rolesForUser(username)) || (templateConfig.isAllowGroupAdmins() && isGroupAdministrator(username));

    }

    @Override
    public boolean isAuthorizedToViewTemplates(CaseInsensitiveString username) {
        return canViewAndEditTemplates(username) || getTemplates().canUserViewTemplates(username, rolesForUser(username), isGroupAdministrator(username));
    }

    private List<Role> rolesForUser(CaseInsensitiveString username) {
        return server().security().getRoles().memberRoles(new AdminUser(username));
    }

    @Override
    public void validate(ValidationContext validationContext) {
        areThereCyclicDependencies();
    }

    @Override
    public Hashtable<CaseInsensitiveString, Node> getDependencyTable() {
        final Hashtable<CaseInsensitiveString, Node> hashtable = new Hashtable<>();
        this.accept((PiplineConfigVisitor) pipelineConfig -> hashtable.put(pipelineConfig.name(), pipelineConfig.getDependenciesAsNode()));
        return hashtable;
    }

    private class DependencyTable implements PipelineDependencyState {
        private Hashtable<CaseInsensitiveString, Node> targetTable;

        public DependencyTable(Hashtable<CaseInsensitiveString, Node> targetTable) {
            this.targetTable = targetTable;
        }

        @Override
        public boolean hasPipeline(CaseInsensitiveString key) {
            return targetTable.containsKey(key);
        }

        @Override
        public Node getDependencyMaterials(CaseInsensitiveString pipeline) {
            return targetTable.get(pipeline);
        }
    }

    private void areThereCyclicDependencies() {
        final DFSCycleDetector dfsCycleDetector = new DFSCycleDetector();
        final Hashtable<CaseInsensitiveString, Node> dependencyTable = getDependencyTable();
        List<PipelineConfig> pipelineConfigs = this.getAllPipelineConfigs();
        DependencyTable pipelineDependencyState = new DependencyTable(dependencyTable);
        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            try {
                dfsCycleDetector.topoSort(pipelineConfig.name(), pipelineDependencyState);
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
    public StageConfig stageConfigByName(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
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
        if (pipelineNameToConfigMap == null) {
            pipelineNameToConfigMap = new PipelineNameToConfigMap();
        }
        if (pipelineNameToConfigMap.containsKey(name)) {
            return pipelineNameToConfigMap.get(name);
        }
        PipelineConfig pipelineConfig = getPipelineConfigByName(name);
        if (pipelineConfig == null) {
            throw new RecordNotFoundException(Pipeline, name);
        }
        pipelineNameToConfigMap.putIfAbsent(pipelineConfig.name(), pipelineConfig);

        return pipelineConfig;
    }

    @Override
    public boolean hasStageConfigNamed(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName, boolean ignoreCase) {
        PipelineConfig pipelineConfig = getPipelineConfigByName(pipelineName);
        if (pipelineConfig == null) {
            return false;
        }
        return pipelineConfig.findBy(stageName) != null;
    }

    @Override
    public PipelineConfig getPipelineConfigByName(CaseInsensitiveString pipelineName) {
        return pipelinesFromAllGroups().findBy(pipelineName);
    }

    @Override
    public boolean hasPipelineNamed(final CaseInsensitiveString pipelineName) {
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
    public ServerConfig server() {
        return serverConfig;
    }

    @Override
    public MailHost mailHost() {
        return serverConfig.mailHost();
    }

    @Override
    public EnvironmentsConfig getEnvironments() {
        return environments;
    }

    private PipelineConfigs pipelinesFromAllGroups() {
        //#2388 - hack to flatten all pipelines.  We need a "pipelineGroup" model
        return new BasicPipelineConfigs(allPipelines().toArray(new PipelineConfig[0]));
    }


    @Override
    public List<PipelineConfig> allPipelines() {
        List<PipelineConfig> configs = new ArrayList<>();
        for (PipelineConfigs group : groups) {
            for (PipelineConfig pipeline : group) {
                configs.add(pipeline);
            }
        }
        return configs;
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
    public int schemaVersion() {
        return GoConstants.CONFIG_SCHEMA_VERSION;
    }

    @Override
    public Set<MaterialConfig> getAllUniquePostCommitSchedulableMaterials() {
        Set<MaterialConfig> materialConfigs = new HashSet<>();
        Set<String> uniqueMaterials = new HashSet<>();
        for (PipelineConfigs pipelineConfigs : this.groups) {
            for (PipelineConfig pipelineConfig : pipelineConfigs) {
                for (MaterialConfig materialConfig : pipelineConfig.materialConfigs()) {
                    if ((materialConfig instanceof ScmMaterialConfig || materialConfig instanceof PluggableSCMMaterialConfig)
                            && !materialConfig.isAutoUpdate()
                            && !uniqueMaterials.contains(materialConfig.getFingerprint())) {
                        materialConfigs.add(materialConfig);
                        uniqueMaterials.add(materialConfig.getFingerprint());
                    }
                }
            }
        }
        for (ConfigRepoConfig configRepo : this.configRepos) {
            MaterialConfig materialConfig = configRepo.getMaterialConfig();
            if (!uniqueMaterials.contains(materialConfig.getFingerprint())) {
                materialConfigs.add(materialConfig);
                uniqueMaterials.add(materialConfig.getFingerprint());
            }
        }
        return materialConfigs;
    }

    @Override
    public ConfigReposConfig getConfigRepos() {
        return configRepos;
    }

    @Override
    public void setConfigRepos(ConfigReposConfig repos) {
        configRepos = repos;
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
        for (PipelineConfig pipelineConfig : pipelinesFromAllGroups()) {
            for (StageConfig stageConfig : pipelineConfig) {
                for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
                    visitor.visit(pipelineConfig, stageConfig, jobConfig);
                }
            }
        }
    }

    @Override
    public void accept(TaskConfigVisitor visitor) {
        for (PipelineConfig pipelineConfig : pipelinesFromAllGroups()) {
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
        accept((PipelineGroupVisitor) group -> group.accept(visitor));
    }

    @Override
    public void setGroup(PipelineGroups pipelineGroups) {
        groups = pipelineGroups;
    }

    @Override
    public PipelineGroups getGroups() {
        return groups;
    }

    @Override
    public List<String> getGroupsForUser(CaseInsensitiveString username, List<Role> roles) {
        ArrayList<String> groups = new ArrayList<>();
        for (PipelineConfigs group : this.groups) {
            if (isAdministrator(username.toString()) || group.isUserAnAdmin(username, roles)) {
                groups.add(group.getGroup());
            }
        }
        return groups;
    }

    // when adding pipelines, groups or environments we must make sure that both merged and basic scopes are updated

    @Override
    public void addPipeline(String groupName, PipelineConfig pipelineConfig) {
        groups.addPipeline(groupName, pipelineConfig);
    }

    @Override
    public void deletePipeline(PipelineConfig pipelineConfig) {
        groups.deletePipeline(pipelineConfig);
    }

    @Override
    public void deletePipelineGroup(String groupName) {
        groups.deleteGroup(groupName);
    }

    @Override
    public void setSecretConfigs(SecretConfigs secretConfigs) {
        this.secretConfigs = secretConfigs;
    }

    @Override
    public SecretConfigs getSecretConfigs() {
        return secretConfigs;
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
        return pipelineIndex < pipelinesFromAllGroups().size();
    }

    @Override
    public boolean hasPipeline() {
        return pipelinesFromAllGroups().isEmpty();
    }

    @Override
    public PipelineConfig find(String groupName, int pipelineIndex) {
        return groups.findPipeline(groupName, pipelineIndex);
    }

    //only for test

    @Override
    public int numberOfPipelines() {
        return pipelinesFromAllGroups().size();
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
    public boolean isSmtpEnabled() {
        MailHost mailHost = server().mailHost();
        return mailHost != null && !mailHost.equals(new MailHost(new GoCipher()));
    }

    @Override
    public boolean isInFirstGroup(final CaseInsensitiveString pipelineName) {
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
    public boolean isSecurityEnabled() {
        return server().isSecurityEnabled();
    }

    @Override
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public String adminEmail() {
        if (isSmtpEnabled()) {
            return server().mailHost().getAdminMail();
        } else {
            return null;
        }
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
    public void updateGroup(PipelineConfigs pipelineConfigs, String groupName) {
        PipelineConfigs old = groups.findGroup(groupName);
        int index = groups.indexOf(old);
        groups.set(index, pipelineConfigs);
    }

    @Override
    public boolean isMailHostConfigured() {
        return !(new MailHost(new GoCipher()).equals(mailHost()) || mailHost() == null);
    }

    @Override
    public List<PipelineConfig> getAllPipelineConfigs() {
        if (allPipelineConfigs == null) {
            AllPipelineConfigs configs = new AllPipelineConfigs();
            PipelineGroups groups = getGroups();
            for (PipelineConfigs group : groups) {
                for (PipelineConfig pipelineConfig : group) {
                    configs.add(pipelineConfig);
                }
            }
            allPipelineConfigs = configs;
        }
        return allPipelineConfigs;
    }

    @Override
    public Map<CaseInsensitiveString, PipelineConfig> pipelineConfigsAsMap() {
        return getAllPipelineConfigs().stream().collect(toMap(PipelineConfig::name, Function.identity()));
    }

    @Override
    public List<CaseInsensitiveString> getAllPipelineNames() {
        List<CaseInsensitiveString> names = new ArrayList<>();
        for (PipelineConfig config : getAllPipelineConfigs()) {
            names.add(config.name());
        }
        return names;
    }

    @Override
    public boolean isAdministrator(String username) {
        return hasAdminPrivileges(new AdminUser(new CaseInsensitiveString(username)));
    }

    private boolean hasAdminPrivileges(Admin admin) {
        return server().security().isAdmin(admin);
    }

    // For tests

    @Override
    public void setEnvironments(EnvironmentsConfig environments) {
        this.environments = environments;
    }

    @Override
    public Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelines() {
        return getUniqueMaterials(true, true);
    }

    @Override
    public Set<MaterialConfig> getAllUniqueMaterialsBelongingToAutoPipelinesAndConfigRepos() {
        return getUniqueMaterials(true, false);
    }

    @Override
    public Set<MaterialConfig> getAllUniqueMaterials() {
        return getUniqueMaterials(false, true);
    }

    @Override
    public Set<MaterialConfig> getAllUniqueMaterialsOfPipelinesAndConfigRepos() {
        return getUniqueMaterials(false, false);
    }

    private Set<MaterialConfig> getUniqueMaterials(boolean ignoreManualPipelines, boolean ignoreConfigRepos) {
        Set<MaterialConfig> materialConfigs = new HashSet<>();
        Set<Map> uniqueMaterials = new HashSet<>();
        for (PipelineConfig pipelineConfig : pipelinesFromAllGroups()) {
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
        if (!ignoreConfigRepos) {
            for (ConfigRepoConfig configRepo : this.configRepos) {
                MaterialConfig materialConfig = configRepo.getMaterialConfig();
                if (!uniqueMaterials.contains(materialConfig.getSqlCriteria())) {
                    materialConfigs.add(materialConfig);
                    uniqueMaterials.add(materialConfig.getSqlCriteria());
                }
            }
        }
        return materialConfigs;
    }

    private Set<MaterialConfig> getUniqueMaterialConfigs(boolean ignoreManualPipelines) {
        Set<MaterialConfig> materialConfigs = new HashSet<>();
        Set<Map> uniqueMaterials = new HashSet<>();
        for (PipelineConfig pipelineConfig : pipelinesFromAllGroups()) {
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
    public Set<StageConfig> getStagesUsedAsMaterials(PipelineConfig pipelineConfig) {
        Set<String> stagesUsedAsMaterials = new HashSet<>();
        for (MaterialConfig materialConfig : getAllUniqueMaterials()) {
            if (materialConfig instanceof DependencyMaterialConfig) {
                DependencyMaterialConfig dep = (DependencyMaterialConfig) materialConfig;
                stagesUsedAsMaterials.add(dep.getPipelineName() + "|" + dep.getStageName());
            }
        }
        Set<StageConfig> stages = new HashSet<>();
        for (StageConfig stage : pipelineConfig) {
            if (stagesUsedAsMaterials.contains(pipelineConfig.name() + "|" + stage.name())) {
                stages.add(stage);
            }
        }
        return stages;
    }

    @Override
    public EnvironmentConfig addEnvironment(String environmentName) {
        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        this.addEnvironment(environmentConfig);
        return environmentConfig;
    }

    @Override
    public void addEnvironment(BasicEnvironmentConfig config) {
        environments.add(config);
    }

    @Override
    public Boolean isPipelineLockable(String pipelineName) {
        PipelineConfig pipelineConfig = pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        if (pipelineConfig.hasExplicitLock()) {
            return pipelineConfig.explicitLock();
        }
        return false;
    }

    @Override
    public boolean isPipelineUnlockableWhenFinished(String pipelineName) {
        return pipelineConfigByName(new CaseInsensitiveString(pipelineName)).isPipelineUnlockableWhenFinished();
    }

    @Override
    public Set<ResourceConfig> getAllResources() {
        final HashSet<ResourceConfig> resourceConfigs = new HashSet<>();
        accept((pipelineConfig, stageConfig, jobConfig) -> resourceConfigs.addAll(jobConfig.resourceConfigs()));
        return resourceConfigs;
    }

    @Override
    public TemplatesConfig getTemplates() {
        return templatesConfig;
    }

    @Override
    public PipelineTemplateConfig findTemplate(CaseInsensitiveString templateName) {
        for (PipelineTemplateConfig config : templatesConfig) {
            if (templateName.equals(config.name())) {
                return config;
            }
        }
        return null;
    }

    @Override
    public void addTemplate(PipelineTemplateConfig pipelineTemplate) {
        templatesConfig.add(pipelineTemplate);
    }

    @Override
    public PipelineTemplateConfig getTemplateByName(CaseInsensitiveString pipeline) {
        PipelineTemplateConfig template = getTemplates().templateByName(pipeline);
        if (template == null) {
            throw bomb(String.format("Template %s was not found.", pipeline));
        }
        return template;
    }

    @Override
    public void setTemplates(TemplatesConfig templates) {
        this.templatesConfig = templates;
    }

    @Override
    public void makePipelineUseTemplate(CaseInsensitiveString pipelineName, CaseInsensitiveString templateName) {
        pipelineConfigByName(pipelineName).templatize(templateName);
    }

    @Override
    public Iterable<PipelineConfig> getDownstreamPipelines(String pipelineName) {
        ArrayList<PipelineConfig> configs = new ArrayList<>();
        for (PipelineConfig pipelineConfig : pipelinesFromAllGroups()) {
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
    public EnvironmentVariablesConfig variablesFor(String pipelineName) {
        EnvironmentVariablesConfig pipelineVariables = pipelineConfigByName(new CaseInsensitiveString(pipelineName)).getVariables();
        EnvironmentConfig environment = this.environments.findEnvironmentForPipeline(new CaseInsensitiveString(pipelineName));
        return environment != null ? environment.getVariables().overrideWith(pipelineVariables) : pipelineVariables;
    }

    @Override
    public boolean isGroupAdministrator(final CaseInsensitiveString userName) {
        final List<Role> roles = server().security().memberRoleFor(userName);
        FindPipelineGroupAdminstrator finder = new FindPipelineGroupAdminstrator(userName, roles);
        groups.accept(finder);
        return finder.isGroupAdmin;
    }

    @Override
    public String getMd5() {
        return md5;
    }

    @Override
    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    @Override
    public List<ConfigErrors> getAllErrorsExceptFor(Validatable skipValidatable) {
        List<ConfigErrors> all = getAllErrors();
        if (skipValidatable != null) {
            all.removeAll(ErrorCollector.getAllErrors(skipValidatable));
        }
        return all;
    }

    @Override
    public List<ConfigErrors> validateAfterPreprocess() {
        final List<ConfigErrors> allErrors = new ArrayList<>();
        new GoConfigGraphWalker(this).walk(new ErrorCollectingHandler(allErrors) {
            @Override
            public void handleValidation(Validatable validatable, ValidationContext context) {
                validatable.validate(context);
            }
        });
        return allErrors;
    }

    @Override
    public void copyErrorsTo(CruiseConfig to) {
        copyErrors(this, to);
    }

    public static <T> void copyErrors(T from, T to) {
        GoConfigParallelGraphWalker walker = new GoConfigParallelGraphWalker(from, to);
        walker.walk((rawObject, objectWithErrors) -> rawObject.errors().addAll(objectWithErrors.errors()));
    }

    public static void clearErrors(Validatable obj) {
        GoConfigGraphWalker walker = new GoConfigGraphWalker(obj);
        walker.walk((validatable, ctx) -> validatable.errors().clear());
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
    public Map<CaseInsensitiveString, List<PipelineConfig>> generatePipelineVsDownstreamMap() {
        List<PipelineConfig> pipelineConfigs = getAllPipelineConfigs();
        Map<CaseInsensitiveString, List<PipelineConfig>> result = new HashMap<>();

        for (PipelineConfig currentPipeline : pipelineConfigs) {
            CaseInsensitiveString currentPipelineName = currentPipeline.name();
            if (!result.containsKey(currentPipelineName)) {
                result.put(currentPipelineName, new ArrayList<>());
            }

            for (MaterialConfig materialConfig : currentPipeline.materialConfigs()) {
                if (materialConfig instanceof DependencyMaterialConfig) {
                    CaseInsensitiveString pipelineWhichTriggersMe = ((DependencyMaterialConfig) materialConfig).getPipelineName();
                    if (!result.containsKey(pipelineWhichTriggersMe)) {
                        result.put(pipelineWhichTriggersMe, new ArrayList<>());
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
    public Map<CaseInsensitiveString, Map<CaseInsensitiveString, Authorization>> templatesWithAssociatedPipelines() {
        if (allTemplatesWithAssociatedPipelines == null) {
            allTemplatesWithAssociatedPipelines = new AllTemplatesWithAssociatedPipelines();
            for (PipelineTemplateConfig templateConfig : getTemplates()) {
                if (!allTemplatesWithAssociatedPipelines.containsKey(templateConfig.name())) {
                    allTemplatesWithAssociatedPipelines.put(templateConfig.name(), new HashMap<>());
                }
            }

            for (PipelineConfigs pipelineConfigs : getGroups()) {
                List<PipelineConfig> pipelines = pipelineConfigs.getPipelines();
                for (PipelineConfig pipeline : pipelines) {
                    if (pipeline.hasTemplate()) {
                        Map<CaseInsensitiveString, Authorization> authorizationMap = allTemplatesWithAssociatedPipelines.get(pipeline.getTemplateName());
                        authorizationMap.put(pipeline.getName(), pipelineConfigs.getAuthorization());
                    }
                }
            }
        }
        return allTemplatesWithAssociatedPipelines;
    }

    @Override
    public ArrayList<CaseInsensitiveString> pipelinesAssociatedWithTemplate(CaseInsensitiveString templateName) {
        ArrayList<CaseInsensitiveString> pipelines = new ArrayList<>();
        if (templateName != null) {
            for (PipelineConfig pipelineConfig : getAllPipelineConfigs()) {
                if (pipelineConfig.hasTemplate() && pipelineConfig.getTemplateName().equals(templateName)) {
                    pipelines.add(pipelineConfig.getName());
                }
            }
        }
        return pipelines;
    }

    @Override
    public ArrayList<PipelineConfig> pipelineConfigsAssociatedWithTemplate(CaseInsensitiveString templateName) {
        ArrayList<PipelineConfig> pipelines = new ArrayList<>();
        if (templateName != null) {
            for (PipelineConfig pipelineConfig : getAllPipelineConfigs()) {
                if (pipelineConfig.hasTemplate() && pipelineConfig.getTemplateName().equals(templateName)) {
                    pipelines.add(pipelineConfig);
                }
            }
        }
        return pipelines;
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

    @Override
    public MaterialConfig materialConfigFor(CaseInsensitiveString pipelineName, String fingerprint) {
        PipelineConfig pipelineConfig = pipelineConfigByName(pipelineName);
        MaterialConfigs materialConfigs = pipelineConfig.materialConfigs();
        for (MaterialConfig materialConfig : materialConfigs) {
            if (materialConfig.getFingerprint().equals(fingerprint)) {
                return materialConfig;
            }
        }
        return null;
    }

    @Override
    public String sanitizedGroupName(String name) {
        return BasicPipelineConfigs.sanitizedGroupName(name);
    }

    @Override
    public void removePackageRepository(String id) {
        packageRepositories.removePackageRepository(id);
    }

    @Override
    public PackageRepositories getPackageRepositories() {
        return packageRepositories;
    }

    @Override
    public void savePackageRepository(final PackageRepository packageRepository) {
        packageRepository.clearEmptyConfigurations();
        if (StringUtils.isBlank(packageRepository.getRepoId())) {
            packageRepository.setId(UUID.randomUUID().toString());
        }
        PackageRepository existingPackageRepository = packageRepositories.find(packageRepository.getRepoId());
        if (existingPackageRepository == null) {
            packageRepositories.add(packageRepository);
        } else {
            existingPackageRepository.setName(packageRepository.getName());
            existingPackageRepository.setPluginConfiguration(packageRepository.getPluginConfiguration());
            existingPackageRepository.setConfiguration(packageRepository.getConfiguration());
        }
    }

    @Override
    public void savePackageDefinition(PackageDefinition packageDefinition) {
        packageDefinition.clearEmptyConfigurations();
        PackageRepository packageRepository = packageRepositories.find(packageDefinition.getRepository().getId());
        packageDefinition.setId(UUID.randomUUID().toString());
        packageRepository.addPackage(packageDefinition);
    }

    @Override
    public void setPackageRepositories(PackageRepositories packageRepositories) {
        this.packageRepositories = packageRepositories;
    }

    @Override
    public SCMs getSCMs() {
        return scms;
    }

    @Override
    public void setSCMs(SCMs scms) {
        this.scms = scms;
    }

    @Override
    public boolean canDeletePackageRepository(PackageRepository repository) {
        return groups.canDeletePackageRepository(repository);
    }

    @Override
    public boolean canDeletePluggableSCMMaterial(SCM scmConfig) {
        return groups.canDeletePluggableSCMMaterial(scmConfig);
    }

    @Override
    public List<CaseInsensitiveString> pipelinesAssociatedWithPluggableSCM(SCM scmConfig) {
        List<CaseInsensitiveString> pipelines = new ArrayList<>();
        if (scmConfig != null) {
            for (PipelineConfig pipelineConfig : getAllPipelineConfigs()) {
                for (PluggableSCMMaterialConfig pluggableSCMMaterialConfig : pipelineConfig.pluggableSCMMaterialConfigs()) {
                    if (pluggableSCMMaterialConfig.getScmId().equals(scmConfig.getId())) {
                        pipelines.add(pipelineConfig.getName());
                        break;
                    }
                }
            }
        }
        return pipelines;
    }

    @Override
    public List<PipelineConfig> getAllLocalPipelineConfigs() {
        return strategy.getAllLocalPipelineConfigs(false);
    }

    @Override
    public void setPartials(List<PartialConfig> partials) {
        this.partials = partials;
    }

    @Override
    public List<PartialConfig> getPartials() {
        return partials;
    }

    @Override
    public List<PartialConfig> getMergedPartials() {
        return strategy.getMergedPartials();
    }

    @Override
    public List<PipelineConfig> getAllLocalPipelineConfigs(boolean excludeMembersOfRemoteEnvironments) {
        return strategy.getAllLocalPipelineConfigs(excludeMembersOfRemoteEnvironments);
    }

    @Override
    public boolean isLocal() {
        return this.strategy.isLocal();
    }

    @Override
    public ConfigOrigin getOrigin() {
        return strategy.getOrigin();
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        this.strategy.setOrigins(origins);
    }

    private static class FindPipelineGroupAdminstrator implements PipelineGroupVisitor {
        private final CaseInsensitiveString username;
        private final List<Role> roles;
        private boolean isGroupAdmin;

        public FindPipelineGroupAdminstrator(CaseInsensitiveString username, List<Role> roles) {
            this.username = username;
            this.roles = roles;
        }

        @Override
        public void visit(PipelineConfigs pipelineConfigs) {
            if (pipelineConfigs.getAuthorization().isUserAnAdmin(username, roles)) {
                isGroupAdmin = true;
            }
        }
    }

    @Override
    public ArtifactStores getArtifactStores() {
        return artifactStores;
    }

    @Override
    public void setArtifactStores(ArtifactStores artifactStores) {
        this.artifactStores = artifactStores;
    }

    @Override
    public void encryptSecureProperties(CruiseConfig preprocessed) {
        for (PipelineTemplateConfig template : getTemplates()) {
            template.encryptSecureProperties(preprocessed, preprocessed.getTemplateByName(template.name()));
        }

        for (PipelineConfig pipelineConfig : getAllPipelineConfigs()) {
            pipelineConfig.encryptSecureProperties(preprocessed, preprocessed.pipelineConfigByName(pipelineConfig.name()));
        }

        for (PluginRoleConfig pluginRoleConfig : server().security().getRoles().getPluginRoleConfigs()) {
            pluginRoleConfig.encryptSecureProperties(preprocessed);
        }

        for (ElasticProfile elasticProfile : getElasticConfig().getProfiles()) {
            elasticProfile.encryptSecureProperties(preprocessed);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicCruiseConfig)) return false;

        BasicCruiseConfig that = (BasicCruiseConfig) o;

        if (serverConfig != null ? !serverConfig.equals(that.serverConfig) : that.serverConfig != null) return false;
        if (elasticConfig != null ? !elasticConfig.equals(that.elasticConfig) : that.elasticConfig != null)
            return false;
        if (artifactStores != null ? !artifactStores.equals(that.artifactStores) : that.artifactStores != null)
            return false;
        if (groups != null ? !groups.equals(that.groups) : that.groups != null) return false;
        if (templatesConfig != null ? !templatesConfig.equals(that.templatesConfig) : that.templatesConfig != null)
            return false;
        return environments != null ? environments.equals(that.environments) : that.environments == null;
    }

    @Override
    public int hashCode() {
        int result = serverConfig != null ? serverConfig.hashCode() : 0;
        result = 31 * result + (elasticConfig != null ? elasticConfig.hashCode() : 0);
        result = 31 * result + (artifactStores != null ? artifactStores.hashCode() : 0);
        result = 31 * result + (groups != null ? groups.hashCode() : 0);
        result = 31 * result + (templatesConfig != null ? templatesConfig.hashCode() : 0);
        result = 31 * result + (environments != null ? environments.hashCode() : 0);
        return result;
    }

    public class AllTemplatesWithAssociatedPipelines extends HashMap<CaseInsensitiveString, Map<CaseInsensitiveString, Authorization>> {
    }

    public class AllPipelineConfigs extends ArrayList<PipelineConfig> {
    }

    public class PipelineNameToConfigMap extends ConcurrentHashMap<CaseInsensitiveString, PipelineConfig> {
    }
}
