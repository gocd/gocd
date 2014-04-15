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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import org.apache.commons.lang.StringUtils;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@ConfigTag("pipelines")
@ConfigCollection(value = PipelineConfig.class, asFieldName = "pipelines")
public class PipelineConfigs extends BaseCollection<PipelineConfig> implements Validatable, ParamsAttributeAware {
    public static final String DEFAULT_GROUP = "defaultGroup";

    public static final String GROUP = "group";
    @ConfigAttribute(value = "group", optional = true) @SkipParameterResolution
    private String group;

    public static final String AUTHORIZATION = "authorization";
    @ConfigSubtag @SkipParameterResolution
    private Authorization authorization = new Authorization();

    private final ConfigErrors configErrors = new ConfigErrors();


    public PipelineConfigs() {
    }

    public PipelineConfigs(PipelineConfig... pipelineConfigs) {
        this(new Authorization(), pipelineConfigs);
    }

    public PipelineConfigs(Authorization authorization, PipelineConfig... pipelineConfigs) {
        super(pipelineConfigs);
        this.authorization = authorization;
    }

    public PipelineConfigs(String group, Authorization authorization, PipelineConfig... pipelineConfigs) {
        super(pipelineConfigs);
        this.group = group;
        this.authorization = authorization;
    }

    public PipelineConfig findBy(final CaseInsensitiveString pipelineName) {
        for (int i=0; i< this.size(); i++) {
            PipelineConfig pipelineConfig = this.get(i);
            if (pipelineConfig.name().equals(pipelineName)) {
                return pipelineConfig;
            }
        }
        return null;
    }

