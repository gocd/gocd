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

import com.thoughtworks.go.server.newsecurity.authentication.filters.CachingSubjectDnX509PrincipalExtractor;
import net.sf.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;

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

    private static class X509UserDetailService implements org.springframework.security.core.userdetails.UserDetailsService {

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            return new User("_go_agent_" + username, "", Collections.singletonList(GoAuthority.ROLE_AGENT.asAuthority()));
        }
    }
}
