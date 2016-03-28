package com.thoughtworks.go.server.security;

import com.thoughtworks.go.util.SystemEnvironment;

import javax.servlet.http.HttpServletRequest;

public class HeaderConstraint {
    private SystemEnvironment systemEnvironment;

    public HeaderConstraint(SystemEnvironment systemEnvironment) {

        this.systemEnvironment = systemEnvironment;
    }

    public boolean isSatisfied(HttpServletRequest request) {
        if(!systemEnvironment.isApiSafeModeEnabled()) {
            return true;
        }

        String requestHeader = request.getHeader("Confirm");
        if(requestHeader == null || !requestHeader.equalsIgnoreCase("true")) {
            return false;
        }

        return true;
    }
}
