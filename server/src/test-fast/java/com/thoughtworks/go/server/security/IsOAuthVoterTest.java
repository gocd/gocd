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

import org.junit.Test;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.SecurityConfig;
import org.springframework.security.providers.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.vote.AccessDecisionVoter;
import org.springframework.security.vote.AuthenticatedVoter;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class IsOAuthVoterTest {

    @Test
    public void shouldSupportOAuthUserRole() {
        IsOAuthVoter voter = new IsOAuthVoter();
        ConfigAttribute attribute = new SecurityConfig(GoAuthority.ROLE_OAUTH_USER.name());
        assertThat(voter.supports(attribute), is(true));
    }

    @Test
    public void shouldNotSupportAUserRoleOtherThanOAuthUserRole() {
        IsOAuthVoter voter = new IsOAuthVoter();
        ConfigAttribute attribute = new SecurityConfig(GoAuthority.ROLE_GROUP_SUPERVISOR.name());
        assertThat(voter.supports(attribute), is(false));
    }

    @Test
    public void shouldDenyWhenOAuthTokenUserIsAccessingNonOAuthURLs() {
        IsOAuthVoter voter = new IsOAuthVoter();
        assertThat(voter.vote(new OauthAuthenticationToken("token"), new Object(), new ConfigAttributeDefinition(withoutOAuth())), is(AccessDecisionVoter.ACCESS_DENIED));
    }

    @Test
    public void shouldGrantWhenOAuthTokenUserIsAccessingOAuthURLs() {
        IsOAuthVoter voter = new IsOAuthVoter();
        assertThat(voter.vote(new OauthAuthenticationToken("token"), new Object(), new ConfigAttributeDefinition(withOAuth())), is(AccessDecisionVoter.ACCESS_GRANTED));
    }

    @Test
    public void shouldAbstainWhenAuthenticationIsNotOAuthToken() {
        IsOAuthVoter voter = new IsOAuthVoter();
        assertThat(voter.vote(new PreAuthenticatedAuthenticationToken(new Object(), new Object()), new Object(), new ConfigAttributeDefinition(withOAuth())), is(AccessDecisionVoter.ACCESS_ABSTAIN));
    }

    private List<ConfigAttribute> withOAuth() {
        ConfigAttribute first = new SecurityConfig(GoAuthority.ROLE_OAUTH_USER.name());
        ConfigAttribute second = new SecurityConfig(AuthenticatedVoter.IS_AUTHENTICATED_REMEMBERED);
        return Arrays.asList(first, second);
    }

    private List<ConfigAttribute> withoutOAuth() {
        return Arrays.asList((ConfigAttribute) new SecurityConfig(AuthenticatedVoter.IS_AUTHENTICATED_REMEMBERED));
    }
}
