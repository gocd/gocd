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
package com.thoughtworks.go.server.newsecurity.helpers;

import com.thoughtworks.go.server.security.GoAuthority;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.thoughtworks.go.server.security.GoAuthority.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AuthorityVerifierTest {

    @Test
    void shouldAnswerTrueWhenAuthoritiesMatch() {
        AuthorityVerifier authorityVerifier = new AuthorityVerifier(authoritySet(ROLE_GROUP_SUPERVISOR, ROLE_TEMPLATE_SUPERVISOR));

        assertThat(authorityVerifier.hasAnyAuthorityMatching(authoritySet(ROLE_GROUP_SUPERVISOR)))
                .isTrue();
        assertThat(authorityVerifier.hasAnyAuthorityMatching(authoritySet(ROLE_USER, ROLE_GROUP_SUPERVISOR)))
                .isTrue();
    }

    @Test
    void shouldAnswerFalseWhenAuthoritiesDoNotMatch() {
        AuthorityVerifier authorityVerifier = new AuthorityVerifier(authoritySet(ROLE_GROUP_SUPERVISOR, ROLE_TEMPLATE_SUPERVISOR));
        assertThat(authorityVerifier.hasAnyAuthorityMatching(authoritySet(ROLE_AGENT, ROLE_SUPERVISOR)))
                .isFalse();
    }

    @Test
    void shouldAnswerFalseWhenAuthoritiesAreEmpty() {
        AuthorityVerifier authorityVerifier = new AuthorityVerifier(authoritySet(ROLE_GROUP_SUPERVISOR, ROLE_TEMPLATE_SUPERVISOR));
        assertThat(authorityVerifier.hasAnyAuthorityMatching(authoritySet()))
                .isFalse();
    }

    @Test
    void shouldBailWhenNoAuthoritiesArePassedToVerifier() {
        assertThatCode(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new AuthorityVerifier(new HashSet<>());
            }
        }).hasMessage("granted authority must not be empty");
        assertThatCode(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new AuthorityVerifier(null);
            }
        }).hasMessage("granted authority must not be empty");
        assertThatCode(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new AuthorityVerifier(Collections.singleton(null));
            }
        }).hasMessage("granted authority must not contain null elements");
    }

    private Set<GrantedAuthority> authoritySet(GoAuthority... authorities) {
        if (authorities.length == 0) {
            return Collections.emptySet();
        }


        final HashSet<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (GoAuthority authority : authorities) {
            grantedAuthorities.add(authority.asAuthority());
        }

        return grantedAuthorities;
    }
}
