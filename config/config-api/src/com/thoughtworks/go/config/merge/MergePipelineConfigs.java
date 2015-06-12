package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PiplineConfigVisitor;

import java.util.*;

/**
 * Created by tomzo on 6/11/15.
 */
public class MergePipelineConfigs implements PipelineConfigs {

    private List<PipelineConfigs> parts = new ArrayList<PipelineConfigs>();

    public MergePipelineConfigs(PipelineConfigs... parts)
    {
        for(PipelineConfigs part : parts)
        {
            this.parts.add(part);
        }
    }

    @Override
    public ConfigOrigin getOrigin() {
        return null;
    }

    @Override
    public PipelineConfig findBy(CaseInsensitiveString pipelineName) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<PipelineConfig> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return null;
    }

    @Override
    public boolean add(PipelineConfig pipelineConfig) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends PipelineConfig> collection) {
        return false;
    }

    @Override
    public boolean addAll(int i, Collection<? extends PipelineConfig> collection) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public PipelineConfig get(int i) {
        return null;
    }

    @Override
    public boolean addWithoutValidation(PipelineConfig pipelineConfig) {
        return false;
    }

    @Override
    public PipelineConfig set(int index, PipelineConfig pipelineConfig) {
        return null;
    }

    @Override
    public void addToTop(PipelineConfig pipelineConfig) {

    }

    @Override
    public void add(int index, PipelineConfig pipelineConfig) {

    }

    @Override
    public PipelineConfig remove(int i) {
        return null;
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @Override
    public ListIterator<PipelineConfig> listIterator() {
        return null;
    }

    @Override
    public ListIterator<PipelineConfig> listIterator(int i) {
        return null;
    }

    @Override
    public List<PipelineConfig> subList(int i, int i1) {
        return null;
    }

    @Override
    public String getGroup() {
        return null;
    }

    @Override
    public void setGroup(String group) {

    }

    @Override
    public boolean isNamed(String groupName) {
        return false;
    }

    @Override
    public void update(String groupName, PipelineConfig pipeline, String pipelineName) {

    }

    @Override
    public boolean save(PipelineConfig pipeline, String groupName) {
        return false;
    }

    @Override
    public void add(List<String> allGroup) {

    }

    @Override
    public boolean exist(int pipelineIndex) {
        return false;
    }

    @Override
    public boolean hasPipeline(CaseInsensitiveString pipelineName) {
        return false;
    }

    @Override
    public Authorization getAuthorization() {
        return null;
    }

    @Override
    public void accept(PiplineConfigVisitor visitor) {

    }

    @Override
    public void setAuthorization(Authorization authorization) {

    }

    @Override
    public boolean hasViewPermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return false;
    }

    @Override
    public boolean hasViewPermissionDefined() {
        return false;
    }

    @Override
    public boolean hasOperationPermissionDefined() {
        return false;
    }

    @Override
    public boolean hasOperatePermission(CaseInsensitiveString username, UserRoleMatcher userRoleMatcher) {
        return false;
    }

    @Override
    public boolean hasAuthorizationDefined() {
        return false;
    }

    @Override
    public boolean hasTemplate() {
        return false;
    }

    @Override
    public PipelineConfigs getCopyForEditing() {
        return null;
    }

    @Override
    public boolean isUserAnAdmin(CaseInsensitiveString userName, List<Role> memberRoles) {
        return false;
    }

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public void validateNameUniqueness(Map<String, PipelineConfigs> groupNameMap) {

    }

    @Override
    public ConfigErrors errors() {
        return null;
    }

    @Override
    public List<PipelineConfig> getPipelines() {
        return null;
    }

    @Override
    public void addError(String fieldName, String message) {

    }

    @Override
    public List<AdminUser> getOperateUsers() {
        return null;
    }

    @Override
    public List<AdminRole> getOperateRoles() {
        return null;
    }

    @Override
    public List<String> getOperateRoleNames() {
        return null;
    }

    @Override
    public List<String> getOperateUserNames() {
        return null;
    }

    @Override
    public void setConfigAttributes(Object attributes) {

    }

    @Override
    public void cleanupAllUsagesOfRole(Role roleToDelete) {

    }
}
