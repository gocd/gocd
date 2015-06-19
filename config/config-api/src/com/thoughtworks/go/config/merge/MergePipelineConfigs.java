package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
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
public class MergePipelineConfigs implements PipelineConfigs {

    private List<PipelineConfigs> parts = new ArrayList<PipelineConfigs>();

    private final ConfigErrors configErrors = new ConfigErrors();

    public MergePipelineConfigs(PipelineConfigs... parts)
    {
        this.parts.addAll(Arrays.asList(parts));
        validateGroupNameUniqueness(this.parts);
    }
    public MergePipelineConfigs(List<PipelineConfigs> parts)
    {
        this.parts = parts;
        validateGroupNameUniqueness(this.parts);
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
            if(part.getOrigin() != null && part.getOrigin().canEdit())
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
        String group = this.getGroup();
        if (StringUtils.isBlank(group) || !new NameTypeValidator().isNameValid(group)) {
            this.configErrors.add(GROUP, NameTypeValidator.errorMessage("group", group));
        }
        for(PipelineConfigs part : this.parts)
        {
            part.validate(validationContext);
        }

        verifyPipelineNameUniqueness();
    }

    private void verifyPipelineNameUniqueness() {
        HashMap<String, PipelineConfig> hashMap = new HashMap<String, PipelineConfig>();
        for(PipelineConfig pipelineConfig : this){
            pipelineConfig.validateNameUniqueness(hashMap);
        }
    }

    @Override
    public void validateNameUniqueness(Map<String, PipelineConfigs> groupNameMap) {

    }

    @Override
    public ConfigOrigin getOrigin() {
        return null;
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
        throw new RuntimeException("TODO: Not implemented yet");
    }

    @Override
    public PipelineConfig remove(int i) {
        throw new RuntimeException("TODO: Not implemented yet");
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


/*
    @Override
    public void clear() {
        for (PipelineConfigs part : this.parts)
        {
            part.clear();
        }
    }*/

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
                if(part.getOrigin() != null && part.getOrigin().canEdit()) {
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
        throw new RuntimeException("TODO: Not implemented yet");
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

            }
        };
    }


    @Override
    public String getGroup() {
        return this.parts.get(0).getGroup();
    }

    @Override
    public void setGroup(String group) {
        if(!group.equals(this.getGroup()))
        {
            throw bomb("Cannot change group name in configuration merged from many parts");
        }
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
        throw new RuntimeException("TODO: Not implemented yet");
    }

    @Override
    public boolean exist(int pipelineIndex) {
        return false;
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
        throw new RuntimeException("TODO: Not implemented yet");
    }




    @Override
    public boolean hasTemplate() {
        throw new RuntimeException("TODO: Not implemented yet");
    }

    @Override
    public PipelineConfigs getCopyForEditing() {
        throw new RuntimeException("TODO: Not implemented yet");
    }

    @Override
    public boolean isUserAnAdmin(CaseInsensitiveString userName, List<Role> memberRoles) {
        throw new RuntimeException("TODO: Not implemented yet");
    }


    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public List<PipelineConfig> getPipelines() {
        throw new RuntimeException("TODO: Not implemented yet");
    }

    @Override
    public void addError(String fieldName, String message) {

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
        return this.getAuthorizationPart().hasAuthorizationDefined();
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
        return this.getAuthorizationPart().hasViewPermissionDefined();
    }

    @Override
    public boolean hasOperationPermissionDefined() {
        return this.getAuthorizationPart().hasOperationPermissionDefined();
    }

    @Override
    public boolean hasOperatePermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return this.getAuthorizationPart().hasOperatePermission(username,userRoleMatcher);
    }

}
