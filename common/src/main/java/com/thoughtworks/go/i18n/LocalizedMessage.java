/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.i18n;

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * Understands converting the localized message for a given key
 */
public abstract class LocalizedMessage {

    public static String cannotDeleteResourceBecauseOfDependentPipelines(String resourceTypeBeingDeleted, CaseInsensitiveString resourceNameBeingDeleted, List<String> dependentPipelines) {
        return cannotDeleteResourceBecauseOfDependentPipelines(resourceTypeBeingDeleted, resourceNameBeingDeleted.toString(), dependentPipelines);
    }

    public static String cannotDeleteResourceBecauseOfDependentPipelines(String resourceTypeBeingDeleted, String resourceNameBeingDeleted, List<String> dependentPipelines) {
        return "Cannot delete the " + resourceTypeBeingDeleted + " '" + resourceNameBeingDeleted + "' as it is used by pipeline(s): '" + join(dependentPipelines, ", ") + "'";
    }

    public static String cannotDeleteResourceBecauseOfDependentResources(String resourceTypeBeingDeleted, String resourceNameBeingDeleted, String dependentResourceType, List<String> dependentResourceUUIDs) {
        return String.format("Cannot delete %s '%s' as it is referenced from %s(s) [%s]", resourceTypeBeingDeleted, resourceNameBeingDeleted, dependentResourceType, join(dependentResourceUUIDs, ", "));
    }

    public static String staleResourceConfig(String resourceType, String resourceName) {
        return "Someone has modified the configuration for " + resourceType + " '" + resourceName + "'. Please update your copy of the config with the changes.";
    }

    public static String forbiddenToViewPipeline(CaseInsensitiveString pipelineName) {
        return forbiddenToViewPipeline(pipelineName.toString());
    }

    public static String forbiddenToViewPipeline(String pipelineName) {
        return "You do not have view permissions for pipeline '" + pipelineName + "'.";
    }

    public static String resourceNotFound(String resourceType, String resourceName) {
        return resourceType + " '" + resourceName + "' not found.";
    }

    public static String forbiddenToEdit() {
        return "Unauthorized to edit.";
    }

    public static String forbiddenToEditPipeline(String pipelineName) {
        return "Unauthorized to edit '" + pipelineName + "' pipeline.";
    }

    public static String entityConfigValidationFailed(String entityType, String entityName, String errors) {
        return "Validations failed for " + entityType + " '" + entityName + "'. Error(s): [" + errors + "]. Please correct and resubmit.";
    }

    public static String entityConfigValidationFailed(String entityType, String errors) {
        return "Validations failed for " + entityType + ". Error(s): [" + errors + "]. Please correct and resubmit.";
    }

    public static String entityConfigValidationFailed(String entityType, CaseInsensitiveString entityName, String errors) {
        return entityConfigValidationFailed(entityType, entityName.toString(), errors);
    }

    public static String saveFailedWithReason(String reason) {
        return "Save failed. " + reason;
    }

    public static String composite(String... messages) {
        return join(messages, " ");
    }

    public static String resourceDeleteSuccessful(String entityType, String entityName) {
        return "The " + entityType + " '" + entityName + "' was deleted successfully.";
    }

    public static String resourceDeleteSuccessful(String entityType, CaseInsensitiveString entityName) {
        return resourceDeleteSuccessful(entityType, entityName.toString());
    }

    public static String resourcesDeleteSuccessful(String entityType, List<String> names) {
        return entityType + " '" + join(names, ", ") + "' were deleted successfully.";
    }

}
