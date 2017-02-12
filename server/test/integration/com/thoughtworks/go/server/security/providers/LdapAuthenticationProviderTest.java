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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
}
)
public class LdapAuthenticationProviderTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private LdapAuthenticationProvider ldapAuthenticationProvider;
    private GoConfigFileHelper configFileHelper;

    @Before
    public void setUp() throws Exception {
        configFileHelper = new GoConfigFileHelper();
        configFileHelper.usingCruiseConfigDao(goConfigDao);
        configFileHelper.initializeConfigFile();
    }

    @Test
    public void shouldNotSupportAuthenticationIfNoLdapConfig() throws IOException {
        assertThat(ldapAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class), is(false));
    }

}
