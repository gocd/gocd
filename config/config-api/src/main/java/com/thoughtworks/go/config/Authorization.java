/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigOriginTraceable;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.sort;

@ConfigTag("authorization")
public class Authorization implements Validatable, ParamsAttributeAware, ConfigOriginTraceable {
    @ConfigSubtag
    private ViewConfig viewConfig = new ViewConfig();

    @ConfigSubtag
    private OperationConfig operationConfig = new OperationConfig();

    @ConfigSubtag
    private AdminsConfig adminsConfig = new AdminsConfig();

    @ConfigAttribute(value = "allGroupAdminsAreViewers")
    private boolean allowGroupAdmins = true;

    private final ConfigErrors configErrors = new ConfigErrors();
    public static final String NAME = "name";
    public static final String ALLOW_GROUP_ADMINS="allowGroupAdmins";
    public static final String PRIVILEGES = "privileges";
    public static final String VALUE = "value";
    public static final String PRIVILEGE_TYPE = "privilege_type";
    public static final String TYPE = "type";

    private ConfigOrigin origin;

    @Override
    public ConfigOrigin getOrigin() {
        return origin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        origin = origins;
    }

    public static enum UserType {
        USER {
            @Override public Admin makeUser(String name) {
                return new AdminUser(new CaseInsensitiveString(name));
            }},
        ROLE {
            @Override public Admin makeUser(String name) {
                return new AdminRole(new CaseInsensitiveString(name));
            }};

        abstract public Admin makeUser(String name);
    }

    public static enum PrivilegeType {
        ADMIN {
            @Override public AdminsConfig group(Authorization authorization) {
                return authorization.getAdminsConfig();
            }
            @Override public void set(PresentationElement el) {
                el.makeAdmin();
            }},
        OPERATE {
            @Override public AdminsConfig group(Authorization authorization) {
                return authorization.getOperationConfig();
            }
            @Override public void set(PresentationElement el) {
                el.makeOperator();
            }},
        VIEW {
            @Override public AdminsConfig group(Authorization authorization) {
                return authorization.getViewConfig();
            }
            @Override public void set(PresentationElement el) {
                el.makeViewer();
            }};

        abstract public AdminsConfig group(Authorization authorization);

        public abstract void set(PresentationElement el);
    }

    public static enum PrivilegeState {
        ON {
            @Override public void apply(AdminsConfig adminsConfig, Admin userOrRole) {
                adminsConfig.add(userOrRole);
            }},
        OFF,
        DISABLED;

        public void apply(AdminsConfig adminsConfig, Admin userOrRole) {
        }
    }

    public Authorization() {
    }

    public Authorization(ViewConfig viewConfig, OperationConfig operationConfig, AdminsConfig adminsConfig) {
        this.viewConfig = viewConfig;
        this.operationConfig = operationConfig;
        this.adminsConfig = adminsConfig;
    }

    public Authorization(ViewConfig viewConfig) {
        this.viewConfig = viewConfig;
    }

    public Authorization(OperationConfig operationConfig) {
        this.operationConfig = operationConfig;
    }

    public Authorization(AdminsConfig adminsConfig) {
        this.adminsConfig = adminsConfig;
    }

    public Authorization(OperationConfig operationConfig, ViewConfig viewConfig) {
        this.operationConfig = operationConfig;
        this.viewConfig = viewConfig;
    }

    //only for test
    public ViewConfig getViewConfig() {
        return viewConfig;
    }

    public AdminsConfig getOperationConfig() {
        return operationConfig;
    }

    public boolean hasViewPermission(final CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return viewConfig.hasUser(username, userRoleMatcher);
    }

    public boolean hasOperationPermissionDefined() {
        return !operationConfig.equals(new OperationConfig());
    }

    public boolean hasViewPermissionDefined() {
        return !viewConfig.equals(new ViewConfig());
    }

    public boolean hasOperatePermission(final CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return operationConfig.hasUser(username, userRoleMatcher);
    }

    public boolean hasAdminsDefined() {
        return !adminsConfig.equals(new AdminsConfig());
    }

    public boolean isUserAnAdmin(final CaseInsensitiveString userName, List<Role> memberRoles) {
        return adminsConfig.isAdmin(new AdminUser(userName), memberRoles);
    }

    public boolean isViewUser(final CaseInsensitiveString username, List<Role> memberRoles) {
        return viewConfig.isAdmin(new AdminUser(username), memberRoles);
    }

