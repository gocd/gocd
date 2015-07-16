/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoSmtpMailSender;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class EmailNotificationListenerTest {
    public ClassMockery context;
    public GoConfigService goConfigService;
    public EmailNotificationListener.GoMailSenderFactory goMailSenderFactory;
    public EmailNotificationListener emailNotificationListener;

    @Before
    public void setUp() throws Exception {
        context = new ClassMockery();
        goConfigService = context.mock(GoConfigService.class);
        goMailSenderFactory = context.mock(EmailNotificationListener.GoMailSenderFactory.class);
        emailNotificationListener = new EmailNotificationListener(goConfigService, goMailSenderFactory);
    }

    @Test
    public void shouldNotCreateMailSenderIfMailHostIsNotConfigured() {
        context.checking(new Expectations() {
            {
                allowing(goConfigService).currentCruiseConfig();
                will(returnValue(new BasicCruiseConfig()));
            }
        });
        emailNotificationListener.onMessage(null);
    }

    @Test
    public void shouldCreateMailSenderIfMailHostIsConfigured() {
        final MailHost mailHost = new MailHost("hostName", 1234, "user", "pass", true, true, "from", "admin@local.com");
        final CruiseConfig config = GoConfigMother.cruiseConfigWithMailHost(mailHost);
        context.checking(new Expectations() {
            {
                allowing(goConfigService).currentCruiseConfig();
                will(returnValue(config));
                one(goMailSenderFactory).createSender();
                will(returnValue(GoSmtpMailSender.createSender(mailHost)));
            }
        });
        emailNotificationListener.onMessage(new SendEmailMessage("subject", "body", "to"));
    }

}
