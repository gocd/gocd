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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.util.StringUtil;

@ConfigTag(value = "approval")
//TODO: ChrisS: Make this a proper enumeration
public class Approval implements Validatable, ParamsAttributeAware {
    @ConfigSubtag(optional = true)
    private AuthConfig authConfig = new AuthConfig();

    @ConfigAttribute(value = "type", optional = false, alwaysWrite = true)
    @SkipParameterResolution
    private String type = SUCCESS;

    public static final String MANUAL = "manual";
    public static final String SUCCESS = "success";
    public static final String TYPE = "type";

    private ConfigErrors errors = new ConfigErrors();

    public static final Approval automaticApproval() {
        return new Approval(SUCCESS);
    }

    public static final Approval manualApproval() {
        return new Approval(MANUAL);
    }

    public Approval() {
        this(MANUAL);
    }

    private Approval(String type) {
        this.type = type;
    }

    //for test
    public Approval(AuthConfig authConfig) {
        this(MANUAL);
        this.authConfig = authConfig;
    }

    public boolean isManual() {
        return type.equals(MANUAL);
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }
    public void setAuthConfig(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    public boolean isAuthorizationDefined() {
        return !this.authConfig.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Approval approval = (Approval) o;
        if (!authConfig.equals(approval.authConfig)) {
            return false;
        }
        if (!type.equals(approval.type)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = authConfig.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type= type;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors.isEmpty();
        for (Admin admin : authConfig) {
            admin.validate(validationContext);
            authConfig.errors().addAll(admin.errors());
            isValid = admin.errors().isEmpty() && isValid;
        }
        return isValid;
    }

    public void validate(ValidationContext validationContext) {
        if (!isValidTypeValue()) {
            errors.add(TYPE, String.format("You have defined approval type as '%s'. Approval can only be of the type '%s' or '%s'.", type, MANUAL, SUCCESS));
        }
        if (validationContext.isWithinPipelines()) {
            PipelineConfigs group = validationContext.getPipelineGroup();
            if (!group.hasOperationPermissionDefined()) {
                return;
            }

            AdminsConfig groupOperators = group.getAuthorization().getOperationConfig();
            SecurityConfig serverSecurityConfig = validationContext.getServerSecurityConfig();
            RolesConfig roles = serverSecurityConfig.getRoles();

            for (Admin approver : authConfig) {
                boolean approverIsASuperAdmin = serverSecurityConfig.isAdmin(approver);
                boolean approverIsAGroupAdmin = group.isUserAnAdmin(approver.getName(), roles.memberRoles(approver));

                boolean approverIsNotAnAdmin = !(approverIsASuperAdmin || approverIsAGroupAdmin);
                boolean approverIsNotAGroupOperator = !groupOperators.has(approver, roles.memberRoles(approver));

                if (approverIsNotAnAdmin && approverIsNotAGroupOperator) {
                    approver.addError(String.format("%s \"%s\" who is not authorized to operate pipeline group can not be authorized to approve stage", approver.describe(), approver, group.getGroup()));
                }
            }
        }
    }

    private boolean isValidTypeValue() {
        return type.equals(MANUAL) || type.equals(SUCCESS);
    }

    public void setConfigAttributes(Object attributes) {
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(TYPE)) {
            type = (String) attributeMap.get(TYPE);
        }
    }

    public ConfigErrors errors() {
        return errors;
    }

    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public void addAdmin(Admin... admins) {
        for (Admin admin : admins) {
            if (!authConfig.contains(admin)) {
                authConfig.add(admin);
            }
        }
    }

    public String getDisplayName() {
        return type.equals(MANUAL) ? "Manual" : "On Success";
    }

    public void setOperatePermissions(List<Map<String, String>> usersMap, List<Map<String, String>> rolesMap) {
        authConfig.clear();
        Iterable<String> i;

        if (usersMap != null) {
            addAdmin(extractAdminUsers(usersMap));
        }
        if (rolesMap != null) {
            addAdmin(extractAdminRole(rolesMap));
        }
    }

    private Admin[] extractAdminUsers(List<Map<String, String>> map) {
        List<Admin> result = new ArrayList<>(map.size());
        for (Map<String, String> usernameMap : map) {
            String value = usernameMap.get("name").trim();
            if (!StringUtil.isBlank(value)) {
                result.add(new AdminUser(new CaseInsensitiveString(value)));
            }
        }
        return result.toArray(new Admin[result.size()]);
    }

    private Admin[] extractAdminRole(List<Map<String, String>> map) {
        List<Admin> result = new ArrayList<>(map.size());
        for (Map<String, String> usernameMap : map) {
            String value = usernameMap.get("name").trim();
            if (!StringUtil.isBlank(value)) {
                result.add(new AdminRole(new CaseInsensitiveString(value)));
            }
        }
        return result.toArray(new Admin[result.size()]);
    }

    public void removeOperatePermissions() {
        authConfig.clear();
    }
}
