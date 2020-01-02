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
package com.thoughtworks.go.plugin.access.authorization.v1;

import com.thoughtworks.go.config.PluginRoleConfig;
import org.junit.Test;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class AuthorizationMessageConverterV1Test {

    @Test
    public void getProcessRoleConfigsResponseBody() throws Exception {
        AuthorizationMessageConverterV1 converter = new AuthorizationMessageConverterV1();
        String json = converter.getProcessRoleConfigsResponseBody(singletonList(new PluginRoleConfig("blackbird", "ldap", create("foo", false, "bar"))));
        assertThatJson("[{\"name\":\"blackbird\",\"configuration\":{\"foo\":\"bar\"}}]").isEqualTo(json);
    }
}