    public boolean hasAdminOrViewPermissions(final CaseInsensitiveString userName, List<Role> memberRoles) {
        return isUserAnAdmin(userName, memberRoles) || isViewUser(userName, memberRoles);
    }

    public AdminsConfig getAdminsConfig() {
        return adminsConfig;
    }

    public void setAdminsConfig(AdminsConfig adminsConfig) {
        this.adminsConfig = adminsConfig;
    }

    public void setViewConfig(ViewConfig viewConfig) {
        this.viewConfig = viewConfig;
    }

    public void setOperationConfig(OperationConfig operationConfig) {
        this.operationConfig = operationConfig;
    }

    public void setAllowGroupAdmins(boolean allowGroupAdmins) {
        this.allowGroupAdmins = allowGroupAdmins;
    }

    public boolean isAllowGroupAdmins() {
        return allowGroupAdmins;
    }

    public List<PrivilegeType> privilagesOfRole(final CaseInsensitiveString roleName){
        List<PrivilegeType> result = new ArrayList<>();
        if (isRoleAnAdmin(roleName)){
            result.add(PrivilegeType.ADMIN);
        }
        if (isRoleAnOperator(roleName)){
            result.add(PrivilegeType.OPERATE);
        }
        if (isRoleAViewer(roleName)){
            result.add(PrivilegeType.VIEW);
        }
        return result;
    }

    public void validateTree(ValidationContext validationContext) {
        for (Admin admin : getAdminsConfig()) {
            admin.validate(validationContext);
            this.getAdminsConfig().errors().addAll(admin.errors());
        }
        for (Admin admin : getViewConfig()) {
            admin.validate(validationContext);
            this.getViewConfig().errors().addAll(admin.errors());
        }

        for (Admin admin : getOperationConfig()) {
            admin.validate(validationContext);
            this.getOperationConfig().errors().addAll(admin.errors());
        }
    }

