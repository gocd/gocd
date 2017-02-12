/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.server.oauth.OauthDataSource;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.OauthAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.jaas.AuthorityGranter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OauthAuthenticationProviderTest {
    private OauthDataSource dataSource;
    private AuthorityGranter granter;

    private OauthAuthenticationProvider provider;

    @Before
    public void setUp() throws Exception {
        dataSource = mock(OauthDataSource.class);
        granter = mock(AuthorityGranter.class);
        provider = new OauthAuthenticationProvider(dataSource);
    }

    @Test
    public void shouldReturnOAUTH_USERAsTheGrantedAuthority() {
        when(dataSource.findOauthTokenByAccessToken("token-string")).thenReturn(oauthTokenDto("user-id"));
        List<GrantedAuthority> grantedAuthorities = Collections.singletonList(GoAuthority.ROLE_OAUTH_USER.asAuthority());

        OauthAuthenticationToken authentication = provider.authenticate(new OauthAuthenticationToken("token-string"));
        assertThat(authentication.isAuthenticated(), is(true));
        UserDetails userDetails = authentication.getPrincipal();
        assertThat(userDetails.getUsername(), is("user-id"));
        assertThat(new ArrayList<>(userDetails.getAuthorities()), hasItem(GoAuthority.ROLE_OAUTH_USER.asAuthority()));
        assertThat(authentication.getAuthorities(), is(grantedAuthorities));
    }

    @Test
    public void shouldRaiseAuthenticationExceptionWhenNoMatchForTokenExists() {
        when(dataSource.findOauthTokenByAccessToken("token-string")).thenReturn(null);

        try {
            provider.authenticate(new OauthAuthenticationToken("token-string"));
            fail("should have thrown an AuthenticationException");
        } catch (AuthenticationException e) {
            assertThat(e.getMessage(), is("No match for OAuth token: token-string"));
        }
    }

    private OauthDataSource.OauthTokenDTO oauthTokenDto(String userId) {
        OauthDataSource.OauthTokenDTO dto = new OauthDataSource.OauthTokenDTO();
        dto.setUserId(userId);
        return dto;
    }
}
