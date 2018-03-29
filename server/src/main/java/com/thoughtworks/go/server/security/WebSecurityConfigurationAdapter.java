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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.security.providers.OauthAuthenticationProvider;
import com.thoughtworks.go.server.security.providers.PluginAuthenticationProvider;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.web.ApiSessionReduceIdleTimeoutFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;

@Configuration
@Order(2)
public class WebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
    private static final String SUPERVISOR = GoAuthority.ROLE_SUPERVISOR.name();
    private static final String GROUP_SUPERVISOR = GoAuthority.ROLE_GROUP_SUPERVISOR.name();
    private static final String TEMPLATE_VIEW_USER = GoAuthority.ROLE_TEMPLATE_VIEW_USER.name();
    private static final String TEMPLATE_SUPERVISOR = GoAuthority.ROLE_TEMPLATE_SUPERVISOR.name();
    private static final String USER = GoAuthority.ROLE_USER.name();
    private static final String OAUTH_USER = GoAuthority.ROLE_OAUTH_USER.name();

    @Autowired
    private PluginAuthenticationProvider pluginAuthenticationProvider;
    @Autowired
    private OauthAuthenticationProvider oauthAuthenticationProvider;
    @Autowired
    private WebBasedPluginAuthenticationProcessingFilter webBasedPluginAuthenticationProcessingFilter;
    @Autowired
    private WebBasedThirdPartyRedirectFilter webBasedThirdPartyRedirectFilter;
    @Autowired
    private ApiSessionReduceIdleTimeoutFilter apiSessionReduceIdleTimeoutFilter;
    @Autowired
    private AnonymousProcessingFilter anonymousProcessingFilter;
    @Autowired
    private RemoveAdminPermissionFilter removeAdminPermissionFilter;

    @Autowired
    @Qualifier("filterChainProxy")
    private FilterChainProxy filterChainProxy;

    @Autowired
    private UserService userService;

    protected void configure(HttpSecurity http) throws Exception {
        http.anonymous()
                .authenticationFilter(anonymousProcessingFilter);

        disableCsrf(http);
        configureAuthority(http);
        configureFormLogin(http);
        configureBasicAuth(http);
        configureLogout(http);
        configureSession(http);

        http.addFilterAfter(filterChainProxy, SwitchUserFilter.class);
        http.addFilterBefore(apiSessionReduceIdleTimeoutFilter, SecurityContextPersistenceFilter.class);
        http.addFilterAfter(removeAdminPermissionFilter, SecurityContextPersistenceFilter.class);
        http.addFilterBefore(webBasedThirdPartyRedirectFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(webBasedPluginAuthenticationProcessingFilter, WebBasedThirdPartyRedirectFilter.class);
    }

    private void disableCsrf(HttpSecurity http) throws Exception {
        http.csrf().disable();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth
                .authenticationProvider(oauthAuthenticationProvider)
                .authenticationProvider(pluginAuthenticationProvider)
                .authenticationProvider(new AnonymousAuthenticationProvider("anonymousKey"))
        ;
    }

    private void configureAuthority(HttpSecurity http) throws Exception {
        http
                .securityContext()
                .securityContextRepository(new CustomHttpSessionSecurityContextRepository(userService));

        http
                .authorizeRequests()
                .antMatchers("/auth/login").permitAll()
                .antMatchers("/auth/logout").permitAll()
                .antMatchers("/auth/security_check").permitAll()
                .antMatchers("/compressed/*").permitAll()
                .antMatchers("/assets/**").permitAll()
                .antMatchers("/api/webhooks/github/notify/**").permitAll()
                .antMatchers("/api/webhooks/gitlab/notify/**").permitAll()
                .antMatchers("/api/webhooks/bitbucket/notify/**").permitAll()
                .antMatchers("/api/v1/health/**").permitAll()
                .antMatchers("/images/cruise.ico").permitAll()
                .antMatchers("/admin/agent").permitAll()
                .antMatchers("/admin/agent/token").permitAll()
                .antMatchers("/admin/latest-agent.status").permitAll()
                .antMatchers("/admin/agent-launcher.jar").permitAll()
                .antMatchers("/admin/tfs-impl.jar").permitAll()
                .antMatchers("/admin/agent-plugins.zip").permitAll()
                .antMatchers("/admin/configuration/file/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/admin/configuration/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/restful/configuration/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/pipelines/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/pipeline_group/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/templates/**").hasAnyAuthority(SUPERVISOR, TEMPLATE_SUPERVISOR, TEMPLATE_VIEW_USER, GROUP_SUPERVISOR)
                .antMatchers("/admin/commands/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR, TEMPLATE_SUPERVISOR)
                .antMatchers("/api/admin/security/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/admin/plugins").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/pipeline/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/materials/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/package_repositories/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/package_definitions/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/internal/material_test").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/internal/pipelines").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/internal/resources").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/internal/environments").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/admin/internal/repository_check_connection").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/internal/package_check_connection").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/pipelines").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/pipelines/*").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/encrypt").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR, TEMPLATE_SUPERVISOR)
                .antMatchers("/api/admin/scms/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/repositories/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/packages/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/plugin_info/**").hasAnyAuthority(USER)
                .antMatchers("/api/admin/plugin_settings/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/admin/agents").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/admin/config_repos").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/elastic/profiles/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/admin/elastic_profiles/**").hasAnyAuthority(SUPERVISOR, GROUP_SUPERVISOR)
                .antMatchers("/api/admin/templates/**").hasAnyAuthority(USER)
                .antMatchers("/api/admin/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/config-repository.git/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/jobs/scheduled.xml").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/admin/agents").hasAnyAuthority(USER)
                .antMatchers("/admin/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/feeds/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/support").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/api/pipelines.xml").hasAnyAuthority(USER)
                .antMatchers("/api/version").permitAll()
                .antMatchers("/api/plugin_images/**").permitAll()
                .antMatchers("/api/*/*.xml").hasAnyAuthority(USER)
                .antMatchers("/api/pipelines/*/*.xml").hasAnyAuthority(USER, OAUTH_USER)
                .antMatchers("/api/agents/**").hasAnyAuthority(USER)
                .antMatchers("/api/users/**").hasAnyAuthority(USER)
                .antMatchers("/api/version_infos/**").hasAnyAuthority(USER)
                .antMatchers("/*/environments/*").hasAnyAuthority(SUPERVISOR)

                .antMatchers("/oauth/admin/**").hasAnyAuthority(SUPERVISOR)

                .antMatchers("/oauth/token").permitAll()
                .antMatchers("/oauth/authorize").hasAnyAuthority(USER)
                .antMatchers("/oauth/user_tokens").hasAnyAuthority(USER)
                .antMatchers("/oauth/user_tokens/revoke/**").hasAnyAuthority(USER)

                .antMatchers("/plugin/interact/**").permitAll()

                .antMatchers("/agents").hasAnyAuthority(USER)
                .antMatchers("/dashboard").hasAnyAuthority(USER)
                .antMatchers("/agents/*/job_run_history*").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/agents/*/job_run_history/*").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/config_view/templates/*").hasAnyAuthority(USER)
                .antMatchers("/add-on/*/admin/**").hasAnyAuthority(SUPERVISOR)
                .antMatchers("/add-on/*/api/**").hasAnyAuthority(OAUTH_USER)
                .antMatchers("/**").hasAnyAuthority(USER)
                .anyRequest().authenticated();
    }

    private void configureLogout(HttpSecurity http) throws Exception {
        http
                .logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login")
                .permitAll()
        ;
    }

    private void configureFormLogin(HttpSecurity http) throws Exception {
        http
                .formLogin()
                .passwordParameter("j_password")
                .usernameParameter("j_username")
                .loginPage("/auth/login")
                .defaultSuccessUrl("/home")
                .failureUrl("/auth/login?login_error=1")
                .loginProcessingUrl("/auth/security_check")
                .permitAll()
        ;
    }

    private void configureBasicAuth(HttpSecurity http) throws Exception {
        http
                .httpBasic()
                .realmName("GoCD");
    }

    private void configureSession(HttpSecurity http) throws Exception {
        http
                .sessionManagement()
                .sessionFixation()
                .newSession()
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
    }

    private class CustomHttpSessionSecurityContextRepository extends HttpSessionSecurityContextRepository {

        private final UserService userService;

        public CustomHttpSessionSecurityContextRepository(UserService userService) {
            this.userService = userService;
        }

        @Override
        public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
            SecurityContext securityContext = super.loadContext(requestResponseHolder);

            if (securityContext == null || securityContext.getAuthentication() == null) {
                return securityContext;
            }

            Object principal = securityContext.getAuthentication().getPrincipal();
            if (principal == null) {
                return securityContext;
            }

            Assert.isInstanceOf(UserDetails.class, principal);

            if (userEnabled(securityContext, requestResponseHolder, (UserDetails) principal)) {
                return securityContext;
            } else {
                return securityContext;
            }
        }

        private boolean userEnabled(SecurityContext securityContext, HttpRequestResponseHolder requestResponseHolder, UserDetails principal) {
            HttpServletRequest request = requestResponseHolder.getRequest();
            com.thoughtworks.go.domain.User user = getUser(request);

            if (user.getId() != NOT_PERSISTED && UserHelper.getUserId(request) == null) {
                UserHelper.setUserId(request, user.getId());
            }

            if (!user.isEnabled()) {
                securityContext.setAuthentication(null);
                UserHelper.setUserId(request, null);
            }

            return user.isEnabled();
        }

        private com.thoughtworks.go.domain.User getUser(HttpServletRequest request) {
            Long userId = UserHelper.getUserId(request);
            if (userId == null) {
                Username userName = UserHelper.getUserName();

                if (userName.isAnonymous() || userName.isGoAgentUser()) {
                    return new NullUser();
                }

                return userService.findUserByName(CaseInsensitiveString.str(userName.getUsername()));
            } else {
                return userService.load(userId);
            }
        }
    }
}
