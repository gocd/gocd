/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.service.TaskFactory;
import com.thoughtworks.go.util.GoConstants;

import java.util.List;
import java.util.Map;

/**
 * @understands the configuration for a stage
 */
@ConfigTag("stage")
public class StageConfig implements Validatable, ParamsAttributeAware, EnvironmentVariableScope {
    @SkipParameterResolution
    @ConfigAttribute(value = "name", optional = false)
    private CaseInsensitiveString name;

    @ConfigAttribute(value = "fetchMaterials") private boolean fetchMaterials = DEFAULT_FETCH_MATERIALS;
    @ConfigAttribute(value = "artifactCleanupProhibited") private boolean artifactCleanupProhibited = false;
    @ConfigAttribute(value = "cleanWorkingDir") private boolean cleanWorkingDir = DEFAULT_CLEAN_WORKING_DIR;
    @ConfigSubtag(optional = true) private Approval approval = Approval.automaticApproval();
    @ConfigSubtag private EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
    @ConfigSubtag(optional = false) private JobConfigs jobConfigs;

    private ConfigErrors errors = new ConfigErrors();

    public static final boolean DEFAULT_FETCH_MATERIALS = true;
    public static final boolean DEFAULT_CLEAN_WORKING_DIR = false;

    public static final String NAME = "name";
    public static final String APPROVAL = "approval";
    public static final String JOBS = "jobs";
    public static final String FETCH_MATERIALS = "fetchMaterials";
    public static final String CLEAN_WORKING_DIR = "cleanWorkingDir";
    public static final String ENVIRONMENT_VARIABLES = "variables";
    public static final String OPERATE_USERS = "operateUsers";
    public static final String OPERATE_ROLES = "operateRoles";
    public static final String SECURITY_MODE = "securityMode";
    public static final String DEFAULT_NAME = "defaultStage";
    public static final String ARTIFACT_CLEANUP_PROHIBITED = "artifactCleanupProhibited";

    public StageConfig() {
    }

    public StageConfig(final CaseInsensitiveString name, JobConfigs jobConfigs) {
        this();
        this.name = name;
        this.jobConfigs = jobConfigs;
    }

    public StageConfig(final CaseInsensitiveString name, JobConfigs jobConfigs, Approval approval) {
        this(name, DEFAULT_FETCH_MATERIALS, DEFAULT_CLEAN_WORKING_DIR, approval, false, jobConfigs);
    }

    public StageConfig(final CaseInsensitiveString name, boolean fetchMaterials, boolean cleanWorkingDir, Approval approval, boolean artifactCleanupProhibited, JobConfigs jobConfigs) {
        this(name, jobConfigs);
        this.fetchMaterials = fetchMaterials;
        this.cleanWorkingDir = cleanWorkingDir;
        this.approval = approval;
        this.artifactCleanupProhibited = artifactCleanupProhibited;
    }

    public CaseInsensitiveString name() {
        return name;
    }

    public void setName(CaseInsensitiveString name) {
        this.name = name;
    }


    /* Used in view */
    public boolean isFetchMaterials() {
        return fetchMaterials;
    }

    /* Used in view */
    public boolean isCleanWorkingDir() {
        return cleanWorkingDir;
    }

    public JobConfig jobConfigByInstanceName(String jobInstanceName, boolean ignoreCase) {
        for (JobConfig jobConfig : jobConfigs) {
            if (jobConfig.isInstanceOf(jobInstanceName, ignoreCase)) {
                return jobConfig;
            }
        }
        return null;
    }

    public JobConfig jobConfigByConfigName(final CaseInsensitiveString jobName) {
        for (JobConfig jobConfig : jobConfigs) {
            if (jobConfig.name().equals(jobName)) {
                return jobConfig;
            }
        }
        return null;
    }

    public JobConfig jobConfigByConfigName(String jobName) {
        return jobConfigByConfigName(new CaseInsensitiveString(jobName));
    }

    // TODO - #2491 - rename jobConfig to job

    public JobConfigs allBuildPlans() {
        return jobConfigs;
    }

