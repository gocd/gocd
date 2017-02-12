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

package com.thoughtworks.go.server.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class X509AuthoritiesPopulator implements com.thoughtworks.go.server.security.x509.X509AuthoritiesPopulator, InitializingBean, MessageSourceAware {

    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]*)");
    private static final Pattern OU_PATTERN = Pattern.compile("OU=([^,]*)");
    public static final String ROLE_AGENT = "ROLE_AGENT";
    public static String ROLE_SHINE = "ROLE_SHINE";
    private String role;

    public X509AuthoritiesPopulator(String role) {
        this.role = role;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.messages, "A message source must be set");
    }

    public UserDetails getUserDetails(X509Certificate clientCert) throws AuthenticationException {
        X500Principal principal = clientCert.getSubjectX500Principal();
        Matcher cnMatcher = CN_PATTERN.matcher(principal.getName());
        Matcher ouMatcher = OU_PATTERN.matcher(principal.getName());
        if (cnMatcher.find() && ouMatcher.find()) {
            SimpleGrantedAuthority agentAuthority = new SimpleGrantedAuthority(role);
            return new User(cnMatcher.group(1), "", true, true, true, true, Collections.singletonList(agentAuthority));
        }
        throw new BadCredentialsException("Couldn't find CN and/or OU for the certificate");
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }


}
