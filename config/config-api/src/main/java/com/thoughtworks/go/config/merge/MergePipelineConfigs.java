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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PipelineConfigVisitor;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.BiPredicate;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Understands pipeline group configuration in many parts.
 * <p>
 * Composite of many pipeline configuration parts.
 */
@ConfigTag("pipelines")
public class MergePipelineConfigs implements PipelineConfigs {

    @ConfigSubtag
    private PipelineConfigsPartials parts = new PipelineConfigsPartials();

    private final ConfigErrors configErrors = new ConfigErrors();

    public MergePipelineConfigs(PipelineConfigs... parts) {
        this.parts.addAll(Arrays.asList(parts));
        validateGroupNameUniqueness(this.parts);
    }

    public MergePipelineConfigs(List<PipelineConfigs> parts) {
        this.parts.addAll(parts);
        validateGroupNameUniqueness(this.parts);
    }

    public void addPart(BasicPipelineConfigs pipelineConfigs) {
        if (!Strings.CS.equals(pipelineConfigs.getGroup(), this.getGroup())) {
            throw new IllegalArgumentException("Group names must be the same in merge");
        }
        this.parts.add(pipelineConfigs);
    }

    private void validateGroupNameUniqueness(List<PipelineConfigs> parts) {
        String name = parts.getFirst().getGroup();
        for (PipelineConfigs part : parts) {
            String otherName = part.getGroup();
            if (!Strings.CS.equals(otherName, name)) {
                throw new IllegalArgumentException("Group names must be the same in merge");
            }
        }
    }


    public PipelineConfigs getAuthorizationPart() {
        PipelineConfigs found = this.getAuthorizationPartOrNull();
        if (found == null) {
            throw bomb("No valid configuration part to store authorization");
        }

        return found;
    }

    public PipelineConfigs getAuthorizationPartOrNull() {
        for (PipelineConfigs part : parts) {
            if (part.getOrigin() != null && part.getOrigin().isLocal()) {
                return part;
            }
        }
        return null;
    }

    public PipelineConfigs getPartWithPipeline(CaseInsensitiveString pipelineName) {
        for (PipelineConfigs part : parts) {
            if (part.hasPipeline(pipelineName)) {
                return part;
            }
        }
        return null;
    }

    public PipelineConfigs getFirstEditablePartOrNull() {
        for (PipelineConfigs part : parts) {
            if (part.isEditable()) {
                return part;
            }
        }
        return null;
    }

    public PipelineConfigs getFirstEditablePart() {
        PipelineConfigs found = getFirstEditablePartOrNull();
        if (found == null) {
            throw bomb("No editable configuration part");
        }

        return found;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        this.validateGroupNameAndAddErrorsTo(this.configErrors);
        for (PipelineConfigs part : this.parts) {
            part.validate(validationContext);
        }

        verifyPipelineNameUniqueness();
    }

    private void verifyPipelineNameUniqueness() {
        Map<CaseInsensitiveString, PipelineConfig> hashMap = new HashMap<>();
        for (PipelineConfig pipelineConfig : this) {
            pipelineConfig.validateNameUniqueness(hashMap);
        }
    }

    @Override
    public void validateNameUniqueness(Map<String, PipelineConfigs> groupNameMap) {
        String currentName = sanitizedGroupName(this.getGroup()).toLowerCase();
        PipelineConfigs groupWithSameName = groupNameMap.get(currentName);
        if (groupWithSameName == null) {
            groupNameMap.put(currentName, this);
        } else {
            groupWithSameName.addError(GROUP, createNameConflictError());
            this.nameConflictError();
        }
    }

    private void nameConflictError() {
        this.configErrors.add(GROUP, createNameConflictError());
    }

    private String createNameConflictError() {
        return String.format("Group with name '%s' already exists", this.getGroup());
    }

    public static String sanitizedGroupName(String group) {
        return isBlank(group) ? DEFAULT_GROUP : group;
    }

    @Override
    public ConfigOrigin getOrigin() {
        MergeConfigOrigin origins = new MergeConfigOrigin();
        for (PipelineConfigs part : this.parts) {
            origins.add(part.getOrigin());
        }
        return origins;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        throw bomb("Cannot set origins on merged config");
    }

