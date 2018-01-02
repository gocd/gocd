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

package com.thoughtworks.go.listener;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SecurityConfigChangeListenerTest {

    @Test
    public void shouldCareAboutSecurityAuthConfigChange() {
        SecurityConfigChangeListener securityConfigChangeListener = new SecurityConfigChangeListener() {
            @Override
            public void onEntityConfigChange(Object entity) {

            }
        };
        assertThat(securityConfigChangeListener.shouldCareAbout(new SecurityAuthConfig()), is(true));
    }

    @Test
    public void shouldCareAboutRoleConfigChange() {
        SecurityConfigChangeListener securityConfigChangeListener = new SecurityConfigChangeListener() {
            @Override
            public void onEntityConfigChange(Object entity) {

            }
        };
        assertThat(securityConfigChangeListener.shouldCareAbout(new RoleConfig()), is(true));
    }

    @Test
    public void shouldCareAboutPluginRoleConfigChange() {
        SecurityConfigChangeListener securityConfigChangeListener = new SecurityConfigChangeListener() {
            @Override
            public void onEntityConfigChange(Object entity) {

            }
        };
        assertThat(securityConfigChangeListener.shouldCareAbout(new PluginRoleConfig()), is(true));
    }

    @Test
    public void shouldNotCareAboutEntityWhichIsNotPartOfSecurityConfig() {
        SecurityConfigChangeListener securityConfigChangeListener = new SecurityConfigChangeListener() {

            @Override
            public void onEntityConfigChange(Object entity) {

            }
        };

        assertThat(securityConfigChangeListener.shouldCareAbout(new ElasticProfile()), is(false));
    }
}