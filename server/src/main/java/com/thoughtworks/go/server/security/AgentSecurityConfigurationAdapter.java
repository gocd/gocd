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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

import java.security.cert.X509Certificate;
import java.util.Collections;

@Configuration
@Order(3)
public class AgentSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

    private static final String AGENT = GoAuthority.ROLE_AGENT.name();

    @Autowired
    @Qualifier("userCache")
    private Cache userCache;

    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/remoting/remoteBuildRepository").hasAnyAuthority(AGENT)
                .antMatchers("/remoting/files/**").hasAnyAuthority(AGENT)
                .antMatchers("/remoting/properties/**").hasAnyAuthority(AGENT)
                .antMatchers("/agent-websocket/**").hasAnyAuthority(AGENT)
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
