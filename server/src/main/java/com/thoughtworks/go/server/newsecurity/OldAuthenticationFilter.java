///*
// * Copyright 2018 ThoughtWorks, Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.thoughtworks.go.server.newsecurity;
//
//import com.thoughtworks.go.server.newsecurity.authentication.providers.PasswordBasedPluginAuthenticationProvider;
//import com.thoughtworks.go.server.security.GoAuthority;
//import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
//import com.thoughtworks.go.server.service.GoConfigService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//import java.util.UUID;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import static java.util.Collections.singletonList;
//import static org.apache.commons.lang.StringUtils.isBlank;
//
//@Component
//public class OldAuthenticationFilter extends OncePerRequestFilter {
//
//    private static final String CURRENT_USER = "CURRENT_USER";
//    private static final String ANONYMOUS_AUTHENTICATION_KEY = UUID.randomUUID().toString();
//
//    private final LoginHandler loginHandler = new LoginHandler();
//
//    @Autowired
//    private final PasswordBasedPluginAuthenticationProvider pluginAuthenticationProvider;
//    @Autowired
//    private final GoConfigService goConfigService;
//
//    public OldAuthenticationFilter(PasswordBasedPluginAuthenticationProvider pluginAuthenticationProvider, GoConfigService goConfigService) {
//        this.pluginAuthenticationProvider = pluginAuthenticationProvider;
//        this.goConfigService = goConfigService;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        HttpSession session = request.getSession();
//        User currentUser = (User) session.getAttribute(CURRENT_USER);
//
//        if (currentUser == null) {
//            currentUser = verifyX509Cert(request);
//        }
//
//        if (currentUser == null && goConfigService.isSecurityEnabled()) {
//            currentUser = verifyBasicAuthCredentials(request);
//
//            if (currentUser == null) {
//                currentUser = verifyOauthCredentials(request);
//            }
//
//            if (currentUser == null && supportsAnonymous(request)) {
//                currentUser = new GoUserPrinciple("anonymous", "Anonymous", "", singletonList(GoAuthority.ROLE_ANONYMOUS.asAuthority()), "anonymous");
//            }
//
//        } else {
//            currentUser = new GoUserPrinciple("anonymous", "Anonymous", "", singletonList(GoAuthority.ROLE_SUPERVISOR.asAuthority()), "anonymous");
//        }
//
//        if (currentUser == null) {
//            loginHandler.handle(request, response);
//        } else {
//            session.setAttribute(CURRENT_USER, currentUser);
//            filterChain.doFilter(request, response);
//        }
//    }
//
//    private User verifyBasicAuthCredentials(HttpServletRequest request) {
//        if (!pluginAuthenticationProvider.hasPluginsForUsernamePasswordAuth()) {
//            return null;
//        }
//        String header = request.getHeader("Authorization");
//        if (isBlank(header)) {
//            return null;
//        }
//
//        final Pattern pattern = Pattern.compile("basic (.*)", Pattern.CASE_INSENSITIVE);
//
//        final Matcher matcher = pattern.matcher(header);
//        if (matcher.matches()) {
//            final String encodedCredentials = matcher.group(1);
//            final byte[] decode = Base64.getDecoder().decode(encodedCredentials);
//            String decodedCredentials = new String(decode, StandardCharsets.UTF_8);
//
//            final int indexOfSeparator = decodedCredentials.indexOf(':');
//            if (indexOfSeparator == -1) {
//                throw new BadCredentialsException("Invalid basic authentication token");
//            }
//
//            final String username = decodedCredentials.substring(0, indexOfSeparator);
//            final String password = decodedCredentials.substring(indexOfSeparator + 1);
//
//            return pluginAuthenticationProvider.authenticate(username, password);
//        }
//    }
//
//    private boolean supportsAnonymous(HttpServletRequest request) {
//        return new AntPathRequestMatcher("/cctray.xml").matches(request);
//    }
//
//    private class LoginHandler {
//        public void handle(HttpServletRequest request, HttpServletResponse response) {
//
//        }
//    }
//
//}
