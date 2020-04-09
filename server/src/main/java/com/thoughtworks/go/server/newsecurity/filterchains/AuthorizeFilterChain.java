/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.newsecurity.handlers.BasicAuthenticationWithChallengeFailureResponseHandler;
import com.thoughtworks.go.server.newsecurity.handlers.GenericAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.server.security.GoAuthority.*;

@Component("authorizeFilterChain")
public class AuthorizeFilterChain extends FilterChainProxy {

    @Autowired
    public AuthorizeFilterChain(AllowAllAccessFilter allowAllAccessFilter,
                                BasicAuthenticationWithChallengeFailureResponseHandler apiAccessDeniedHandler,
                                GenericAccessDeniedHandler genericAccessDeniedHandler) {
        super(FilterChainBuilder.newInstance()
                // agent access
                .addAuthorityFilterChain("/remoting/remoteBuildRepository", apiAccessDeniedHandler, ROLE_AGENT)
                .addAuthorityFilterChain("/remoting/files/**", apiAccessDeniedHandler, ROLE_AGENT)
                .addAuthorityFilterChain("/remoting/properties/**", apiAccessDeniedHandler, ROLE_AGENT)
                .addFilterChain("/remoting/**", new DenyAllAccessFilter())

                // authentication urls, allow everyone
                .addFilterChain("/auth/*", allowAllAccessFilter)
                .addFilterChain("/plugin/*/login", allowAllAccessFilter)
                .addFilterChain("/plugin/*/authenticate", allowAllAccessFilter)

                .addFilterChain("/assets/**", allowAllAccessFilter)

                // this is under the `/admin` namespace, but is used by the agent to download various jars
                .addFilterChain("/admin/agent", allowAllAccessFilter)
                .addFilterChain("/admin/agent/token", allowAllAccessFilter)
                .addFilterChain("/admin/latest-agent.status", allowAllAccessFilter)
                .addFilterChain("/admin/agent-launcher.jar", allowAllAccessFilter)
                .addFilterChain("/admin/tfs-impl.jar", allowAllAccessFilter)
                .addFilterChain("/admin/agent-plugins.zip", allowAllAccessFilter)

                // some publicly available APIs
                .addFilterChain("/api/version", allowAllAccessFilter)
                .addFilterChain("/api/plugin_images/**", allowAllAccessFilter)
                .addFilterChain("/api/v1/health/**", allowAllAccessFilter)
                .addFilterChain("/api/webhooks/*/notify/**", allowAllAccessFilter)

                // for some kind of admins
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

                .addAuthorityFilterChain("/admin/environments/**", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/admin/scms/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/admin/config_repos/**", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/admin/elastic_agents/**", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/admin/admin/elastic_profiles/**", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/admin/elastic_agent_configurations/**", genericAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/admin/status_reports/**", genericAccessDeniedHandler, ROLE_USER)

                .addAuthorityFilterChain("/admin/**", genericAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/agents/*/job_run_history/**", genericAccessDeniedHandler, ROLE_SUPERVISOR)

                // all apis
                .addAuthorityFilterChain("/cctray.xml", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/pipelines.json", apiAccessDeniedHandler, ROLE_USER)

                // new controllers, so we say `ROLE_USER`, and let the controller handle authorization
                .addAuthorityFilterChain("/api/admin/internal/*", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/pipelines/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/pipeline_groups/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/export/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/encrypt", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/scms/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/repositories/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/packages/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/plugin_info/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/templates/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/elastic/profiles/**", apiAccessDeniedHandler, ROLE_USER)

                .addAuthorityFilterChain("/api/admin/environments/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/config_repos/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/internal/environments/merged", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/internal/environments/**", apiAccessDeniedHandler, ROLE_USER)
                .addAuthorityFilterChain("/api/admin/elastic/cluster_profiles/**", apiAccessDeniedHandler, ROLE_USER)

                // blanket role that requires supervisor access, used by old admin apis
                .addAuthorityFilterChain("/api/admin/**", apiAccessDeniedHandler, ROLE_SUPERVISOR)

                .addAuthorityFilterChain("/api/config-repository.git/**", apiAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/jobs/scheduled.xml", apiAccessDeniedHandler, ROLE_SUPERVISOR)
                .addAuthorityFilterChain("/api/support", apiAccessDeniedHandler, ROLE_SUPERVISOR)

                // any other APIs require `ROLE_USER`
                .addAuthorityFilterChain("/api/**", apiAccessDeniedHandler, ROLE_USER)

                // addons will be expected to do their own authentication/authorization
                .addFilterChain("/add-on/**", allowAllAccessFilter)

                .addAuthorityFilterChain("/**", genericAccessDeniedHandler, ROLE_USER)
                .build());
    }

}
