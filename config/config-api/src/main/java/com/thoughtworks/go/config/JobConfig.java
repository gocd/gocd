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

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.XmlUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @understands configuratin for a job
 */
@ConfigTag("job")
public class JobConfig implements Validatable, ParamsAttributeAware, EnvironmentVariableScope {
    @SkipParameterResolution
    @ConfigAttribute(value = "name", optional = false)
    private CaseInsensitiveString jobName;

    @ConfigSubtag
    private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
    @ConfigSubtag
    private Tasks tasks = new Tasks();
    @ConfigSubtag
    private Tabs tabs = new Tabs();

    @ConfigSubtag
    private ResourceConfigs resourceConfigs = new ResourceConfigs();
    @ConfigSubtag
    private ArtifactTypeConfigs artifactConfigs = new ArtifactTypeConfigs();

    @ConfigAttribute(value = "runOnAllAgents", optional = true) private boolean runOnAllAgents = false;
    @ConfigAttribute(value = "runInstanceCount", optional = true, allowNull = true) private String runInstanceCount;
    @ConfigAttribute(value = "timeout", optional = true, allowNull = true) private String timeout;
    @ConfigAttribute(value = "elasticProfileId", optional = true, allowNull = true) private String elasticProfileId;

    private ConfigErrors errors = new ConfigErrors();
    public static final String NAME = "name";
    public static final String TASKS = "tasks";
    public static final String RESOURCES = "resources";
    public static final String TABS = "tabs";
    public static final String ENVIRONMENT_VARIABLES = "variables";
    public static final String ARTIFACT_CONFIGS = "artifactConfigs";
    public static final String DEFAULT_NAME = "defaultJob";
    public static final String TIMEOUT = "timeout";
    public static final String DEFAULT_TIMEOUT = "defaultTimeout";
    public static final String OVERRIDE_TIMEOUT = "overrideTimeout";
    public static final String NEVER_TIMEOUT = "neverTimeout";
	public static final String RUN_TYPE = "runType";
	public static final String RUN_SINGLE_INSTANCE = "runSingleInstance";
	public static final String RUN_ON_ALL_AGENTS = "runOnAllAgents";
	public static final String RUN_MULTIPLE_INSTANCE = "runMultipleInstance";
	public static final String RUN_INSTANCE_COUNT = "runInstanceCount";
	public static final String ELASTIC_PROFILE_ID = "elasticProfileId";
	private static final String JOB_NAME_PATTERN = "[a-zA-Z0-9_\\-.]+";
    private static final Pattern JOB_NAME_PATTERN_REGEX = Pattern.compile(String.format("^(%s)$", JOB_NAME_PATTERN));

    public JobConfig() {
    }

    public JobConfig(CaseInsensitiveString jobName) {
        this();
        this.jobName = jobName;
    }

    public JobConfig(final CaseInsensitiveString jobName, ResourceConfigs resourceConfigs, ArtifactTypeConfigs artifactConfigs) {
        this(jobName, resourceConfigs, artifactConfigs, new Tasks());
    }

    public JobConfig(final CaseInsensitiveString jobName, ResourceConfigs resourceConfigs, ArtifactTypeConfigs artifactConfigs, Tasks tasks) {
        this(jobName);
        this.resourceConfigs = resourceConfigs;
        this.artifactConfigs = artifactConfigs;
        this.tasks = tasks;
    }

    public JobConfig(String planName) {
        this(new CaseInsensitiveString(planName), new ResourceConfigs(), new ArtifactTypeConfigs());
    }

    @Override
    public CaseInsensitiveString name() {
        return jobName;
    }

    public void setName(String name) {
        setName(new CaseInsensitiveString(name));
    }

    public void setName(CaseInsensitiveString name) {
        this.jobName = name;
    }

    public ResourceConfigs resourceConfigs() {
        return resourceConfigs;
    }

