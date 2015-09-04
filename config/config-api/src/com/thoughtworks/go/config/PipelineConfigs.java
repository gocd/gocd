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

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigOriginTraceable;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PiplineConfigVisitor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PipelineConfigs extends Iterable<PipelineConfig>, Cloneable, Validatable,
        ParamsAttributeAware, ConfigOriginTraceable {

    public static final String DEFAULT_GROUP = "defaultGroup";
    public static final String GROUP = "group";
    public static final String AUTHORIZATION = "authorization";
    public static final String NO_REMOTE_AUTHORIZATION = "no_remote_authorization";

    int size();

    boolean contains(PipelineConfig pipelineConfig);

    boolean isEmpty();

    ConfigOrigin getOrigin();

    PipelineConfig findBy(CaseInsensitiveString pipelineName);

    boolean add(PipelineConfig pipelineConfig);

    boolean addWithoutValidation(PipelineConfig pipelineConfig);

    PipelineConfig set(int index, PipelineConfig pipelineConfig);

    void addToTop(PipelineConfig pipelineConfig);

    void add(int index, PipelineConfig pipelineConfig);

    String getGroup();

    void setGroup(String group);

    boolean isNamed(String groupName);

    void update(String groupName, PipelineConfig pipeline, String pipelineName);

    boolean save(PipelineConfig pipeline, String groupName);

    void add(List<String> allGroup);

    boolean exist(int pipelineIndex);

    boolean hasPipeline(CaseInsensitiveString pipelineName);

    Authorization getAuthorization();

    void accept(PiplineConfigVisitor visitor);

    void setAuthorization(Authorization authorization);

    boolean hasViewPermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher);

    boolean hasViewPermissionDefined();

    boolean hasOperationPermissionDefined();

    boolean hasOperatePermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher);

    boolean hasAuthorizationDefined();

    boolean hasTemplate();

    PipelineConfigs getCopyForEditing();

    boolean isUserAnAdmin(CaseInsensitiveString userName, List<Role> memberRoles);

    void validate(ValidationContext validationContext);

    void validateNameUniqueness(Map<String, PipelineConfigs> groupNameMap);

    ConfigErrors errors();

    List<PipelineConfig> getPipelines();

    void addError(String fieldName, String message);

    List<AdminUser> getOperateUsers();

    List<AdminRole> getOperateRoles();

    List<String> getOperateRoleNames();

    List<String> getOperateUserNames();

    void setConfigAttributes(Object attributes);

    void cleanupAllUsagesOfRole(Role roleToDelete);

    int indexOf(PipelineConfig pipelineConfig);

    PipelineConfig get(int i);

    void remove(PipelineConfig pipelineConfig);

    PipelineConfig remove(int i);

    void validateGroupNameAndAddErrorsTo(ConfigErrors errors);
}
