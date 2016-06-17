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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands pipeline group configuration in many parts.
 *
 * Composite of many pipeline configuration parts.
 */
@ConfigTag("pipelines")
public class MergePipelineConfigs implements PipelineConfigs {

    @ConfigSubtag
    private PipelineConfigsPartials parts = new PipelineConfigsPartials();

    private final ConfigErrors configErrors = new ConfigErrors();

    public MergePipelineConfigs(PipelineConfigs... parts)
    {
        this.parts.addAll(Arrays.asList(parts));
        validateGroupNameUniqueness(this.parts);
    }
    public MergePipelineConfigs(List<PipelineConfigs> parts)
    {
        this.parts.addAll(parts);
        validateGroupNameUniqueness(this.parts);
    }

    public void addPart(BasicPipelineConfigs pipelineConfigs) {
        if (!StringUtils.equals(pipelineConfigs.getGroup(), this.getGroup()))
            throw new IllegalArgumentException("Group names must be the same in merge");
        this.parts.add(pipelineConfigs);
    }

    private void validateGroupNameUniqueness(List<PipelineConfigs> parts) {
        String name = parts.get(0).getGroup();
        for (PipelineConfigs part : parts) {
            String otherName = part.getGroup();
            if (!StringUtils.equals(otherName, name))
                throw new IllegalArgumentException("Group names must be the same in merge");
        }
    }


    public PipelineConfigs getAuthorizationPart()
    {
        PipelineConfigs found = this.getAuthorizationPartOrNull();
        if(found == null)
            throw bomb("No valid configuration part to store authorization");

        return  found;
    }
    public PipelineConfigs getAuthorizationPartOrNull()
    {
        for(PipelineConfigs part : parts)
        {
            if(part.getOrigin() != null && part.getOrigin().isLocal())
                return part;
        }
        return null;
    }

    public PipelineConfigs getPartWithPipeline(CaseInsensitiveString pipelineName)
    {
        for(PipelineConfigs part : parts)
        {
            if(part.hasPipeline(pipelineName))
                return part;
        }
        return null;
    }

    public PipelineConfigs getFirstEditablePartOrNull()
    {
        for(PipelineConfigs part : parts)
        {
            if(isEditable(part))
                return  part;
        }
        return  null;
    }
    public PipelineConfigs getFirstEditablePart()
    {
        PipelineConfigs found = getFirstEditablePartOrNull();
        if(found == null)
            throw bomb("No editable configuration part");

        return found;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        this.validateGroupNameAndAddErrorsTo(this.configErrors);
        for(PipelineConfigs part : this.parts)
        {
            part.validate(validationContext);
        }

        verifyPipelineNameUniqueness();
    }

