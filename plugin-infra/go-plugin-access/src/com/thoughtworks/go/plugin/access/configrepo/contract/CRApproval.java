package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Collection;

public class CRApproval {
    private final CRApprovalCondition type;
    private final Collection<String> authorizedUsers;
    private final Collection<String> authorizedRoles;

    public CRApproval(CRApprovalCondition type, Collection<String> authorizedRoles, Collection<String> authorizedUsers) {
        this.type = type;
        this.authorizedUsers = authorizedUsers;
        this.authorizedRoles = authorizedRoles;
    }


    public CRApprovalCondition getType() {
        return type;
    }

    public Collection<String> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public Collection<String> getAuthorizedRoles() {
        return authorizedRoles;
    }
}