    public String approvalType() {
        return requiresApproval() ? GoConstants.APPROVAL_MANUAL : GoConstants.APPROVAL_SUCCESS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageConfig that = (StageConfig) o;

        if (fetchMaterials != that.fetchMaterials) {
            return false;
        }
        if (artifactCleanupProhibited != that.artifactCleanupProhibited) {
            return false;
        }
        if (cleanWorkingDir != that.cleanWorkingDir) {
            return false;
        }
        if (approval != null ? !approval.equals(that.approval) : that.approval != null) {
            return false;
        }
        if (jobConfigs != null ? !jobConfigs.equals(that.jobConfigs) : that.jobConfigs != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (variables != null ? !variables.equals(that.variables) : that.variables != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (fetchMaterials ? 1 : 0);
        result = 31 * result + (artifactCleanupProhibited ? 1 : 0);
        result = 31 * result + (cleanWorkingDir ? 1 : 0);
        result = 31 * result + (approval != null ? approval.hashCode() : 0);
        result = 31 * result + (variables != null ? variables.hashCode() : 0);
        result = 31 * result + (jobConfigs != null ? jobConfigs.hashCode() : 0);
        return result;
    }

    public boolean requiresApproval() {
        return approval.isManual();
    }

    public void updateApproval(Approval approval) {
        this.approval = approval;
    }

    public boolean supportAutoApproval() {
        return !requiresApproval();
    }

    public Approval getApproval() {
        return approval;
    }

    public void setApproval(Approval approval) {
        this.approval = approval;
    }

    public boolean hasOperatePermissionDefined() {
        return this.approval.isAuthorizationDefined();
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

    public boolean hasVariableInScope(String variableName) {
        if (variables.hasVariable(variableName)) {
            return true;
        }
        for (JobConfig jobConfig : jobConfigs) {
            if (jobConfig.hasVariable(variableName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTests() {
        for (JobConfig job : jobConfigs) {
            if (job.hasTests()) {
                return true;
            }
        }
        return false;
    }

    public void setFetchMaterials(boolean fetchMaterials) {
        this.fetchMaterials = fetchMaterials;
    }

    public void setCleanWorkingDir(boolean cleanWorkingDir) {
        this.cleanWorkingDir = cleanWorkingDir;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors.isEmpty();
        ValidationContext contextForChildren = validationContext.withParent(this);
        isValid = jobConfigs.validateTree(contextForChildren) && isValid;
        isValid = approval.validateTree(contextForChildren) && isValid;
        isValid = variables.validateTree(contextForChildren) && isValid;
        return isValid;
    }

    public void validate(ValidationContext validationContext) {
        isNameValid();
    }

    private boolean isNameValid() {
        if (!new NameTypeValidator().isNameValid(name)) {
            this.errors.add(NAME, NameTypeValidator.errorMessage("stage", name));
            return false;
        }
        return true;
    }

    public void validateNameUniqueness(Map<String, StageConfig> stageNameMap) {
        if (isNameValid()) {
            String currentName = name.toLower();
            StageConfig stageWithSameName = stageNameMap.get(currentName);
            if (stageWithSameName == null) {
                stageNameMap.put(currentName, this);
            } else {
                stageWithSameName.nameConflictError();
                this.nameConflictError();
            }
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public void setConfigAttributes(Object attributes) {
        setConfigAttributes(attributes, null);
    }

    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(NAME)) {
            name = new CaseInsensitiveString((String) attributeMap.get(NAME));
        }
        if (attributeMap.containsKey(ARTIFACT_CLEANUP_PROHIBITED)) {
            artifactCleanupProhibited = attributeMap.get(ARTIFACT_CLEANUP_PROHIBITED).equals("1") ? true : false;
        }
        if (attributeMap.containsKey(FETCH_MATERIALS)) {
            fetchMaterials = attributeMap.get(FETCH_MATERIALS).equals("1") ? true : false;
        }
        if (attributeMap.containsKey(CLEAN_WORKING_DIR)) {
            cleanWorkingDir = attributeMap.get(CLEAN_WORKING_DIR).equals("1") ? true : false;
        }
        if (attributeMap.containsKey(APPROVAL)) {
            approval.setConfigAttributes(attributeMap.get(APPROVAL));
        }
        if (attributeMap.containsKey(JOBS)) {
            if (jobConfigs == null) {
                jobConfigs = new JobConfigs();
            }
            jobConfigs.setConfigAttributes(attributeMap.get(JOBS), taskFactory);
        }
        if (attributeMap.containsKey(ENVIRONMENT_VARIABLES)) {
            variables.setConfigAttributes(attributeMap.get(ENVIRONMENT_VARIABLES));
        }
        if (attributeMap.containsKey(SECURITY_MODE)) {
            String mode = (String) attributeMap.get(SECURITY_MODE);
            if ("define".equals(mode)) {
                approval.setOperatePermissions((List<Map<String, String>>) attributeMap.get(OPERATE_USERS), (List<Map<String, String>>) attributeMap.get(OPERATE_ROLES));
            }
            if ("inherit".equals(mode)) {
                approval.removeOperatePermissions();
            }
        }
    }

    private void nameConflictError() {
        errors.add(NAME, String.format("You have defined multiple stages called '%s'. Stage names are case-insensitive and must be unique.", name));
    }

    public JobConfigs getJobs() {
        return jobConfigs;
    }

    public void setJobs(JobConfigs jobConfigs) {
        this.jobConfigs = jobConfigs;
    }

    public List<AdminUser> getOperateUsers() {
        return getApproval().getAuthConfig().getUsers();
    }

    public List<AdminRole> getOperateRoles() {
        return getApproval().getAuthConfig().getRoles();
    }

    public boolean isArtifactCleanupProhibited() {
        return artifactCleanupProhibited;
    }

    public void setArtifactCleanupProhibited(boolean artifactCleanupProhibited) {
        this.artifactCleanupProhibited = artifactCleanupProhibited;
    }

    public void cleanupAllUsagesOfRole(Role roleToDelete) {
        this.getApproval().getAuthConfig().remove(new AdminRole(roleToDelete));
    }

    public boolean canBeOperatedBy(Role role) {
        return getOperateRoles().contains(new AdminRole(role));
    }
}
