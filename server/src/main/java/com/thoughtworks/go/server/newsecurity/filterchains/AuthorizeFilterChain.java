/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.server.newsecurity.filters.AllowAllAccessFilter;
import com.thoughtworks.go.server.newsecurity.filters.DenyAllAccessFilter;
import com.thoughtworks.go.server.newsecurity.filters.UserEnabledCheckFilter;
import com.thoughtworks.go.server.newsecurity.handlers.BasicAuthenticationWithChallengeFailureResponseHandler;
import com.thoughtworks.go.server.newsecurity.handlers.GenericAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import java.util.List;

import static com.thoughtworks.go.server.security.GoAuthority.*;

@Component("authorizeFilterChain")
public class AuthorizeFilterChain extends FilterChainProxy {

    @Autowired
    public AuthorizeFilterChain(UserEnabledCheckFilter userEnabledCheckFilter,
                                AllowAllAccessFilter allowAllAccessFilter,
                                BasicAuthenticationWithChallengeFailureResponseHandler apiAccessDeniedHandler,
                                GenericAccessDeniedHandler genericAccessDeniedHandler) {
        super(FilterChainBuilder.newInstance()
                .addFilterChain("/remoting/**", addAuthorityFilterChainForAgents(apiAccessDeniedHandler))
                .addFilterChain("/**", userEnabledCheckFilter, addAuthorityFilterChain(allowAllAccessFilter, apiAccessDeniedHandler, genericAccessDeniedHandler))
                .build());
    }

    private static Filter addAuthorityFilterChainForAgents(BasicAuthenticationWithChallengeFailureResponseHandler apiFailureHandler) {
        final List<SecurityFilterChain> filterChain = FilterChainBuilder.newInstance()
                // agent remoting
                .addAuthorityFilterChain("/remoting/remoteBuildRepository", apiFailureHandler, ROLE_AGENT)
                .addAuthorityFilterChain("/remoting/files/**", apiFailureHandler, ROLE_AGENT)
                .addAuthorityFilterChain("/remoting/properties/**", apiFailureHandler, ROLE_AGENT)
                .addFilterChain("/remoting/**", new DenyAllAccessFilter())
                .addAuthorityFilterChain("/agent-websocket/**", apiFailureHandler, ROLE_AGENT)
                .build();

        return new FilterChainProxy(filterChain);
    }

    private static Filter addAuthorityFilterChain(AllowAllAccessFilter allowAllAccessFilter,
                                                  BasicAuthenticationWithChallengeFailureResponseHandler apiFailureHandler,
                                                  GenericAccessDeniedHandler genericAccessDeniedHandler) {
        final List<SecurityFilterChain> filterChain = FilterChainBuilder.newInstance()
                // allow all access
                .addFilterChain("/auth/login", allowAllAccessFilter)
                .addFilterChain("/auth/logout", allowAllAccessFilter)
                .addFilterChain("/auth/security_check", allowAllAccessFilter)
                .addFilterChain("/compressed/*", allowAllAccessFilter)
                .addFilterChain("/assets/**", allowAllAccessFilter)
                .addFilterChain("/api/webhooks/github/notify/**", allowAllAccessFilter)
                .addFilterChain("/api/webhooks/gitlab/notify/**", allowAllAccessFilter)
                .addFilterChain("/api/webhooks/bitbucket/notify/**", allowAllAccessFilter)
                .addFilterChain("/api/v1/health/**", allowAllAccessFilter)
                .addFilterChain("/images/cruise.ico", allowAllAccessFilter)
                .addFilterChain("/admin/agent", allowAllAccessFilter)
                .addFilterChain("/admin/agent/token", allowAllAccessFilter)
                .addFilterChain("/admin/latest-agent.status", allowAllAccessFilter)
                .addFilterChain("/admin/agent-launcher.jar", allowAllAccessFilter)
                .addFilterChain("/admin/tfs-impl.jar", allowAllAccessFilter)
                .addFilterChain("/admin/agent-plugins.zip", allowAllAccessFilter)
                .addFilterChain("/api/version", allowAllAccessFilter)
                .addFilterChain("/api/plugin_images/**", allowAllAccessFilter)
                .addFilterChain("/plugin/*/login", allowAllAccessFilter)
                .addFilterChain("/plugin/*/authenticate", allowAllAccessFilter)

                // rest of the urls
                .addAuthorityFilterChain("/admin/configuration/file/**", genericAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/admin/configuration/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/restful/configuration/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/pipelines/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/pipeline_group/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/templates/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_TEMPLATE_SUPERVISOR, ROLE_TEMPLATE_VIEW_USER, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/commands/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR, ROLE_TEMPLATE_SUPERVISOR)
                .addAuthorityFilterChain("/admin/plugins", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/pipeline/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/materials/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/package_repositories/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/package_definitions/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/elastic_profiles/**", genericAccessDeniedHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/admin/agents", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/admin/**", genericAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/security/**", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/internal/material_test", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/internal/pipelines", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/internal/resources", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/internal/environments", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/internal/repository_check_connection", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/internal/package_check_connection", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/pipelines", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/pipelines/*", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/encrypt", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR, ROLE_TEMPLATE_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/scms/**", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/repositories/**", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/packages/**", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/plugin_info/**", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/plugin_settings/**", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/agents", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/config_repos", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/elastic/profiles/**", apiFailureHandler, ROLE_SUPERVISOR, ROLE_GROUP_SUPERVISOR)
                .addAuthorityFilterChain("/api/admin/templates/**", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/**", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/config-repository.git/**", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/jobs/scheduled.xml", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/feeds/**", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/support", apiFailureHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/pipelines.xml", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/*/*.xml", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/pipelines/*/*.xml", apiFailureHandler, ROLE_USER, ROLE_OAUTH_USER)
                .addAuthorityFilterChain("/api/agents/**", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/users/**", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/version_infos/**", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/cctray.xml", apiFailureHandler, ROLE_USER)
                .addAuthorityFilterChain("/*/environments/*", genericAccessDeniedHandler, ROLE_SUPERVISOR)

                //OAuth
                .addFilterChain("/oauth/token", allowAllAccessFilter)
                .addAuthorityFilterChain("/oauth/admin/**", genericAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/oauth/authorize", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/oauth/user_tokens", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/oauth/user_tokens/revoke/**", genericAccessDeniedHandler, ROLE_USER)

                .addAuthorityFilterChain("/agents", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/dashboard", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/agents/*/job_run_history*", genericAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/agents/*/job_run_history/*", genericAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/config_view/templates/*", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/add-on/*/admin/**", genericAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/add-on/*/api/**", genericAccessDeniedHandler, ROLE_OAUTH_USER)
                .addAuthorityFilterChain("/**", genericAccessDeniedHandler, ROLE_USER)
                .build();

        return new FilterChainProxy(filterChain);
    }

}