    public boolean add(PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig);
        return addWithoutValidation(pipelineConfig);
    }

    public boolean addWithoutValidation(PipelineConfig pipelineConfig) {
        return super.add(pipelineConfig);
    }

    public PipelineConfig set(int index, PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig, index);
        return super.set(index, pipelineConfig);
    }

    public void addToTop(PipelineConfig pipelineConfig) {
        this.add(0, pipelineConfig);
    }

    public void add(int index, PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig);
        super.add(index, pipelineConfig);
    }

    private void verifyUniqueName(PipelineConfig pipelineConfig) {
        if (alreadyContains(pipelineConfig)) {
            throw bomb("You have defined multiple pipelines called '" + pipelineConfig.name() + "'. Pipeline names must be unique.");
        }
    }

    private void verifyUniqueName(PipelineConfig pipelineConfig, int index) {
        if (pipelineConfig.name().equals(super.get(index).name())) {
            return;
        }
        verifyUniqueName(pipelineConfig);
    }


    private boolean alreadyContains(PipelineConfig pipelineConfig) {
        return findBy(pipelineConfig.name()) != null;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = sanitizedGroupName(group);
    }

    public static String sanitizedGroupName(String group) {
        return StringUtils.isBlank(group) ? DEFAULT_GROUP : group;
    }

    public boolean isNamed(String groupName) {
        return group.equals(groupName);
    }


    public void update(String groupName, PipelineConfig pipeline, String pipelineName) {
        if (!isSameGroup(groupName)) {
            return;
        }
        this.set(getIndex(pipelineName), pipeline);
    }

    private int getIndex(String pipelineName) {
        return this.indexOf(this.findBy(new CaseInsensitiveString(pipelineName)));
    }

    public boolean save(PipelineConfig pipeline, String groupName) {
        if (isSameGroup(groupName)) {
            this.addToTop(pipeline);
            return true;
        } else {
            return false;
        }
    }

    private boolean isSameGroup(String groupName) {
        return StringUtils.equals(groupName, this.getGroup());
    }

    public void add(List<String> allGroup) {
        allGroup.add(group);
    }

    public boolean exist(int pipelineIndex) {
        return pipelineIndex < this.size();
    }

    public boolean hasPipeline(CaseInsensitiveString pipelineName) {
        for (PipelineConfig pipelineConfig : this) {
            if (pipelineConfig.name().equals(pipelineName)) {
                return true;
            }
        }
        return false;
    }

    public Authorization getAuthorization() {
        return this.authorization;
    }

    public void accept(PiplineConfigVisitor visitor) {
        for (PipelineConfig pipelineConfig : this) {
            visitor.visit(pipelineConfig);
        }
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    public boolean hasViewPermission(final CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return !hasAuthorizationDefined() || authorization.hasViewPermission(username, userRoleMatcher);
    }

    public boolean hasViewPermissionDefined() {
        return authorization.hasViewPermissionDefined();
    }

    public boolean hasOperationPermissionDefined() {
        return authorization.hasOperationPermissionDefined();
    }

    public boolean hasOperatePermission(final CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return !hasAuthorizationDefined() || authorization.hasOperatePermission(username, userRoleMatcher);
    }

    public boolean hasAuthorizationDefined() {
        return !authorization.equals(new Authorization());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PipelineConfigs pipelines = (PipelineConfigs) o;

        if (authorization != null ? !authorization.equals(pipelines.authorization) : pipelines.authorization != null) {
            return false;
        }
        if (group != null ? !group.equals(pipelines.group) : pipelines.group != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (authorization != null ? authorization.hashCode() : 0);
        return result;
    }

    public boolean hasTemplate() {
        for (PipelineConfig pipelineConfig : this) {
            if (pipelineConfig.hasTemplate()) { return true; }
        }
        return false;
    }

    public PipelineConfigs getCopyForEditing() {
        PipelineConfigs clone = (PipelineConfigs) clone();
        clone.clear();
        for (PipelineConfig pipeline : this) {
            clone.add(pipeline.getCopyForEditing());
        }
        return clone;
    }

    public boolean isUserAnAdmin(final CaseInsensitiveString userName, List<Role> memberRoles) {
        return authorization.hasAdminsDefined() && authorization.isUserAnAdmin(userName, memberRoles);
    }

    public void validate(ValidationContext validationContext) {
        if (StringUtils.isBlank(group) || !new NameTypeValidator().isNameValid(group)) {
            this.configErrors.add(GROUP, NameTypeValidator.errorMessage("group", group));
        }

        verifyPipelineNameUniqueness();
    }

    private void verifyPipelineNameUniqueness() {
        HashMap<String, PipelineConfig> hashMap = new HashMap<String, PipelineConfig>();
        for(PipelineConfig pipelineConfig : this){
            pipelineConfig.validateNameUniqueness(hashMap);
        }
    }

    public void validateNameUniqueness(Map<String, PipelineConfigs> groupNameMap) {
        String currentName = group.toLowerCase();
        PipelineConfigs groupWithSameName = groupNameMap.get(currentName);
        if (groupWithSameName == null) {
            groupNameMap.put(currentName, this);
        } else {
            groupWithSameName.nameConflictError();
            this.nameConflictError();
        }
    }

    private void nameConflictError() {
        this.configErrors.add(GROUP, String.format("Group with name '%s' already exists", group));
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public List<AdminUser> getOperateUsers() {
        return authorization.getOperationConfig().getUsers();
    }

    public List<AdminRole> getOperateRoles() {
        return authorization.getOperationConfig().getRoles();
    }

    public List<String> getOperateRoleNames() {
        List<String> roles = new ArrayList<String>();
        for (AdminRole role : getOperateRoles()) {
            roles.add(CaseInsensitiveString.str(role.getName()));
        }
        return roles;
    }

    public List<String> getOperateUserNames() {
        List<String> users = new ArrayList<String>();
        for (AdminUser user : getOperateUsers()) {
            users.add(CaseInsensitiveString.str(user.getName()));
        }
        return users;
    }

    public void setConfigAttributes(Object attributes) {
        Map attributeMap = (Map) attributes;
        if (attributeMap == null) {
            return;
        }
        if (attributeMap.containsKey(GROUP)) {
            this.group = (String) attributeMap.get(GROUP);
        }
        if (attributeMap.containsKey(AUTHORIZATION)) {
            this.authorization = new Authorization();
            this.authorization.setConfigAttributes(attributeMap.get(AUTHORIZATION));
        }
        else {
            this.authorization = new Authorization();
        }
    }


    public void cleanupAllUsagesOfRole(Role roleToDelete) {
        getAuthorization().removeAllUsagesOfRole(roleToDelete);
        for (PipelineConfig pipelineConfig : this){
            pipelineConfig.cleanupAllUsagesOfRole(roleToDelete);
        }
    }
}
