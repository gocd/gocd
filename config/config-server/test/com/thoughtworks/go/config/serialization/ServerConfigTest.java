/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ServerConfigTest {

    @Test
    public void shouldParseServerConfigWithMailhost() throws Exception {
        String xml = "<mailhost hostname=\"smtp.company.com\" port=\"25\" "
                + "username=\"smtpuser\" password=\"password\" tls=\"true\" "
                + "from=\"cruise@me.com\" admin=\"jez@me.com\"/>";

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

        CruiseConfig config = new MagicalGoConfigXmlLoader(new ConfigCache(), registry).loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(
                ConfigFileFixture.withServerConfig(xml)))).config;
        MailHost mailHost = config.server().mailHost();
        assertThat(mailHost,
                is(new MailHost("smtp.company.com", 25, "smtpuser", "password", true, true, "cruise@me.com", "jez@me.com")));
    }
}
