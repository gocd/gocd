package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PiplineConfigVisitor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by tomzo on 6/11/15.
 */
public interface PipelineConfigs extends List<PipelineConfig>, Cloneable, Serializable, Validatable, ParamsAttributeAware {

    public static final String DEFAULT_GROUP = "defaultGroup";
    public static final String GROUP = "group";
    public static final String AUTHORIZATION = "authorization";

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
}
