/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.SecurityConfigChangeListener;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.AuthorizationExtensionCacheService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.util.ClonerFactory;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

import static com.thoughtworks.go.server.newsecurity.filters.InvalidateAuthenticationOnSecurityConfigChangeFilter.SECURITY_CONFIG_LAST_CHANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class InvalidateAuthenticationOnSecurityConfigChangeFilterTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PluginRoleService pluginRoleService;
    @Mock
    private AuthorizationExtensionCacheService cacheService;

    @Captor
    private ArgumentCaptor<ConfigChangedListener> configChangedListenerArgumentCaptor;

    private InvalidateAuthenticationOnSecurityConfigChangeFilter filter;
    private TestingClock clock;
    private MockHttpServletResponse response;
    private MockHttpServletRequest request;
    private FilterChain filterChain;
    private SystemEnvironment systemEnvironment;
    private BasicCruiseConfig cruiseConfig;


    @BeforeEach
    void setUp() {
        initMocks(this);
        //request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        clock = new TestingClock();
        systemEnvironment = new SystemEnvironment();
        filterChain = mock(FilterChain.class);

        cruiseConfig = new BasicCruiseConfig();
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);

        filter = new InvalidateAuthenticationOnSecurityConfigChangeFilter(goConfigService, clock, cacheService, pluginRoleService);
        filter.initialize();
        filter.onPluginRoleChange();
        filter.onConfigChange(ClonerFactory.instance().deepClone(cruiseConfig));
        reset(cacheService);
    }

    @Test
    void shouldInitializeRemoveAdminPermissionFilterWithSListeners() {
        verify(goConfigService, times(2)).register(configChangedListenerArgumentCaptor.capture());

        List<ConfigChangedListener> registeredListeners = configChangedListenerArgumentCaptor.getAllValues();

        assertThat(registeredListeners.get(0)).isInstanceOf(InvalidateAuthenticationOnSecurityConfigChangeFilter.class);
        assertThat(registeredListeners.get(0)).isSameAs(filter);
        assertThat(registeredListeners.get(1)).isInstanceOf(SecurityConfigChangeListener.class);
    }

    @Test
    void shouldContinueWithTheChainIfTheSecurityConfigHasNotChanged() throws IOException, ServletException {
        request = HttpRequestBuilder.GET("/")
                .withRequestedSessionIdFromSession()
                .build();

        final AuthenticationToken<UsernamePassword> authenticationToken = setupAuthentication();
        SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
        final HttpSession originalSession = request.getSession(false);

        assertThat(SessionUtils.getAuthenticationToken(request).isAuthenticated(clock, systemEnvironment)).isTrue();
        filter.doFilter(request, response, filterChain);
        assertThat(request.getSession(false).getAttribute(SECURITY_CONFIG_LAST_CHANGE)).isEqualTo(clock.currentTimeMillis());

        long timeBeforeConfigChange = clock.currentTimeMillis();

        clock.addSeconds(1);
        cruiseConfig.addEnvironment("Foo");
        filter.onConfigChange(ClonerFactory.instance().deepClone(cruiseConfig));

        response.reset();
        filter.doFilter(request, response, filterChain);

        assertThat(SessionUtils.getAuthenticationToken(request).isAuthenticated(clock, systemEnvironment)).isTrue();
        assertThat(request.getSession(false)).isSameAs(originalSession);
        assertThat(request.getSession(false).getAttribute(SECURITY_CONFIG_LAST_CHANGE)).isEqualTo(timeBeforeConfigChange);
        verifyZeroInteractions(cacheService);
    }

    @Test
    void shouldInvalidateAuthenticationTokenIfTheSecurityConfigHasChanged() throws IOException, ServletException {
        request = HttpRequestBuilder.GET("/")
                .withRequestedSessionIdFromSession()
                .build();

        final AuthenticationToken<UsernamePassword> authenticationToken = setupAuthentication();
        SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
        final HttpSession originalSession = request.getSession(false);

        assertThat(SessionUtils.getAuthenticationToken(request).isAuthenticated(clock, systemEnvironment)).isTrue();
        filter.doFilter(request, response, filterChain);

        clock.addSeconds(1);
        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, "bob");
        filter.onConfigChange(ClonerFactory.instance().deepClone(cruiseConfig));

        response.reset();
        filter.doFilter(request, response, filterChain);

        assertThat(SessionUtils.getAuthenticationToken(request).isAuthenticated(clock, systemEnvironment)).isFalse();
        assertThat(request.getSession(false)).isSameAs(originalSession);
        verify(cacheService, times(1)).invalidateCache();
    }

    @Test
    void shouldInvalidateAuthenticationTokenIfRoleConfigHasChanged() throws IOException, ServletException {
        request = HttpRequestBuilder.GET("/")
                .withRequestedSessionIdFromSession()
                .build();

        final AuthenticationToken<UsernamePassword> authenticationToken = setupAuthentication();
        SessionUtils.setAuthenticationTokenAfterRecreatingSession(authenticationToken, request);
        final HttpSession originalSession = request.getSession(false);

        assertThat(SessionUtils.getAuthenticationToken(request).isAuthenticated(clock, systemEnvironment)).isTrue();
        filter.doFilter(request, response, filterChain);

        clock.addSeconds(1);
        filter.onPluginRoleChange();

        response.reset();
        filter.doFilter(request, response, filterChain);

        assertThat(SessionUtils.getAuthenticationToken(request).isAuthenticated(clock, systemEnvironment)).isFalse();
        assertThat(request.getSession(false)).isSameAs(originalSession);
        assertThat(request.getSession(false).getAttribute(SECURITY_CONFIG_LAST_CHANGE)).isEqualTo(clock.currentTimeMillis());
        verify(cacheService, times(1)).invalidateCache();
    }

    @Test
    void shouldContinueFilterChainWhenRequestDoesNotHaveAuthenticationToken() throws ServletException, IOException {
        request = HttpRequestBuilder.GET("/")
                .withRequestedSessionIdFromSession()
                .build();

        final HttpSession originalSession = request.getSession(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SessionUtils.getAuthenticationToken(request)).isNull();
        assertThat(originalSession).isSameAs(request.getSession(false));
        verify(filterChain).doFilter(request, response);
    }

    private AuthenticationToken<UsernamePassword> setupAuthentication(GrantedAuthority... grantedAuthorities) {
        final GoUserPrinciple goUserPrinciple = new GoUserPrinciple("bob", "Bob", grantedAuthorities);
        return new AuthenticationToken<>(goUserPrinciple,
                new UsernamePassword("bob", "p@ssw0rd"),
                null,
                clock.currentTimeMillis(),
                null);
    }
}