    private void verifyPipelineNameUniqueness() {
        HashMap<CaseInsensitiveString, PipelineConfig> hashMap = new HashMap<>();
        for(PipelineConfig pipelineConfig : this){
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
        return StringUtils.isBlank(group) ? DEFAULT_GROUP : group;
    }

    @Override
    public ConfigOrigin getOrigin() {
        MergeConfigOrigin origins = new MergeConfigOrigin();
        for(PipelineConfigs part : this.parts)
        {
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
        for (PipelineConfigs part : this.parts)
        {
            PipelineConfig found = part.findBy(pipelineName);
            if(found != null)
                return found;
        }
        return  null;
    }

    @Override
    public int size() {
        int count = 0;
        for (PipelineConfigs part : this.parts)
        {
            count += part.size();
        }
        return  count;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean hasRemoteParts() {
        return getOrigin() != null && !getOrigin().isLocal();
    }

    @Override
    public boolean contains(PipelineConfig o) {
        for (PipelineConfigs part : this.parts)
        {
            if(part.contains(o))
                return  true;
        }
        return  false;
    }

    @Override
    public void remove(PipelineConfig pipelineConfig) {
        PipelineConfigs part = this.getPartWithPipeline(pipelineConfig.name());
        if(!isEditable(part))
            throw bomb("Cannot remove pipeline fron non-editable configuration source");

        part.remove(pipelineConfig);
    }

    @Override
    public PipelineConfig remove(int i) {
        if(i < 0)
            throw new IndexOutOfBoundsException();

        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();
            if(i < end)
                return  part.remove(i - start);
            start = end;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void validateGroupNameAndAddErrorsTo(ConfigErrors errors) {
        this.parts.get(0).validateGroupNameAndAddErrorsTo(errors);
    }

    public PipelineConfigs getLocal() {
        for (PipelineConfigs part : this.parts)
        {
            if(part.isLocal())
                return part;
        }
        return null;
    }

    @Override
    public boolean isLocal() {
        return getOrigin() == null || getOrigin().isLocal();
    }

    @Override
    public boolean add(PipelineConfig pipelineConfig) {
        verifyUniqueName(pipelineConfig);
        PipelineConfigs part = this.getFirstEditablePartOrNull();
        if(part == null)
            throw bomb("No editable configuration sources");

        return part.add(pipelineConfig);
    }
    private void verifyUniqueName(PipelineConfig pipelineConfig) {
        if (alreadyContains(pipelineConfig)) {
            throw bomb("You have defined multiple pipelines called '" + pipelineConfig.name() + "'. Pipeline names must be unique.");
        }
    }
    private boolean alreadyContains(PipelineConfig pipelineConfig) {
        for (PipelineConfigs part : this.parts)
        {
            if(part.hasPipeline(pipelineConfig.name()))
                return true;
        }
        return  false;
    }

    public PipelineConfigs getPartWithIndex(int i)
    {
        if(i < 0)
            throw new IndexOutOfBoundsException();

        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();
            if(i < end)
                return  part;
            start = end;
        }
        throw new IndexOutOfBoundsException();
    }
    public PipelineConfigs getPartWithIndexForInsert(int i)
    {
        if(i < 0)
            throw new IndexOutOfBoundsException();

        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();
            if(i < end)
                return  part;
            start = end;
        }
        return  this.parts.get(this.parts.size() -1);
    }


    @Override
    public PipelineConfig get(int i) {
        if(i < 0)
            throw new IndexOutOfBoundsException();

        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();
            if(i < end)
                return  part.get(i - start);
            start = end;
        }
        throw new IndexOutOfBoundsException();
    }


    @Override
    public boolean addWithoutValidation(PipelineConfig pipelineConfig) {
        PipelineConfigs part = this.getFirstEditablePartOrNull();
        if(part == null)
            throw bomb("No editable configuration sources");

        return part.addWithoutValidation(pipelineConfig);
    }

    @Override
    public PipelineConfig set(int i, PipelineConfig pipelineConfig) {
        if(i < 0)
            throw new IndexOutOfBoundsException();

        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();
            if(i < end) {
                if(isEditable(part)) {
                    return part.set(i - start, pipelineConfig);
                }
                else {
                    throw bomb(String.format("Cannot edit pipeline %s", pipelineConfig.name()));
                }
            }
            start = end;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void addToTop(PipelineConfig pipelineConfig) {
        PipelineConfigs part = this.getFirstEditablePart();
        part.addToTop(pipelineConfig);
    }

    @Override
    public void add(int index, PipelineConfig pipelineConfig) {
        PipelineConfigs part = getPartWithIndexForInsert(index);
        if(!isEditable(part))
            throw bomb("Cannot add pipeline to non-editable configuration part");

        int start = getFirstIndexInPart(part);

        part.add(index - start, pipelineConfig);
    }

    private int getFirstIndexInPart(PipelineConfigs p) {
        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();
            if(part.equals(p))
                return  start;
            start = end;
        }
        return  -1;
    }

    @Override
    public int indexOf(PipelineConfig o) {
        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();

            int internalIndex = part.indexOf(o);
            if(internalIndex > 0)
                return  start + internalIndex;

            start = end;
        }
        return  -1;
    }

