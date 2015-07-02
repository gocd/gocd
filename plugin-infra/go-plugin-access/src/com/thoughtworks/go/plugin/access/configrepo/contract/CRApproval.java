package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Collection;

public class CRApproval {
    private CRApprovalCondition type;
    private Collection<String> authorizedUsers;
    private Collection<String> authorizedRoles;
}
