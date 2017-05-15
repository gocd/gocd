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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:WEB-INF/spring-rest-servlet.xml"
})
public class AcegiSecurityConfigTest {
    @Autowired
    private FilterSecurityInterceptor filterInvocationInterceptor;
    private DefaultFilterInvocationSecurityMetadataSource objectDefinitionSource;

    @Before
    public void setUp() throws Exception {
        objectDefinitionSource = (DefaultFilterInvocationSecurityMetadataSource) filterInvocationInterceptor.obtainSecurityMetadataSource();
    }

    @Test
    public void shouldAllowOnlyRoleUserToHaveAccessToWildcardUrls() {
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**", "hasAnyRole('ROLE_USER')");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**/*.js", "hasAnyRole('ROLE_USER')");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**/*.css", "hasAnyRole('ROLE_USER')");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/**/*.png", "hasAnyRole('ROLE_USER')");
    }

    @Test
    public void shouldAllowAnonymousAccessToAssets() {
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**", "permitAll");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**/*.js", "permitAll");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**/*.css", "permitAll");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/assets/**/*.jpg", "permitAll");
    }

    @Test
    public void shouldNotAllowAnonymousAccessToWildcardAuthUrl(){
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/auth/login", "permitAll");
        verifyGetAccessToUrlPatternIsAvailableToRole(objectDefinitionSource, "/auth/logout", "permitAll");
    }

    private void verifyGetAccessToUrlPatternIsAvailableToRole(DefaultFilterInvocationSecurityMetadataSource objectDefinitionSource, String urlPattern, String role) {
        Collection<ConfigAttribute> definition = objectDefinitionSource.getAllConfigAttributes();
        Iterator<ConfigAttribute> iterator = definition.iterator();
        StringBuilder allowedAccess = new StringBuilder();
        while (iterator.hasNext()) {
            ConfigAttribute configAttribute = iterator.next();
            if (configAttribute.toString().equals(role))
                return;
            else
                allowedAccess.append(configAttribute.toString() + ",");
        }
        fail(String.format("Expected access to url %s only by %s but found %s", urlPattern, role, allowedAccess.toString()));
    }
}