    public void setResourceConfigs(ResourceConfigs resourceConfigs) {
        this.resourceConfigs = resourceConfigs;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobConfig jobConfig = (JobConfig) o;

        if (runOnAllAgents != jobConfig.runOnAllAgents) return false;
        if (jobName != null ? !jobName.equals(jobConfig.jobName) : jobConfig.jobName != null) return false;
        if (variables != null ? !variables.equals(jobConfig.variables) : jobConfig.variables != null) return false;
        if (tasks != null ? !tasks.equals(jobConfig.tasks) : jobConfig.tasks != null) return false;
        if (tabs != null ? !tabs.equals(jobConfig.tabs) : jobConfig.tabs != null) return false;
        if (resourceConfigs != null ? !resourceConfigs.equals(jobConfig.resourceConfigs) : jobConfig.resourceConfigs != null) return false;
        if (artifactConfigs != null ? !artifactConfigs.equals(jobConfig.artifactConfigs) : jobConfig.artifactConfigs != null)
            return false;
        if (runInstanceCount != null ? !runInstanceCount.equals(jobConfig.runInstanceCount) : jobConfig.runInstanceCount != null)
            return false;
        if (timeout != null ? !timeout.equals(jobConfig.timeout) : jobConfig.timeout != null) return false;
        return elasticProfileId != null ? elasticProfileId.equals(jobConfig.elasticProfileId) : jobConfig.elasticProfileId == null;

    }

    @Override
    public int hashCode() {
        int result = jobName != null ? jobName.hashCode() : 0;
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        result = 31 * result + (tasks != null ? tasks.hashCode() : 0);
        result = 31 * result + (tabs != null ? tabs.hashCode() : 0);
        result = 31 * result + (resourceConfigs != null ? resourceConfigs.hashCode() : 0);
        result = 31 * result + (artifactConfigs != null ? artifactConfigs.hashCode() : 0);
        result = 31 * result + (runOnAllAgents ? 1 : 0);
        result = 31 * result + (runInstanceCount != null ? runInstanceCount.hashCode() : 0);
        result = 31 * result + (timeout != null ? timeout.hashCode() : 0);
        result = 31 * result + (elasticProfileId != null ? elasticProfileId.hashCode() : 0);
        return result;
    }