    @Override
    public Iterator<PipelineConfig> iterator() {
        return new Iterator<PipelineConfig>() {

            private int currentIndex = 0;
            private int count = size();

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
        return this.parts.get(0).getGroup();
    }

    @Override
    public void setGroup(String group) {
        if(group.equals(this.getGroup()))
        {
            return;
        }
        for(PipelineConfigs part : this.parts)
        {
            if(!isEditable(part))
            {
                throw bomb("Cannot update group name because there are non-editable parts");
            }
        }
        for(PipelineConfigs part : this.parts)
        {
            part.setGroup(group);
        }
    }

    private boolean isEditable(PipelineConfigs part) {
        return part.getOrigin() != null && part.getOrigin().canEdit();
    }

    @Override
    public boolean isNamed(String groupName) {
        return this.isSameGroup(groupName);
    }

    public void update(String groupName, PipelineConfig pipeline, String pipelineName) {
        if (!isSameGroup(groupName)) {
            return;
        }
        this.set(getIndex(pipelineName), pipeline);
    }
    private boolean isSameGroup(String groupName) {
        return StringUtils.equals(groupName, this.getGroup());
    }

    private int getIndex(String pipelineName) {
        CaseInsensitiveString caseName = new CaseInsensitiveString(pipelineName);
        int start =0;
        for (PipelineConfigs part : this.parts)
        {
            int end = start + part.size();

            if(part.hasPipeline(caseName))
            {
                int internalIndex = part.indexOf(part.findBy(caseName));
                return  start + internalIndex;
            }

            start = end;
        }
        return  -1;
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
    public void add(List<String> allGroup) {
        allGroup.add(this.getGroup());
    }

    @Override
    public boolean exist(int pipelineIndex) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasPipeline(CaseInsensitiveString pipelineName) {
        for (PipelineConfigs part : this.parts)
        {
            if(part.hasPipeline(pipelineName))
                return true;
        }
        return false;
    }

    @Override
    public void accept(PiplineConfigVisitor visitor) {
        for (PipelineConfig pipelineConfig : this) {
            visitor.visit(pipelineConfig);
        }
    }

    @Override
    public boolean hasTemplate() {
        for(PipelineConfigs part : this.parts)
        {
            if(part.hasTemplate())
                return  true;
        }
        return  false;
    }

    @Override
    public PipelineConfigs getCopyForEditing() {
        List<PipelineConfigs> parts = new ArrayList<>();
        for(PipelineConfigs part : this.parts)
        {
            parts.add(part.getCopyForEditing());
        }
        return new MergePipelineConfigs(parts);
    }

    @Override
    public boolean isUserAnAdmin(CaseInsensitiveString userName, List<Role> memberRoles) {
        return this.getAuthorizationPart().isUserAnAdmin(userName,memberRoles);
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public List<PipelineConfig> getPipelines() {
        List<PipelineConfig> list = new ArrayList<>();
        for(PipelineConfig pipe : this)
        {
            list.add(pipe);
        }
        return  list;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        Map attributeMap = (Map) attributes;
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
    public List<AdminUser> getOperateUsers() {
        return this.getAuthorizationPart().getOperateUsers();
    }

    @Override
    public List<AdminRole> getOperateRoles() {
        return this.getAuthorizationPart().getOperateRoles();
    }

    @Override
    public List<String> getOperateRoleNames() {
        return this.getAuthorizationPart().getOperateRoleNames();
    }

    @Override
    public List<String> getOperateUserNames() {
        return this.getAuthorizationPart().getOperateUserNames();
    }

    @Override
    public void cleanupAllUsagesOfRole(Role roleToDelete) {
        this.getAuthorizationPart().cleanupAllUsagesOfRole(roleToDelete);
    }


    @Override
    public boolean hasAuthorizationDefined() {
        PipelineConfigs authPart = this.getAuthorizationPartOrNull();
        if(authPart == null)
            return  false;
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
    public boolean hasViewPermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return this.getAuthorizationPart().hasViewPermission(username,userRoleMatcher);
    }

    @Override
    public boolean hasViewPermissionDefined() {
        PipelineConfigs authPart = this.getAuthorizationPartOrNull();
        if(authPart == null)
            return  false;
        return authPart.hasViewPermissionDefined();
    }

    @Override
    public boolean hasOperationPermissionDefined() {
        PipelineConfigs authPart = this.getAuthorizationPartOrNull();
        if(authPart == null)
            return  false;
        return authPart.hasOperationPermissionDefined();
    }

    @Override
    public boolean hasOperatePermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return this.getAuthorizationPart().hasOperatePermission(username,userRoleMatcher);
    }

}