    @Override
    public PipelineConfig findBy(CaseInsensitiveString pipelineName) {
        for (PipelineConfigs part : this.parts) {
            PipelineConfig found = part.findBy(pipelineName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public int size() {
        int count = 0;
        for (PipelineConfigs part : this.parts) {
            count += part.size();
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return parts.isEmpty() || parts.stream().allMatch(PipelineConfigs::isEmpty);
    }

    @Override
    public void remove(PipelineConfig pipelineConfig) {
        PipelineConfigs part = this.getPartWithPipeline(pipelineConfig.name());
        if (!part.isEditable()) {
            throw bomb("Cannot remove pipeline fron non-editable configuration source");
        }

        part.remove(pipelineConfig);
    }

    @Override
    public void validateGroupNameAndAddErrorsTo(ConfigErrors errors) {
        this.parts.getFirst().validateGroupNameAndAddErrorsTo(errors);
    }

    @Override
    public PipelineConfigs getLocal() {
        for (PipelineConfigs part : this.parts) {
            if (part.isLocal()) {
                return part;
            }
        }
        return null;
    }

    @Override
    public boolean isLocal() {
        return getOrigin() == null || getOrigin().isLocal();
    }

    @TestOnly
    @Override
    public PipelineConfig getFirst() {
        return get(0);
    }

    @Override
    public boolean add(PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig);
        PipelineConfigs part = this.getFirstEditablePartOrNull();
        if (part == null) {
            throw bomb("No editable configuration sources");
        }

        return part.add(pipelineConfig);
    }

    private void verifyUniqueName(PipelineConfig pipelineConfig) {
        if (alreadyContains(pipelineConfig)) {
            throw bomb("You have defined multiple pipelines called '" + pipelineConfig.name() + "'. Pipeline names must be unique.");
        }
    }

    private boolean alreadyContains(PipelineConfig pipelineConfig) {
        for (PipelineConfigs part : this.parts) {
            if (part.hasPipeline(pipelineConfig.name())) {
                return true;
            }
        }
        return false;
    }

    public PipelineConfigs getPartWithIndexForInsert(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }

        int start = 0;
        for (PipelineConfigs part : this.parts) {
            int end = start + part.size();
            if (i < end) {
                return part;
            }
            start = end;
        }
        return this.parts.getLast();
    }

    @Override
    public PipelineConfig get(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }

        int start = 0;
        for (PipelineConfigs part : this.parts) {
            int end = start + part.size();
            if (i < end) {
                return part.get(i - start);
            }
            start = end;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean addWithoutValidation(PipelineConfig pipelineConfig) {
        PipelineConfigs part = this.getFirstEditablePartOrNull();
        if (part == null) {
            throw bomb("No editable configuration sources");
        }

        return part.addWithoutValidation(pipelineConfig);
    }

    @Override
    public void addToTop(PipelineConfig pipelineConfig) {
        PipelineConfigs part = this.getFirstEditablePart();
        part.addToTop(pipelineConfig);
    }

    @Override
    public void add(int index, PipelineConfig pipelineConfig) {
        PipelineConfigs part = getPartWithIndexForInsert(index);
        if (!part.isEditable()) {
            throw bomb("Cannot add pipeline to non-editable configuration part");
        }

        int start = getFirstIndexInPart(part);

        part.add(index - start, pipelineConfig);
    }

    private int getFirstIndexInPart(PipelineConfigs p) {
        int start = 0;
        for (PipelineConfigs part : this.parts) {
            int end = start + part.size();
            if (part.equals(p)) {
                return start;
            }
            start = end;
        }
        return -1;
    }

    @Override
    public @NotNull Iterator<PipelineConfig> iterator() {
        return new Iterator<>() {
            private final int count = size();
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < count;
            }

            @Override
            public PipelineConfig next() {
                return get(currentIndex++);
            }

            @Override
            public void remove() {
                throw new RuntimeException("Not implemented");
            }
        };
    }


    @Override
    public String getGroup() {
        return this.parts.getFirst().getGroup();
    }

    @Override
    public void setGroup(String group) {
        if (group.equals(this.getGroup())) {
            return;
        }
        for (PipelineConfigs part : this.parts) {
            if (!part.isEditable()) {
                throw bomb("Cannot update group name because there are non-editable parts");
            }
        }
        for (PipelineConfigs part : this.parts) {
            part.setGroup(group);
        }
    }

    @Override
    public boolean isNamed(String groupName) {
        return this.isSameGroup(groupName);
    }

    @Override
    public void update(String groupName, PipelineConfig pipeline, String pipelineName) {
        if (!isSameGroup(groupName)) {
            return;
        }
        CaseInsensitiveString name = new CaseInsensitiveString(pipelineName);
        if (!this.tryReplace((part, p) -> p.name().equals(name) && checkEditable(part, name), pipeline)) {
            throw new IndexOutOfBoundsException("Unable to find pipeline " + pipelineName + " to update");
        }
    }

    private boolean checkEditable(PipelineConfigs part, CaseInsensitiveString pipelineName) {
        if (!part.isEditable()) {
            throw bomb("Cannot edit pipeline " +  pipelineName);
        }
        return true;
    }

    @Override
    public boolean tryReplace(BiPredicate<PipelineConfigs, PipelineConfig> matcher, PipelineConfig newItem) {
        for (PipelineConfigs part : this.parts) {
            if (part.tryReplace(matcher, newItem)) {;
                return true;
            }
        }
        return false;
    }

    private boolean isSameGroup(String groupName) {
        return Strings.CI.equals(groupName, this.getGroup());
    }

    @Override
    public boolean save(PipelineConfig pipeline, String groupName) {
        if (isSameGroup(groupName)) {
            this.addToTop(pipeline);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasPipeline(CaseInsensitiveString pipelineName) {
        for (PipelineConfigs part : this.parts) {
            if (part.hasPipeline(pipelineName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void accept(PipelineConfigVisitor visitor) {
        for (PipelineConfig pipelineConfig : this) {
            visitor.visit(pipelineConfig);
        }
    }

    @Override
    public PipelineConfigs getCopyForEditing() {
        List<PipelineConfigs> parts = new ArrayList<>();
        for (PipelineConfigs part : this.parts) {
            parts.add(part.getCopyForEditing());
        }
        return new MergePipelineConfigs(parts);
    }

    @Override
    public boolean isUserAnAdmin(CaseInsensitiveString userName, List<Role> memberRoles) {
        return this.getAuthorizationPart().isUserAnAdmin(userName, memberRoles);
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public List<PipelineConfig> getPipelines() {
        List<PipelineConfig> list = new ArrayList<>();
        for (PipelineConfig pipe : this) {
            list.add(pipe);
        }
        return list;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setConfigAttributes(Object attributes) {
        Map<String, Object> attributeMap = (Map<String, Object>) attributes;
        if (attributeMap == null) {
            return;
        }
        if (attributeMap.containsKey(GROUP)) {
            String group = (String) attributeMap.get(GROUP);
            this.setGroup(group);
        }
        if (attributeMap.containsKey(AUTHORIZATION) || attributeMap.isEmpty()) {
            PipelineConfigs authorizationPart = this.getAuthorizationPart();
            authorizationPart.setConfigAttributes(attributes);
        }
    }

    @Override
    public void cleanupAllUsagesOfRole(Role roleToDelete) {
        this.getAuthorizationPart().cleanupAllUsagesOfRole(roleToDelete);
    }

    @Override
    public boolean hasAuthorizationDefined() {
        PipelineConfigs authPart = this.getAuthorizationPartOrNull();
        if (authPart == null) {
            return false;
        }
        return authPart.hasAuthorizationDefined();
    }

    @Override
    public Authorization getAuthorization() {
        return this.getAuthorizationPart().getAuthorization();
    }

    @Override
    public void setAuthorization(Authorization authorization) {
        this.getAuthorizationPart().setAuthorization(authorization);
    }


    @Override
    public boolean hasViewPermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher, boolean everyoneIsAllowedToViewIfNoAuthIsDefined) {
        return this.getAuthorizationPart().hasViewPermission(username, userRoleMatcher, everyoneIsAllowedToViewIfNoAuthIsDefined);
    }

    @Override
    public boolean hasViewPermissionDefined() {
        PipelineConfigs authPart = this.getAuthorizationPartOrNull();
        if (authPart == null) {
            return false;
        }
        return authPart.hasViewPermissionDefined();
    }

    @Override
    public boolean hasOperationPermissionDefined() {
        PipelineConfigs authPart = this.getAuthorizationPartOrNull();
        if (authPart == null) {
            return false;
        }
        return authPart.hasOperationPermissionDefined();
    }

    @Override
    public boolean hasOperatePermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher, boolean everyoneIsAllowedToOperateIfNoAuthIsDefined) {
        return this.getAuthorizationPart().hasOperatePermission(username, userRoleMatcher, everyoneIsAllowedToOperateIfNoAuthIsDefined);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergePipelineConfigs that = (MergePipelineConfigs) o;
        return Objects.equals(parts, that.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parts);
    }
}