    @Override
    public void validate(ValidationContext validationContext) {
        return;
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        List<Map<String, Object>> attributeMap = (List) attributes;
        for (Map<String, Object> userMap : attributeMap) {
            String name = (String) userMap.get(NAME);
            if (StringUtils.isBlank(name)) {
                continue;
            }
            UserType type = UserType.valueOf((String) userMap.get(TYPE));
            Admin admin = type.makeUser(name);
            for (Map.Entry<String, String> privilegeEntry : ((Map<String, String>) ((List) userMap.get(PRIVILEGES)).get(0)).entrySet()) {
                PrivilegeType privilegeType = PrivilegeType.valueOf(privilegeEntry.getKey().toString().toUpperCase());
                AdminsConfig privilegeGroup = privilegeType.group(this);
                PrivilegeState state = PrivilegeState.valueOf(privilegeEntry.getValue());
                state.apply(privilegeGroup, admin);
            }
            //Give default view permission if no checkbox has been checked in the admin UI
            if(!(adminsConfig.contains(admin) || operationConfig.contains(admin) || viewConfig.contains(admin))){
                viewConfig.add(admin);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Authorization that = (Authorization) o;

        if (adminsConfig != null ? !adminsConfig.equals(that.adminsConfig) : that.adminsConfig != null) {
            return false;
        }
        if (operationConfig != null ? !operationConfig.equals(that.operationConfig) : that.operationConfig != null) {
            return false;
        }
        if (viewConfig != null ? !viewConfig.equals(that.viewConfig) : that.viewConfig != null) {
            return false;
        }
        if (allowGroupAdmins != that.allowGroupAdmins) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = viewConfig != null ? viewConfig.hashCode() : 0;
        result = 31 * result + (operationConfig != null ? operationConfig.hashCode() : 0);
        result = 31 * result + (adminsConfig != null ? adminsConfig.hashCode() : 0);
        result = 31 * result + (allowGroupAdmins ? 1 : 0);
        return result;
    }

    public void removeAllUsagesOfRole(Role role) {
        adminsConfig.removeRole(role);
        viewConfig.removeRole(role);
        operationConfig.removeRole(role);
    }

    public static class PresentationElement implements Comparable<PresentationElement>, Validatable {
        public static final String NAME = "name";
        private String name;

        public static final String TYPE = "type";
        private UserType type;

        public static final String ADMIN_PRIVILEGE = "admin";
        private PrivilegeState adminPrivilege;

        public static final String OPERATE_PRIVILEGE = "operate";
        private PrivilegeState operatePrivilege;

        public static final String VIEW_PRIVILEGE = "view";
        private PrivilegeState viewPrivilege;

        private ConfigErrors configErrors = new ConfigErrors();


        public PresentationElement(String name, UserType type) {
            this.name = name;
            this.type = type;
            adminPrivilege = operatePrivilege = viewPrivilege = PrivilegeState.OFF;
        }

        public String getName() {
            return name;
        }

        public PrivilegeState getAdmin() {
            return adminPrivilege;
        }

        public PrivilegeState getView() {
            return viewPrivilege;
        }

        public PrivilegeState getOperate() {
            return operatePrivilege;
        }

        public void makeAdmin() {
            adminPrivilege = PrivilegeState.ON;
            viewPrivilege = PrivilegeState.DISABLED;
            operatePrivilege = PrivilegeState.DISABLED;
        }

        public void makeOperator() {
            operatePrivilege = PrivilegeState.ON;
        }

        public void makeViewer() {
            viewPrivilege = PrivilegeState.ON;
        }

        public UserType getType() {
            return type;
        }

        @Override
        public int compareTo(PresentationElement other) {
            return this.name.compareTo(other.name);
        }

        @Override
        public void validate(ValidationContext validationContext) {
        }

        @Override
        public ConfigErrors errors() {
            return configErrors;
        }

        @Override
        public void addError(String fieldName, String message) {
            configErrors.add(fieldName, message);
        }
    }

    public List<PresentationElement> getUserAuthorizations() {
        ArrayList<PresentationElement> list = new ArrayList<>();
        Class<AdminUser> allowOnly = AdminUser.class;
        addPrivilegesForView(list, operationConfig, PrivilegeType.OPERATE, allowOnly, UserType.USER);
        addPrivilegesForView(list, viewConfig, PrivilegeType.VIEW, allowOnly, UserType.USER);
        addPrivilegesForView(list, adminsConfig, PrivilegeType.ADMIN, allowOnly, UserType.USER);
        sort(list);
        return list;
    }

    public List<PresentationElement> getRoleAuthorizations() {
        ArrayList<PresentationElement> list = new ArrayList<>();
        Class<AdminRole> onlyOfType = AdminRole.class;
        addPrivilegesForView(list, operationConfig, PrivilegeType.OPERATE, onlyOfType, UserType.ROLE);
        addPrivilegesForView(list, viewConfig, PrivilegeType.VIEW, onlyOfType, UserType.ROLE);
        addPrivilegesForView(list, adminsConfig, PrivilegeType.ADMIN, onlyOfType, UserType.ROLE);
        sort(list);
        return list;
    }

    @Override
    public String toString() {
        return String.format("Authorization [view: %s] [operate: %s] [admins: %s] [allowGroupAdmins: %s]", viewConfig, operationConfig, adminsConfig, allowGroupAdmins);
    }

    private void addPrivilegesForView(ArrayList<PresentationElement> list, final AdminsConfig privilegesCollection, final PrivilegeType privilegeType, final Class<? extends Admin> allowOnly,
                                      final UserType type) {
        for (Admin admin : privilegesCollection) {
            if (allowOnly.isAssignableFrom(admin.getClass())) {
                addPresentationPrivilege(admin, list, privilegeType, type);
            }
        }
    }

    private void addPresentationPrivilege(Admin admin, ArrayList<PresentationElement> list, PrivilegeType privilegeType, final UserType type) {
        PresentationElement el = null;
        for (PresentationElement presentationElement : list) {
            if (presentationElement.getName().equals(CaseInsensitiveString.str(admin.getName()))) {
                el = presentationElement;
            }
        }
        if (el == null) {
            el = new PresentationElement(CaseInsensitiveString.str(admin.getName()), type);
            if (!admin.errors().isEmpty()) {
                el.addError(Admin.NAME, admin.errors().on(Admin.NAME));
            }
            list.add(el);
        }
        privilegeType.set(el);
    }

    private boolean isRoleAnAdmin(final CaseInsensitiveString roleName){
        return containsRole(roleName, adminsConfig.getRoles());
    }

    private boolean isRoleAnOperator(final CaseInsensitiveString roleName){
        return containsRole(roleName, operationConfig.getRoles());
    }

    private boolean isRoleAViewer(final CaseInsensitiveString roleName){
        return containsRole(roleName, viewConfig.getRoles());
    }

    private boolean containsRole(final CaseInsensitiveString roleName, List<AdminRole> roles){
        for(AdminRole role : roles){
            if (role.getName().equals(roleName)){
                return true;
            }
        }
        return false;
    }
}
