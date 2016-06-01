/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;

import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.service.GoConfigService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.AttributesMapperCallbackHandler;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.ldap.SpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LdapUserSearch implements org.springframework.security.ldap.LdapUserSearch {
    private final GoConfigService goConfigService;
    private final Logger logger;
    private final SpringSecurityContextSource contextFactory;
    private static final String SAM_ACCOUNT_NAME = "sAMAccountName";
    private static final String UID = "uid";
    private static final String COMMON_NAME = "cn";
    private static final String USER_PRINCIPLE_NAME = "userPrincipalName";
    private static final String MAIL_ID = "mail";
    private static final String ALIAS_EMAIL_ID = "otherMailbox";
    private static final long MAX_RESULTS = 100;
    private LdapTemplate ldapTemplate;


    @Autowired
    public LdapUserSearch(GoConfigService goConfigService, ContextSource contextFactory) {
        this(goConfigService, contextFactory, new LdapTemplate(contextFactory), Logger.getLogger(LdapUserSearch.class));
    }

    public LdapUserSearch(GoConfigService goConfigService, ContextSource contextFactory, final LdapTemplate ldapTemplate, Logger logger) {
        this.goConfigService = goConfigService;
        this.logger = logger;
        this.contextFactory = (SpringSecurityContextSource) contextFactory;
        this.ldapTemplate = ldapTemplate;
    }

    public DirContextOperations searchForUser(String username) {
        SecurityConfig securityConfig = goConfigService.security();
        if (!securityConfig.isSecurityEnabled()) {
            return null;
        }
        LdapConfig ldapConfig = securityConfig.ldapConfig();

        RuntimeException lastFoundException = null;
        BaseConfig failedBaseConfig = null;
        for (BaseConfig baseConfig : ldapConfig.getBasesConfig()) {
            if(lastFoundException != null && !(lastFoundException instanceof BadCredentialsException)) {
                logger.warn(String.format("The ldap configuration for search base '%s' is invalid", failedBaseConfig.getValue()),lastFoundException);
            }
            FilterBasedLdapUserSearch search = getFilterBasedLdapUserSearch(baseConfig.getValue(), ldapConfig.searchFilter());
            search.setSearchSubtree(true);
            search.setSearchTimeLimit(5000); // timeout after five seconds
            try {
                return search.searchForUser(username);
            } catch (UsernameNotFoundException e) {
                failedBaseConfig = baseConfig;
                lastFoundException = new BadCredentialsException("Bad credentials");
            } catch (RuntimeException e) {
                failedBaseConfig = baseConfig;
                lastFoundException = e;
            }
        }
        if(lastFoundException != null) {
            throw lastFoundException;

        }
        throw new RuntimeException("No LDAP Search Bases are configured.");
    }

    public List<User> search(String username) {
        SecurityConfig securityConfig = goConfigService.security();
        return search(username, securityConfig.ldapConfig());
    }

    public List<User> search(String username, LdapConfig ldapConfig) {
        if(ldapConfig.getBasesConfig().isEmpty()) {
            throw new RuntimeException("Atleast one Search Base needs to be configured.");
        }
        OrFilter filter = new OrFilter();
        String searchString = MessageFormat.format("*{0}*", username);
        filter.or(new LikeFilter(SAM_ACCOUNT_NAME, searchString));
        filter.or(new LikeFilter(UID, searchString));
        filter.or(new LikeFilter(COMMON_NAME, searchString));
        filter.or(new LikeFilter(USER_PRINCIPLE_NAME, searchString));
        filter.or(new LikeFilter(MAIL_ID, searchString));
        filter.or(new LikeFilter(ALIAS_EMAIL_ID, searchString));    // This field is optional to search based on. Only for alias emails.
        //List ldapUserList = template.search(ldapConfig.searchBase(), filter.encode(), attributes);
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setCountLimit(MAX_RESULTS);
        AttributesMapperCallbackHandler handler = getAttributesMapperCallbackHandler();
        for (BaseConfig baseConfig : ldapConfig.getBasesConfig()) {
            try {
                ldapTemplate.search(baseConfig.getValue(), filter.encode(), controls, handler);
            } catch (org.springframework.ldap.LimitExceededException e) {
                throw new NotAllResultsShownException(buildUserList(handler.getList()));
            }
        }
        return buildUserList(handler.getList());
    }

    AttributesMapperCallbackHandler getAttributesMapperCallbackHandler() {
        AttributesMapper attributes = new AttributesMapper() {
            public Object mapFromAttributes(Attributes attributes) throws NamingException {
                return attributes;
            }
        };
        return new AttributesMapperCallbackHandler(attributes);
    }

    private List<User> buildUserList(List ldapUserList) {
        List<User> users = new ArrayList<>();

        for (Object ldapUser : ldapUserList) {
            try {
                users.add(toUser((BasicAttributes) ldapUser));
            } catch (NamingException e) {
                throw new RuntimeException("Ldap attributes configured incorrectly. Mismatch in expected attributes.", e);
            }
        }

        return users;
    }

    private User toUser(BasicAttributes attributes) throws NamingException {
        String fullName = attributes.get(COMMON_NAME).get().toString();
        Attribute samAccName = attributes.get(SAM_ACCOUNT_NAME);
        String loginName;
        loginName = samAccName != null ? samAccName.get().toString() : attributes.get(UID).get().toString();

        Attribute emailAttr = attributes.get(USER_PRINCIPLE_NAME);
        String emailAddress = emailAttr != null ? emailAttr.get().toString() : "";
        if(emailAddress.equals("")){
            emailAttr = attributes.get(MAIL_ID);
            emailAddress = emailAttr != null ? emailAttr.get().toString() : "";
        }
        return new User(loginName, fullName, emailAddress);
    }

    public FilterBasedLdapUserSearch getFilterBasedLdapUserSearch(final String searchBase, final String searchFilter) {
        return new FilterBasedLdapUserSearch(searchBase, searchFilter, contextFactory);
    }

    public static class NotAllResultsShownException extends RuntimeException {
        private final List<User> users;

        public NotAllResultsShownException(List<User> users) {
            this.users = users;
        }

        public List<User> getUsers() {
            return users;
        }
    }
}
