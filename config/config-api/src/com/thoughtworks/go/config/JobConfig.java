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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.XmlUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

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
    private Resources resources = new Resources();
    @ConfigSubtag
    private ArtifactPlans artifactPlans = new ArtifactPlans();
    @ConfigSubtag
    private ArtifactPropertiesGenerators artifactPropertiesGenerators = new ArtifactPropertiesGenerators();

    @ConfigAttribute(value = "runOnAllAgents", optional = true) private boolean runOnAllAgents = false;
    @ConfigAttribute(value = "runInstanceCount", optional = true, allowNull = true) private String runInstanceCount;
    @ConfigAttribute(value = "timeout", optional = true, allowNull = true) private String timeout;

    private ConfigErrors errors = new ConfigErrors();
    public static final String NAME = "name";
    public static final String TASKS = "tasks";
    public static final String RESOURCES = "resources";
    public static final String TABS = "tabs";
    public static final String ENVIRONMENT_VARIABLES = "variables";
    public static final String ARTIFACT_PLANS = "artifactPlans";
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
	private static final String JOB_NAME_PATTERN = "[a-zA-Z0-9_\\-.]+";
    private static final Pattern JOB_NAME_PATTERN_REGEX = Pattern.compile(String.format("^(%s)$", JOB_NAME_PATTERN));

    public JobConfig() {
    }

    public JobConfig(CaseInsensitiveString jobName) {
        this();
        this.jobName = jobName;
    }

    public JobConfig(final CaseInsensitiveString jobName, Resources resources, ArtifactPlans artifactPlans) {
        this(jobName, resources, artifactPlans, new Tasks());
    }

    public JobConfig(final CaseInsensitiveString jobName, Resources resources, ArtifactPlans artifactPlans, Tasks tasks) {
        this(jobName);
        this.resources = resources;
        this.artifactPlans = artifactPlans;
        this.tasks = tasks;
    }

    public JobConfig(final CaseInsensitiveString jobName, ArtifactPropertiesGenerators artifactPropertiesGenerators) {
        this(jobName);
        this.artifactPropertiesGenerators = artifactPropertiesGenerators;
    }

    public JobConfig(final CaseInsensitiveString jobName, Resources resources, ArtifactPlans artifactPlans, ArtifactPropertiesGenerators generators) {
        this(jobName, resources, artifactPlans);
        this.artifactPropertiesGenerators = generators;
    }

    public JobConfig(String planName) {
        this(new CaseInsensitiveString(planName), new Resources(), new ArtifactPlans());
    }

    public CaseInsensitiveString name() {
        return jobName;
    }

    public Resources resources() {
        return resources;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobConfig jobConfig = (JobConfig) o;

        return !(jobName != null ? !jobName.equals(jobConfig.jobName) : jobConfig.jobName != null);
    }

    @Override
    public int hashCode() {
        return jobName != null ? jobName.hashCode() : 0;
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

    public ArtifactPlans artifactPlans() {
        return artifactPlans;
    }

    public Tabs getTabs() {
        return tabs;
    }

    public void addTab(String tab, String path) {
        this.tabs.add(new Tab(tab.trim(), path));
    }

    public ArtifactPropertiesGenerators getProperties() {
        return artifactPropertiesGenerators;
    }

    public Tasks getTasks() {
        return tasks;
    }

    public void addResource(String resource) {
        resources.add(new Resource(resource));
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
		this.runInstanceCount = Integer.toString(runInstanceCount);
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
                ", resources=" + resources +
                ", runOnAllAgents=" + runOnAllAgents +
                ", runInstanceCount=" + runInstanceCount +
                '}';
    }

    // only called from tests
    public void addVariable(String name, String value) {
        variables.add(name, value);
    }

    // only called from tests
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
        for (ArtifactPlan artifactPlan : artifactPlans) {
            if (artifactPlan.getArtifactType().isTest()) {
                return true;
            }
        }
        return false;
    }

    public void validate(ValidationContext validationContext) {
        if (CaseInsensitiveString.str(name()).length() > 255 || XmlUtils.doesNotMatchUsingXsdRegex(JOB_NAME_PATTERN_REGEX, CaseInsensitiveString.str(name()))) {
            String message = String.format("Invalid job name '%s'. This must be alphanumeric and can contain underscores and periods. The maximum allowed length is %d characters.", name(),
                    NameTypeValidator.MAX_LENGTH);
            errors.add(NAME, message);
        }
        if (RunOnAllAgentsJobTypeConfig.hasMarker(CaseInsensitiveString.str(jobName))) {
            errors.add(NAME, String.format("A job cannot have '%s' in it's name: %s", RunOnAllAgentsJobTypeConfig.MARKER, jobName));
        }
        if (RunMultipleInstanceJobTypeConfig.hasMarker(CaseInsensitiveString.str(jobName))) {
            errors.add(NAME, String.format("A job cannot have '%s' in it's name: %s", RunMultipleInstanceJobTypeConfig.MARKER, jobName));
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
        for (Resource resource : resources) {
            if (StringUtils.isEmpty(resource.getName())) {
                CaseInsensitiveString pipelineName = validationContext.getPipeline().name();
                CaseInsensitiveString stageName = validationContext.getStage().name();
                CaseInsensitiveString jobName = name();
                String message = String.format("Empty resource name in job \"%s\" of stage \"%s\" of pipeline \"%s\". If a template is used, please ensure that the resource parameters are defined for this pipeline.", jobName, stageName, pipelineName);
                errors.add(RESOURCES, message);
            }
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void setConfigAttributes(Object attributes) {
        setConfigAttributes(attributes, null);
    }

    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        Map attributesMap = (Map) attributes;
		if (attributesMap.containsKey(NAME)) {
            String nameString = (String) attributesMap.get(NAME);
            jobName = nameString == null ? null : new CaseInsensitiveString(nameString);
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
            resources.importFromCsv((String) attributesMap.get(RESOURCES));
        }
        if (attributesMap.containsKey(ARTIFACT_PLANS)) {
            artifactPlans.setConfigAttributes(attributesMap.get(ARTIFACT_PLANS));
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
                if (StringUtil.isBlank(timeout)) {
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
				if (StringUtil.isBlank(runInstanceCount)) {
					this.runInstanceCount = null;
				} else {
					this.runInstanceCount = runInstanceCount;
				}
			}
		}
	}

    public void validateNameUniqueness(Map<String, JobConfig> visitedConfigs) {
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
}
