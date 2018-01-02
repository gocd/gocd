/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.PluginRoleChangeListener;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.util.TimeProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @understands when a logged in user's authorization needs to be redone to get the new roles.
 */
public class RemoveAdminPermissionFilter extends SpringSecurityFilter implements ConfigChangedListener, PluginRoleChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveAdminPermissionFilter.class);

    protected static final String SECURITY_CONFIG_LAST_CHANGE = "security_config_last_changed_time";
    private SecurityConfig securityConfig;
    private GoConfigService goConfigService;
    private TimeProvider timeProvider;
    private PluginRoleService pluginRoleService;
    private volatile long lastChangedTime;

    public RemoveAdminPermissionFilter(GoConfigService goConfigService, TimeProvider timeProvider, PluginRoleService pluginRoleService) {
        this.goConfigService = goConfigService;
        this.timeProvider = timeProvider;
        this.pluginRoleService = pluginRoleService;
    }

    public void initialize() {
        pluginRoleService.register(this);
        goConfigService.register(this);
        goConfigService.register(new SecurityConfigChangeListener() {
            @Override
            public void onEntityConfigChange(Object entity) {
                lastChangedTime = timeProvider.currentTimeMillis();
            }
        });
    }

    public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            chain.doFilter(request, response);
            return;
        }
        synchronized (request.getRequestedSessionId().intern()) {
            long localCopyOfLastChangedTime = lastChangedTime;//This is so that the volatile variable is accessed only once.
            Long previousLastChangedTime = (Long) request.getSession().getAttribute(SECURITY_CONFIG_LAST_CHANGE);
            if (previousLastChangedTime == null) {
                request.getSession().setAttribute(SECURITY_CONFIG_LAST_CHANGE, localCopyOfLastChangedTime);
            } else if (previousLastChangedTime < localCopyOfLastChangedTime) {
                request.getSession().setAttribute(SECURITY_CONFIG_LAST_CHANGE, localCopyOfLastChangedTime);
                authentication.setAuthenticated(false);
            }
        }
        chain.doFilter(request, response);
    }

    public int getOrder() {
        return FilterChainOrder.BASIC_PROCESSING_FILTER - 1;
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        SecurityConfig newSecurityConfig = securityConfig(newCruiseConfig);
        if (this.securityConfig != null && !this.securityConfig.equals(newSecurityConfig)) {
            LOGGER.info("[Configuration Changed] Security Configuration is changed. Updating the last changed time.");
            this.lastChangedTime = timeProvider.currentTimeMillis();
        }
        this.securityConfig = newSecurityConfig;
    }

    private SecurityConfig securityConfig(CruiseConfig newCruiseConfig) {
        return newCruiseConfig.server().security();
    }

    @Override
    public void onPluginRoleChange() {
        this.lastChangedTime = timeProvider.currentTimeMillis();
    }
}
