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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.materials.ValidationBean;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

import com.thoughtworks.go.server.messaging.SendEmailMessage;
import org.jmock.Expectations;
import static org.jmock.Expectations.same;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class BackgroundEmailSenderTest {

    GoMailSender neverReturns = new GoMailSender() {
        public ValidationBean send(String subject, String body, String to) {
            try {
                Thread.sleep(10000000);
            } catch (InterruptedException e) {
            }
            return null;
        }

        public ValidationBean send(SendEmailMessage message) {
            return send(message.getSubject(), message.getBody(), message.getTo());
        }

    };
    private Mockery mockery;
    private GoMailSender sender;

    @Before
    public void setUp() {
        mockery = new Mockery();
        sender = mockery.mock(GoMailSender.class);
    }

    @Test
    public void shouldReturnNotValidIfSendingTimesout() {
        BackgroundMailSender background = new BackgroundMailSender(neverReturns, 1);
        ValidationBean validationBean = background.send("Subject", "body", "to@someone");
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("Failed to send an email. Please check the GoCD server logs for any extra information that might be present."));
    }

    @Test
    public void shouldReturnWhateverTheOtherSenderReturnsIfSendingDoesNotTimeout() {
        final ValidationBean validationBean = ValidationBean.valid();
        mockery.checking(new Expectations() {
            {
                one(sender).send("Subject", "body", "to@someone");
                will(returnValue(validationBean));
            }
        });
        BackgroundMailSender background = new BackgroundMailSender(sender, 1000);
        ValidationBean returned = background.send("Subject", "body", "to@someone");
        assertThat(returned, same(validationBean));
    }


}
