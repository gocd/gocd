/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config.upgrades;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConstants;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

// test for 14.xsl
public class AuthorizationMigrationTest {

    private static final String OLD_AUTH = """
                      <auth>
                        <role>admin</role>
                        <role>qa_lead</role>
                        <user>jez</user>
                      </auth>
            """;
    private static final String NEW_AUTHORIZATION = """
                      <authorization>
                        <role>admin</role>
                        <role>qa_lead</role>
                        <user>jez</user>
                      </authorization>
            """;

    private static final String CONFIG_WITH_AUTH = ("""
            <?xml version="1.0" encoding="utf-8"?><cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="13">
              <server artifactsdir="other-artifacts">
                <security>
                  <roles>
                    <role name="admin" />
                    <role name="qa_lead" />
                  </roles>
                </security>
              </server>
              <pipelines group="defaultGroup">
                <pipeline name="pipeline1" labeltemplate="alpha.${COUNT}">
                   <materials>
                     <svn url="foobar" checkexternals="true" />
                   </materials>
                  <stage name="stage1">
                   <approval type="manual">
            %s       </approval>
                   <jobs>
                     <job name="functional">
                       <artifacts>
                         <log src="artifact1.xml" dest="cruise-output" />
                       </artifacts>
                     </job>
                     <job name="unit">
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>""").formatted(OLD_AUTH);

    @Test
    public void shouldMigrateAuthToAuthorization() throws Exception {
        String newConfig = ConfigMigrator.migrate(CONFIG_WITH_AUTH).replace("\r\n", "\n");
        assertThat(newConfig).contains(String.valueOf(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(newConfig).contains(NEW_AUTHORIZATION);
    }

    @Test
    public void shouldBeAbleToParseNewAuthorization() throws Exception {
        AuthConfig config = new MagicalGoConfigXmlLoader(ConfigElementImplementationRegistryMother.withNoPlugins()).fromXmlPartial(NEW_AUTHORIZATION, AuthConfig.class);
        assertThat(config.size()).isEqualTo(3);
    }

    @Test
    public void shouldBeAbleToParseNewConfig() {
        CruiseConfig newConfig = ConfigMigrator.loadWithMigration(CONFIG_WITH_AUTH).config;
        assertThat(newConfig.stageConfigByName(new CaseInsensitiveString("pipeline1"), new CaseInsensitiveString("stage1")).getApproval().getAuthConfig().size()).isEqualTo(3);
    }

    @Test
    public void shouldBeAbleToWriteNewConfig() throws Exception {
        CruiseConfig newConfig = ConfigMigrator.loadWithMigration(CONFIG_WITH_AUTH).config;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(ConfigElementImplementationRegistryMother.withNoPlugins()).write(newConfig, buffer, false);

        assertThat(buffer.toString().replace("\r\n", "\n")).contains(NEW_AUTHORIZATION);
    }
}
