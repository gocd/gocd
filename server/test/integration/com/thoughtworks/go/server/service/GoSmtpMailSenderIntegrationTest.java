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

package com.thoughtworks.go.server.service;

import com.googlecode.junit.ext.JunitExtRunner;
import com.thoughtworks.go.config.GoSmtpMailSender;
import com.thoughtworks.go.domain.materials.ValidationBean;
import static com.thoughtworks.go.helper.Pop3Matchers.emailContentContains;
import static com.thoughtworks.go.helper.Pop3Matchers.emailSubjectContains;
import com.thoughtworks.go.mail.TestMailServer;
import static com.thoughtworks.go.util.GoConstants.TEST_EMAIL_SUBJECT;
import static com.thoughtworks.go.utils.Assertions.assertWillHappen;
import com.thoughtworks.go.utils.Pop3MailClient;
import static org.hamcrest.core.Is.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@org.junit.Ignore(value = "ChrisS - This is fragile on cruise")
@RunWith(JunitExtRunner.class)
public class GoSmtpMailSenderIntegrationTest {
    private Pop3MailClient pop3MailClient;
    private static final int TIMEOUT = 5 * 60 * 1000;
    private String hostName;
    private String username;
    private String password;

    @Before
    public void setUp() throws Exception {
        TestMailServer.start();
        this.hostName = "127.0.0.1";
        username = "cruise2@cruise.com";
        password = "password123";
        pop3MailClient = new Pop3MailClient(hostName, 10110, username, password);
    }

    @After
    public void tearDown(){
        TestMailServer.stop();
    }
    
    @Test
    public void shouldSendEmailCorrectly() throws Exception {
        String from = "cruise2@cruise.com";
        String to = "cruise2@cruise.com";
        GoSmtpMailSender sender = new GoSmtpMailSender(hostName, 10025, username, password, false, from, to);
        ValidationBean validationBean = sender.send(TEST_EMAIL_SUBJECT, GoSmtpMailSender.emailBody(), to);

        assertThat(validationBean.isValid(), is(true));

        assertWillHappen(pop3MailClient, emailContentContains(GoSmtpMailSender.emailBody()));
        assertWillHappen(pop3MailClient, emailSubjectContains(TEST_EMAIL_SUBJECT));
    }
}
