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
import com.thoughtworks.go.server.security.providers.PluginAuthenticationProvider;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.web.FlashLoadingFilter;
import com.thoughtworks.go.server.web.i18n.LocaleResolver;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration {
    @Autowired
    private ModeAwareFilter modeAwareFilter;
    @Autowired
    private ArtifactSizeEnforcementFilter artifactSizeEnforcementFilter;
    @Autowired
    private LocaleResolver i18nlocaleResolver;
    @Autowired
    private Filter filterInvocationInterceptor;
    @Autowired
    private FlashLoadingFilter flashLoader;
    @Autowired
    private DenyGoCDAccessForArtifactsFilter denyGoCDAccessForArtifactsFilter;
    private UrlRewriteFilter urlRewriter = new UrlRewriteFilter();
    @Autowired
    private Filter basicAuthenticationAccessDenied;
    //    @Autowired
    private Filter cruiseLoginOrBasicAuthentication = new NoOpFilter();
    @Autowired
    private Filter anonymousProcessingFilter;
    @Autowired
    private Filter apiSessionFilter;
    @Autowired
    private Filter userEnabledCheckFilter;
    @Autowired
    private Filter authenticationProcessingFilter;
    @Autowired
    private RemoveAdminPermissionFilter removeAdminPermissionFilter;
    @Autowired
    private Filter agentRemotingFilterInvocationInterceptor;
    @Autowired
    private ReAuthenticationFilter reAuthenticationFilter;
    @Autowired
    private PreAuthenticatedRequestsProcessingFilter preAuthenticationFilter;
    @Autowired
    private WebBasedAuthenticationFilter webBasedAuthFilter;
    @Autowired
    private OauthAuthenticationFilter oauthProcessingFilter;
    @Autowired
    private DisallowExternalReAuthenticationFilter disallowExternalReAuthenticationFilter;

    @Bean(name = "filterChainProxy")
    public FilterChainProxy getFilterChainProxy() throws ServletException, Exception {
        List<SecurityFilterChain> listOfFilterChains = new ArrayList<>();
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/config-repository.git/**"), modeAwareFilter, apiSessionFilter, removeAdminPermissionFilter, authenticationProcessingFilter, userEnabledCheckFilter, anonymousProcessingFilter, basicAuthenticationAccessDenied, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/remoting/**"), modeAwareFilter, artifactSizeEnforcementFilter, i18nlocaleResolver, agentRemotingFilterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/agent-websocket/**"), modeAwareFilter, artifactSizeEnforcementFilter, i18nlocaleResolver, agentRemotingFilterInvocationInterceptor));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/cctray.xml"), modeAwareFilter, i18nlocaleResolver, apiSessionFilter, removeAdminPermissionFilter, oauthProcessingFilter, authenticationProcessingFilter, reAuthenticationFilter, userEnabledCheckFilter, anonymousProcessingFilter, basicAuthenticationAccessDenied, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/**"), modeAwareFilter, i18nlocaleResolver, apiSessionFilter, removeAdminPermissionFilter, oauthProcessingFilter, authenticationProcessingFilter, reAuthenticationFilter, userEnabledCheckFilter, anonymousProcessingFilter, basicAuthenticationAccessDenied, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/files/**"), modeAwareFilter, artifactSizeEnforcementFilter, i18nlocaleResolver, removeAdminPermissionFilter, oauthProcessingFilter, authenticationProcessingFilter, reAuthenticationFilter, userEnabledCheckFilter, anonymousProcessingFilter, cruiseLoginOrBasicAuthentication, filterInvocationInterceptor, flashLoader, urlRewriter));
        listOfFilterChains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/**"), modeAwareFilter, i18nlocaleResolver, disallowExternalReAuthenticationFilter, removeAdminPermissionFilter, oauthProcessingFilter, webBasedAuthFilter, preAuthenticationFilter, authenticationProcessingFilter, reAuthenticationFilter, userEnabledCheckFilter, anonymousProcessingFilter, cruiseLoginOrBasicAuthentication, denyGoCDAccessForArtifactsFilter, filterInvocationInterceptor, flashLoader, urlRewriter));
        return new FilterChainProxy(listOfFilterChains);
    }


    @Configuration
    @Order(1)
    public static class WebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        private static final String SUPERVISOR = "SUPERVISOR";
        private static final String GROUP_SUPERVISOR = "GROUP_SUPERVISOR";
        private static final String TEMPLATE_VIEW_USER = "TEMPLATE_VIEW_USER";
        private static final String TEMPLATE_SUPERVISOR = "TEMPLATE_SUPERVISOR";
        private static final String USER = "USER";
        private static final String OAUTH_USER = "OAUTH_USER";
        @Autowired
        private PluginAuthenticationProvider pluginAuthenticationProvider;

        @Autowired
        private UserService userService;

        protected void configure(HttpSecurity http) throws Exception {
            configureBasicAuth(http);
            configureFormLogin(http);
            configureLogout(http);
            configureSession(http);
            configureAuthority(http);
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.authenticationProvider(pluginAuthenticationProvider);
        }

        private void configureAuthority(HttpSecurity http) throws Exception {
            http
                    .securityContext()
                    .securityContextRepository(new CustomHttpSessionSecurityContextRepository(userService));
            http
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .antMatchers("/auth/login").anonymous()
                    .antMatchers("/auth/logout").anonymous()
                    .antMatchers("/auth/security_check").anonymous()
                    .antMatchers("/compressed/*").anonymous()
                    .antMatchers("/assets/**").anonymous()
                    .antMatchers("/api/webhooks/github/notify/**").anonymous()
                    .antMatchers("/api/webhooks/gitlab/notify/**").anonymous()
                    .antMatchers("/api/webhooks/bitbucket/notify/**").anonymous()
                    .antMatchers("/api/v1/health/**").anonymous()
                    .antMatchers("/images/cruise.ico").anonymous()
                    .antMatchers("/admin/agent").anonymous()
                    .antMatchers("/admin/agent/token").anonymous()
                    .antMatchers("/admin/latest-agent.status").anonymous()
                    .antMatchers("/admin/agent-launcher.jar").anonymous()
                    .antMatchers("/admin/tfs-impl.jar").anonymous()
                    .antMatchers("/admin/agent-plugins.zip").anonymous()
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
                    .antMatchers("/api/version").anonymous()
                    .antMatchers("/api/plugin_images/**").anonymous()
                    .antMatchers("/api/*/*.xml").hasAnyAuthority(USER)
                    .antMatchers("/api/pipelines/*/*.xml").hasAnyAuthority(USER, OAUTH_USER)
                    .antMatchers("/api/agents/**").hasAnyAuthority(USER)
                    .antMatchers("/api/users/**").hasAnyAuthority(USER)
                    .antMatchers("/api/version_infos/**").hasAnyAuthority(USER)
                    .antMatchers("/*/environments/*").hasAnyAuthority(SUPERVISOR)

                    .antMatchers("/oauth/admin/**").hasAnyAuthority(SUPERVISOR)

                    .antMatchers("/oauth/token").anonymous()
                    .antMatchers("/oauth/authorize").hasAnyAuthority(USER)
                    .antMatchers("/oauth/user_tokens").hasAnyAuthority(USER)
                    .antMatchers("/oauth/user_tokens/revoke/**").hasAnyAuthority(USER)

                    .antMatchers("/plugin/interact/**").anonymous()

                    .antMatchers("/agents").hasAnyAuthority(USER)
                    .antMatchers("/dashboard").hasAnyAuthority(USER)
                    .antMatchers("/agents/*/job_run_history*").hasAnyAuthority(SUPERVISOR)
                    .antMatchers("/agents/*/job_run_history/*").hasAnyAuthority(SUPERVISOR)
                    .antMatchers("/config_view/templates/*").hasAnyAuthority(USER)
                    .antMatchers("/add-on/*/admin/**").hasAnyAuthority(SUPERVISOR)
                    .antMatchers("/add-on/*/api/**").hasAnyAuthority(OAUTH_USER)
                    .antMatchers("/**").hasAnyAuthority(USER);
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
                    .loginPage("/auth/login")
                    .defaultSuccessUrl("/")
                    .failureUrl("/auth/login?login_error=1")
                    .loginProcessingUrl("/auth/security_check")
                    .permitAll();
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
                    .changeSessionId()
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

                Object principal = securityContext.getAuthentication().getPrincipal();
                if (principal == null) {
                    return null;
                }

                Assert.isInstanceOf(UserDetails.class, principal);

                if (userEnabled(securityContext, requestResponseHolder, (UserDetails) principal)) {
                    return securityContext;
                } else {
                    return null;
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

    @Configuration
    @Order(2)
    public static class AgentSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {


        @Autowired
        @Qualifier("userCache")
        private Cache userCache;

        protected void configure(HttpSecurity http) throws Exception {

            http
                    .authorizeRequests()
                    .antMatchers("/remoting/remoteBuildRepository").hasAnyAuthority("AGENT")
                    .antMatchers("/remoting/files/**").hasAnyAuthority("AGENT")
                    .antMatchers("/remoting/properties/**").hasAnyAuthority("AGENT")
                    .antMatchers("/agent-websocket/**").hasAnyAuthority("AGENT")
                    .anyRequest()
                    .authenticated()
                    .and()
                    .x509()
                    .x509AuthenticationFilter(getX509AuthenticationFilter())
                    .userDetailsService(new X509UserDetailService())
            ;
        }

        private X509AuthenticationFilter getX509AuthenticationFilter() {
            X509AuthenticationFilter x509AuthenticationFilter = new X509AuthenticationFilter();
            x509AuthenticationFilter.setPrincipalExtractor(new CachingSubjectDnX509PrincipalExtractor(userCache));
            return x509AuthenticationFilter;
        }

        private static class CachingSubjectDnX509PrincipalExtractor implements X509PrincipalExtractor {
            private final SubjectDnX509PrincipalExtractor delegate = new SubjectDnX509PrincipalExtractor();
            private static final Logger LOGGER = LoggerFactory.getLogger(CachingSubjectDnX509PrincipalExtractor.class);

            private final Cache cache;

            public CachingSubjectDnX509PrincipalExtractor(Cache cache) {
                this.cache = cache;
            }

            @Override
            public Object extractPrincipal(X509Certificate cert) {
                Element element = null;

                try {
                    element = cache.get(cert);
                } catch (CacheException cacheException) {
                    throw new DataRetrievalFailureException("Cache failure: " + cacheException.getMessage());
                }

                if (LOGGER.isDebugEnabled()) {
                    String subjectDN = "unknown";

                    if ((cert != null) && (cert.getSubjectDN() != null)) {
                        subjectDN = cert.getSubjectDN().toString();
                    }

                    LOGGER.debug("X.509 Cache hit. SubjectDN: {}", subjectDN);
                }

                if (element == null) {
                    element = new Element(cert, delegate.extractPrincipal(cert));
                    cache.put(element);
                }

                return element.getValue();
            }


        }

        private static class X509UserDetailService implements org.springframework.security.core.userdetails.UserDetailsService {

            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                return new User("_go_agent_" + username, "", Collections.singletonList(GoAuthority.ROLE_AGENT.asAuthority()));
            }
        }
    }

    private static class NoOpFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {

        }
    }
}
