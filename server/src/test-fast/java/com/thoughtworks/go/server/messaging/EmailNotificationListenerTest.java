/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoSmtpMailSender;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmailNotificationListenerTest {
    public GoConfigService goConfigService;
    public EmailNotificationListener.GoMailSenderFactory goMailSenderFactory;
    public EmailNotificationListener emailNotificationListener;

    @BeforeEach
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        goMailSenderFactory = mock(EmailNotificationListener.GoMailSenderFactory.class);
        emailNotificationListener = new EmailNotificationListener(goConfigService, goMailSenderFactory);
    }

    @Test
    public void shouldNotCreateMailSenderIfMailHostIsNotConfigured() {
        when(goConfigService.currentCruiseConfig()).thenReturn(new BasicCruiseConfig());
        emailNotificationListener.onMessage(null);
    }

    @Test
    public void shouldCreateMailSenderIfMailHostIsConfigured() {
        final MailHost mailHost = new MailHost("hostName", 1234, "user", "pass", true, true, "from", "admin@local.com");
        final CruiseConfig config = GoConfigMother.cruiseConfigWithMailHost(mailHost);
        when(goConfigService.currentCruiseConfig()).thenReturn(config);
        when(goMailSenderFactory.createSender()).thenReturn(GoSmtpMailSender.createSender(mailHost));
        emailNotificationListener.onMessage(new SendEmailMessage("subject", "body", "to"));
    }

}
