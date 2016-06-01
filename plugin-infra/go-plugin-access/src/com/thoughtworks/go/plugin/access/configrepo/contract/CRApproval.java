package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;

public class CRApproval extends CRBase {
    private CRApprovalCondition type;
    private Collection<String> users = new ArrayList<>();
    private Collection<String> roles = new ArrayList<>();

    public CRApproval()
    {
    }
    public CRApproval(CRApprovalCondition type)
    {
        this.type = type;
    }
    public CRApproval(CRApprovalCondition type, Collection<String> authorizedRoles, Collection<String> authorizedUsers) {
        this.type = type;
        this.users = authorizedUsers;
        this.roles = authorizedRoles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRApproval that = (CRApproval)o;
        if(that == null)
            return  false;

        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

        if (users != null ? !CollectionUtils.isEqualCollection(this.users, that.users) : that.users != null) {
            return false;
        }

        if (roles != null ? !CollectionUtils.isEqualCollection(this.roles, that.roles) : that.roles != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (users != null ? users.size() : 0);
        result = 31 * result + (roles != null ? roles.size() : 0);
        return result;
    }

    public CRApprovalCondition getType()
    {
        return type;
    }

    public Collection<String> getAuthorizedUsers() {
        return users;
    }

    public void setAuthorizedUsers(Collection<String> authorizedUsers) {
        this.users = authorizedUsers;
    }

    public Collection<String> getAuthorizedRoles() {
        return roles;
    }

    public void setAuthorizedRoles(Collection<String> authorizedRoles) {
        this.roles = authorizedRoles;
    }

    public void addAuthorizedUser(String user) {
        this.users.add(user);
    }

    public void addAuthorizedRole(String role) {
        this.roles.add(role);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location =this.getLocation(parentLocation);
        errors.checkMissing(location,"type",type);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Approval",myLocation);
    }
}
