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
package com.thoughtworks.go.server.newsecurity.helpers;

import com.google.common.collect.Sets;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

public class AuthorityVerifier {
    private final HashSet<GrantedAuthority> grantedAuthorities;

    public AuthorityVerifier(Set<GrantedAuthority> grantedAuthorities) {
        Assert.notEmpty(grantedAuthorities, "granted authority must not be empty");
        Assert.noNullElements(grantedAuthorities.toArray(), "granted authority must not contain null elements");
        this.grantedAuthorities = new HashSet<>(grantedAuthorities);
    }

    public boolean hasAnyAuthorityMatching(Set<GrantedAuthority> userAuthorities) {
        return !Sets.intersection(grantedAuthorities, userAuthorities).isEmpty();
    }

}
