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

package com.thoughtworks.go.config.exceptions;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.thoughtworks.go.config.exceptions.NameOrId.*;
import static java.lang.String.format;

public enum EntityType {
    AccessToken("access token", id),
    PackageDefinition("package definition", id),
    PackageRepository("package repository", id),
    PluginSettings("plugin settings", id),
    Agent("agent", uuid),
    ArtifactStore("artifact store", id),
    ElasticProfile("elastic agent profile", id),
    Environment("environment", name),
    Pipeline("pipeline", name),
    PipelineGroup("pipeline group", name),
    Role("role", name),
    Template("template", name),
    User("user", loginName),
    SecurityAuthConfig("security auth config", id),
    ConfigRepo("config repo", id),
    SCM("SCM", id),
    ClusterProfile("cluster profile", id),
    Backup("backup", id),
    SecretConfig("secret config", id),
    BackupConfig("backup config", none),
    SMTP("SMTP config", none),
    ArtifactConfig("artifact config", none);

    private final String entityType;
    private final NameOrId nameOrId;

    EntityType(String entityType, NameOrId nameOrId) {
        this.entityType = entityType;
        this.nameOrId = nameOrId;
    }

    public String deleteSuccessful() {
        return format("%s was deleted successfully!", StringUtils.capitalize(this.entityType));
    }

    public String deleteSuccessful(String id) {
        return format("%s %s '%s' was deleted successfully!", StringUtils.capitalize(this.entityType), this.nameOrId.descriptor, id);
    }

    public String deleteSuccessful(List<?> ids) {
        return format("%ss %ss '%s' were deleted successfully!", StringUtils.capitalize(this.entityType), this.nameOrId.descriptor, StringUtils.join(ids, ", "));
    }

    public String deleteSuccessful(CaseInsensitiveString id) {
        return deleteSuccessful(id.toString());
    }

    public String staleConfig(String id) {
        return format("Someone has modified the configuration for %s %s '%s'. Please update your copy of the config with the changes.",
                this.entityType.toLowerCase(),
                this.nameOrId.descriptor,
                id);
    }

    public String staleConfig() {
        return format("Someone has modified the configuration for %s. Please update your copy of the config with the changes.", this.entityType.toLowerCase());
    }

    public String staleConfig(CaseInsensitiveString id) {
        return staleConfig(id.toString());
    }

    public String entityConfigValidationFailed(String id, String errors) {
        return format("Validations failed for %s %s '%s'. Error(s): [%s]. Please correct and resubmit.",
                this.entityType.toLowerCase(),
                this.nameOrId.descriptor,
                id,
                errors);
    }

    public String entityConfigValidationFailed(CaseInsensitiveString id, String errors) {
        return entityConfigValidationFailed(id.toString(), errors);
    }

    public String notFoundMessage() {
        return format("%s was not found!", StringUtils.capitalize(this.entityType));
    }

    public String notFoundMessage(String id) {
        return format("%s %s '%s' was not found!", StringUtils.capitalize(this.entityType), this.nameOrId.descriptor, id);
    }

    public String notFoundMessage(long id) {
        return notFoundMessage(String.valueOf(id));
    }

    public String notFoundMessage(CaseInsensitiveString id) {
        return notFoundMessage(id.toString());
    }

    public String notFoundMessage(List<?> ids) {
        return format("%ss %ss '%s' were not found!", StringUtils.capitalize(this.entityType), this.nameOrId.descriptor, StringUtils.join(ids, ","));
    }

    public String getEntityNameLowerCase() {
        return entityType.toLowerCase();
    }

    public String alreadyExists(String id) {
        return format("%s %s '%s' already exists!", StringUtils.capitalize(this.entityType), this.nameOrId.descriptor, id);
    }

    public String alreadyExists(CaseInsensitiveString id) {
        return alreadyExists(id.toString());
    }

    public String forbiddenToView(String id, String username) {
        return format("User '%s' does not have permission to view %s %s '%s'", username, entityType.toLowerCase(), nameOrId.descriptor, id);
    }

    public String forbiddenToView(CaseInsensitiveString id, CaseInsensitiveString username) {
        return forbiddenToView(id.toString(), username.toString());
    }

    public String forbiddenToView(String id, CaseInsensitiveString username) {
        return forbiddenToView(id, username.toString());
    }

    public String forbiddenToView(CaseInsensitiveString id, String username) {
        return forbiddenToView(id.toString(), username);
    }

    public String forbiddenToEdit(String id, String username) {
        return format("User '%s' does not have permission to edit %s %s '%s'", username, entityType.toLowerCase(), nameOrId.descriptor, id);
    }

    public String forbiddenToEdit(CaseInsensitiveString id, CaseInsensitiveString username) {
        return forbiddenToEdit(id.toString(), username.toString());
    }

    public String forbiddenToEdit(String id, CaseInsensitiveString username) {
        return forbiddenToEdit(id, username.toString());
    }

    public String forbiddenToEdit(CaseInsensitiveString id, String username) {
        return forbiddenToEdit(id.toString(), username);
    }

    public String forbiddenToDelete(CaseInsensitiveString id, CaseInsensitiveString username) {
        return forbiddenToDelete(id.toString(), username.toString());
    }

    public String forbiddenToDelete(String id, CaseInsensitiveString username) {
        return forbiddenToDelete(id, username.toString());
    }

    public String forbiddenToDelete(CaseInsensitiveString id, String username) {
        return forbiddenToDelete(id.toString(), username);
    }

    public String forbiddenToDelete(String id, String username) {
        return format("User '%s' does not have permission to delete %s %s '%s'", username, entityType.toLowerCase(), nameOrId.descriptor, id);
    }

    public String idCannotBeBlank() {
        return StringUtils.capitalize(this.entityType) + " " + nameOrId.descriptor + " cannot be blank.";
    }

    public String forbiddenToDelete(CaseInsensitiveString username) {
        return format("User '%s' does not have permission to delete %s", username, entityType.toLowerCase());
    }
}
