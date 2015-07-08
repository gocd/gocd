package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;

public class CRApproval_1 extends CRBase {
    private String type;
    private Collection<String> authorizedUsers = new ArrayList<>();
    private Collection<String> authorizedRoles = new ArrayList<>();

    public CRApproval_1()
    {
    }
    public CRApproval_1(String type)
    {
        this.type = type;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRApproval_1 that = (CRApproval_1)o;
        if(that == null)
            return  false;

        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

        if (authorizedUsers != null ? !CollectionUtils.isEqualCollection(this.authorizedUsers, that.authorizedUsers) : that.authorizedUsers != null) {
            return false;
        }

        if (authorizedRoles != null ? !CollectionUtils.isEqualCollection(this.authorizedRoles, that.authorizedRoles) : that.authorizedRoles != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (authorizedUsers != null ? authorizedUsers.size() : 0);
        result = 31 * result + (authorizedRoles != null ? authorizedRoles.size() : 0);
        return result;
    }

    @Override
    public void getErrors(ErrorCollection errors) {
        validateType(errors);
    }

    private void validateType(ErrorCollection errors) {
        if(!StringUtil.isBlank(type))
        {
            if(!(type.equals("success") || type.equals("manual")))
                errors.add(this,"type in approval must be 'manual' or 'success'");
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Collection<String> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public void setAuthorizedUsers(Collection<String> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }

    public Collection<String> getAuthorizedRoles() {
        return authorizedRoles;
    }

    public void setAuthorizedRoles(Collection<String> authorizedRoles) {
        this.authorizedRoles = authorizedRoles;
    }

    public void addAuthorizedUser(String user) {
        this.authorizedUsers.add(user);
    }

    public void addAuthorizedRole(String role) {
        this.authorizedRoles.add(role);
    }
}