    public Tasks tasks() {
        return tasks.isEmpty() ? defaultTasks() : tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    private Tasks defaultTasks() {
        return new Tasks(new NullTask());
    }

    public ArtifactTypeConfigs artifactConfigs() {
        return artifactConfigs;
    }

    public void setArtifactConfigs(ArtifactTypeConfigs artifactConfigs) {
        this.artifactConfigs = artifactConfigs;
    }

    public Tabs getTabs() {
        return tabs;
    }

    public void setTabs(Tabs tabs) {
        this.tabs = tabs;
    }

    public void addTab(String tab, String path) {
        this.tabs.add(new Tab(tab.trim(), path));
    }

    public Tasks getTasks() {
        return tasks;
    }

    public Tasks getTasksForView() {
        return tasks.stream().map(task -> {
            if (task instanceof FetchTask) {
                return new FetchTaskAdapter((FetchTask) task);
            }
            if (task instanceof FetchPluggableArtifactTask) {
                return new FetchTaskAdapter((FetchPluggableArtifactTask) task);
            }
            return task;
        }).collect(Collectors.toCollection(Tasks::new));
    }

    public void setTasks(Tasks tasks) {
        this.tasks = tasks;
    }

    public void addResourceConfig(String resource) {
        resourceConfigs.add(new ResourceConfig(resource));
    }

    /* Used in rails view */
    public boolean isRunOnAllAgents() {
        return runOnAllAgents;
    }

    public void setRunOnAllAgents(boolean runOnAllAgents) {
        this.runOnAllAgents = runOnAllAgents;
    }

	public boolean isRunMultipleInstanceType() {
		return getRunInstanceCountValue() > 0;
	}

	public Integer getRunInstanceCountValue() {
		return runInstanceCount == null ? 0 : Integer.valueOf(runInstanceCount);
	}

	public String getRunInstanceCount() {
		return runInstanceCount;
	}

	public void setRunInstanceCount(Integer runInstanceCount) {
		setRunInstanceCount(Integer.toString(runInstanceCount));
	}

	public void setRunInstanceCount(String runInstanceCount) {
		this.runInstanceCount = runInstanceCount;
	}

	public boolean isInstanceOf(String jobInstanceName, boolean ignoreCase) {
        return jobTypeConfig().isInstanceOf(jobInstanceName, ignoreCase, CaseInsensitiveString.str(jobName));
    }

    public String translatedName(String jobInstanceName) {
        return jobTypeConfig().translatedJobName(jobInstanceName, CaseInsensitiveString.str(jobName));
    }

    private JobTypeConfig jobTypeConfig() {
        if (runOnAllAgents) {
            return new RunOnAllAgentsJobTypeConfig();
		} else if (isRunMultipleInstanceType()) {
			return new RunMultipleInstanceJobTypeConfig();
		} else {
            return new SingleJobTypeConfig();
        }
    }

    @Override
    public String toString() {
        return "JobConfig{" +
                "jobName='" + jobName + '\'' +
                ", resources=" + resourceConfigs +
                ", runOnAllAgents=" + runOnAllAgents +
                ", runInstanceCount=" + runInstanceCount +
                '}';
    }

    // only called from tests
    public void addVariable(String name, String value) {
        variables.add(name, value);
    }

    public void setVariables(EnvironmentVariablesConfig variables) {
        this.variables = variables;
    }

    public EnvironmentVariablesConfig getVariables() {
        return variables;
    }

    public EnvironmentVariablesConfig getPlainTextVariables() {
        return variables.getPlainTextVariables();
    }

    public EnvironmentVariablesConfig getSecureVariables() {
        return variables.getSecureVariables();
    }

    public boolean hasVariable(String variableName) {
        return variables.hasVariable(variableName);
    }

    public boolean hasTests() {
        for (ArtifactTypeConfig artifactTypeConfig : artifactConfigs) {
            if (artifactTypeConfig.getArtifactType().isTest()) {
                return true;
            }
        }
        return false;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors.isEmpty();
        ValidationContext contextForChildren = validationContext.withParent(this);
        isValid = tasks.validateTree(contextForChildren) && isValid;
        isValid = variables.validateTree(contextForChildren) && isValid;
        isValid = resourceConfigs.validateTree(contextForChildren) && isValid;
        isValid = tabs.validateTree(contextForChildren) && isValid;
        isValid = artifactConfigs.validateTree(contextForChildren) && isValid;
        return isValid;
    }

    public void encryptSecureProperties(CruiseConfig preprocessedConfig, PipelineConfig preprocessedPipelineConfig, JobConfig preprocessedJobConfig) {
        List<PluggableArtifactConfig> artifactConfigs = artifactConfigs().getPluggableArtifactConfigs();
        List<PluggableArtifactConfig> preprocessedArtifactConfigs = preprocessedJobConfig.artifactConfigs().getPluggableArtifactConfigs();
        artifactConfigs.forEach(artifactConfig -> {
            artifactConfig.encryptSecureProperties(preprocessedConfig, preprocessedArtifactConfigs.get(artifactConfigs.indexOf(artifactConfig)));
        });

        tasks.forEach(task -> {
            if (task instanceof FetchPluggableArtifactTask) {
                ((FetchPluggableArtifactTask) task).encryptSecureProperties(preprocessedConfig, preprocessedPipelineConfig, (FetchPluggableArtifactTask) preprocessedJobConfig.getTasks().get(tasks.indexOf(task)));
            }
        });
    }

    public void encryptSecureProperties(CruiseConfig preprocessedConfig, PipelineTemplateConfig pipelineTemplateConfig) {
        List<PluggableArtifactConfig> artifactConfigs = artifactConfigs().getPluggableArtifactConfigs();
        artifactConfigs.forEach(artifactConfig -> {
            artifactConfig.encryptSecureProperties(preprocessedConfig, artifactConfig);
        });

        tasks.forEach(task -> {
            if (task instanceof FetchPluggableArtifactTask) {
                FetchPluggableArtifactTask fetchPluggableArtifactTask = (FetchPluggableArtifactTask) task;
                fetchPluggableArtifactTask.encryptSecureProperties(preprocessedConfig, pipelineTemplateConfig);
            }
        });
    }

    @Override
    public void validate(ValidationContext validationContext) {

        if (isBlank(CaseInsensitiveString.str(jobName))) {
            errors.add(NAME, "Name is a required field");
        } else {
            if ((CaseInsensitiveString.str(jobName).length() > 255 || XmlUtils.doesNotMatchUsingXsdRegex(JOB_NAME_PATTERN_REGEX, CaseInsensitiveString.str(jobName)))) {
                String message = String.format("Invalid job name '%s'. This must be alphanumeric and may contain underscores and periods. The maximum allowed length is %d characters.", jobName,
                        NameTypeValidator.MAX_LENGTH);
                errors.add(NAME, message);
            }
            if (RunOnAllAgentsJobTypeConfig.hasMarker(CaseInsensitiveString.str(jobName))) {
                errors.add(NAME, String.format("A job cannot have '%s' in it's name: %s because it is a reserved keyword", RunOnAllAgentsJobTypeConfig.MARKER, jobName));
            }
            if (RunMultipleInstanceJobTypeConfig.hasMarker(CaseInsensitiveString.str(jobName))) {
                errors.add(NAME, String.format("A job cannot have '%s' in it's name: %s because it is a reserved keyword", RunMultipleInstanceJobTypeConfig.MARKER, jobName));
            }
        }
        if (runInstanceCount != null) {
            try {
                int runInstanceCountForValidation = Integer.parseInt(this.runInstanceCount);
                if (runInstanceCountForValidation < 0) {
                    errors().add(RUN_TYPE, "'Run Instance Count' cannot be a negative number as it represents number of instances Go needs to spawn during runtime.");
                }
            } catch (NumberFormatException e) {
                errors().add(RUN_TYPE, "'Run Instance Count' should be a valid positive integer as it represents number of instances Go needs to spawn during runtime.");
            }
        }
        if (isRunOnAllAgents() && isRunMultipleInstanceType()) {
            errors.add(RUN_TYPE, "Job cannot be 'run on all agents' type and 'run multiple instance' type together.");
        }
        if (timeout != null) {
            try {
                double timeoutForValidation = Double.parseDouble(this.timeout);
                if (timeoutForValidation < 0) {
                    errors().add(TIMEOUT, "Timeout cannot be a negative number as it represents number of minutes");
                }
            } catch (NumberFormatException e) {
                errors().add(TIMEOUT, "Timeout should be a valid number as it represents number of minutes");
            }
        }
        if (!resourceConfigs.isEmpty() && !isBlank(elasticProfileId)) {
            errors().add(RESOURCES, "Job cannot have both `resource` and `elasticProfileId`");
            errors().add(ELASTIC_PROFILE_ID, "Job cannot have both `resource` and `elasticProfileId`");
        }
        if (!isBlank(elasticProfileId)) {
            if (!validationContext.isWithinTemplates() && !validationContext.isValidProfileId(elasticProfileId)) {
                errors().add(ELASTIC_PROFILE_ID, String.format("No profile defined corresponding to profile_id '%s'", elasticProfileId));
            }
        }
        if (elasticProfileId != null && isBlank(elasticProfileId)){
            errors().add(ELASTIC_PROFILE_ID, "Must not be a blank string");
        }
        for (ResourceConfig resourceConfig : resourceConfigs) {
            if (StringUtils.isEmpty(resourceConfig.getName())) {
                CaseInsensitiveString pipelineName = validationContext.getPipeline().name();
                CaseInsensitiveString stageName = validationContext.getStage().name();
                String message = String.format("Empty resource name in job \"%s\" of stage \"%s\" of pipeline \"%s\". If a template is used, please ensure that the resource parameters are defined for this pipeline.", jobName, stageName, pipelineName);
                errors.add(RESOURCES, message);
            }
        }
        if (isRunOnAllAgents() && !isBlank(elasticProfileId)) {
            errors.add(RUN_TYPE, "Job cannot be set to 'run on all agents' when assigned to an elastic agent");
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        setConfigAttributes(attributes, null);
    }

    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        Map attributesMap = (Map) attributes;
		if (attributesMap.containsKey(NAME)) {
            String nameString = (String) attributesMap.get(NAME);
            jobName = nameString == null ? null : new CaseInsensitiveString(nameString);
        }
        if (attributesMap.containsKey("elasticProfileId")) {
            String elasticProfileId = (String) attributesMap.get("elasticProfileId");
            setElasticProfileId(StringUtils.isBlank(elasticProfileId) ? null : elasticProfileId);
        }

        if (attributesMap.containsKey(TASKS)) {
            tasks.setConfigAttributes(attributesMap.get(TASKS), taskFactory);
        }
        if (attributesMap.containsKey(ENVIRONMENT_VARIABLES)) {
            variables.setConfigAttributes(attributesMap.get(ENVIRONMENT_VARIABLES));
        }
        if (attributesMap.containsKey(TABS)) {
            tabs.setConfigAttributes(attributesMap.get(TABS));
        }
        if (attributesMap.containsKey(RESOURCES)) {
            resourceConfigs.importFromCsv((String) attributesMap.get(RESOURCES));
        }
        if (attributesMap.containsKey(ARTIFACT_CONFIGS)) {
            artifactConfigs.setConfigAttributes(attributesMap.get(ARTIFACT_CONFIGS));
        }
        setTimeoutAttribute(attributesMap);
        setJobRunTypeAttribute(attributesMap);
    }

    private void setTimeoutAttribute(Map attributesMap) {
        if (attributesMap.containsKey("timeoutType")) {
            String timeoutType = (String) attributesMap.get("timeoutType");
            if (DEFAULT_TIMEOUT.equals(timeoutType)) {
                this.timeout = null;
            }
            if (NEVER_TIMEOUT.equals(timeoutType)) {
                this.timeout = "0";
            }
            if (OVERRIDE_TIMEOUT.equals(timeoutType)) {
                String timeout = (String) attributesMap.get(TIMEOUT);
                if (isBlank(timeout)) {
                    this.timeout = null;
                } else {
                    this.timeout = timeout;
                }
            }
        }
    }

	private void setJobRunTypeAttribute(Map attributesMap) {
		if (attributesMap.containsKey(RUN_TYPE)) {
			this.runOnAllAgents = false;
			this.runInstanceCount = null;

			String jobRunType = (String) attributesMap.get(RUN_TYPE);
			if (RUN_ON_ALL_AGENTS.equals(jobRunType)) {
				this.runOnAllAgents = true;
			} else if (RUN_MULTIPLE_INSTANCE.equals(jobRunType)) {
				String runInstanceCount = (String) attributesMap.get(RUN_INSTANCE_COUNT);
				if (isBlank(runInstanceCount)) {
					this.runInstanceCount = null;
				} else {
					this.runInstanceCount = runInstanceCount;
				}
			}
		}
	}

    public void validateNameUniqueness(Map<String, JobConfig> visitedConfigs) {
        if (isBlank(CaseInsensitiveString.str(name()))) return;

        String currentJob = name().toLower();
        if (visitedConfigs.containsKey(CaseInsensitiveString.str(name())) || visitedConfigs.containsKey(currentJob)) {
            JobConfig conflictingConfig = visitedConfigs.get(currentJob);
            conflictingConfig.addUniquenessViolationMessage();
            this.addUniquenessViolationMessage();
        } else {
            visitedConfigs.put(currentJob, this);
        }
    }

    private void addUniquenessViolationMessage() {
        this.addError(NAME, String.format("You have defined multiple jobs called '%s'. Job names are case-insensitive and must be unique.", name()));
    }

    @Override
    public void addError(String key, String message) {
        errors.add(key, message);
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getTimeoutType() {
        return timeout == null ? DEFAULT_TIMEOUT : timeout.equals("0") ? NEVER_TIMEOUT : OVERRIDE_TIMEOUT;
    }

	public String getRunType() {
		if (isRunOnAllAgents())
			return RUN_ON_ALL_AGENTS;
		if (isRunMultipleInstanceType())
			return RUN_MULTIPLE_INSTANCE;
		return RUN_SINGLE_INSTANCE;
	}

    public void injectTasksForTest(Tasks tasks) {
        this.tasks = tasks;
    }

    public String getElasticProfileId() {
        return elasticProfileId;
    }

    public boolean usesElasticAgent() {
        return getElasticProfileId() != null;
    }

    public void setElasticProfileId(String elasticProfileId) {
        this.elasticProfileId = elasticProfileId;
    }
}
