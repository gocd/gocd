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
package com.thoughtworks.go.server.newsecurity.providers;

import com.thoughtworks.go.server.newsecurity.models.AnonymousCredential;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnonymousAuthenticationProvider extends AbstractAuthenticationProvider<AnonymousCredential> {
    private final Clock clock;
    private final AuthorityGranter authorityGranter;

    @Autowired
    public AnonymousAuthenticationProvider(Clock clock,
                                           AuthorityGranter authorityGranter) {
        this.clock = clock;
        this.authorityGranter = authorityGranter;
    }

    @Override
    public AuthenticationToken<AnonymousCredential> reauthenticate(AuthenticationToken<AnonymousCredential> originalToken) {
        return createNewToken();
    }

    @Override
    public AuthenticationToken<AnonymousCredential> authenticate(AnonymousCredential credentials, String pluginId) {
        return createNewToken();
    }

    private AuthenticationToken<AnonymousCredential> createNewToken() {
        GoUserPrinciple anonymous = new GoUserPrinciple("anonymous", "anonymous", authorityGranter.authorities("anonymous"));

        AuthenticationToken<AnonymousCredential> authenticationToken = new AuthenticationToken<>(anonymous, AnonymousCredential.INSTANCE, null, clock.currentTimeMillis(), null);

        LOGGER.debug("Authenticating as anonymous user with role(s) {}", anonymous.getAuthorities());
        return authenticationToken;
    }


}
