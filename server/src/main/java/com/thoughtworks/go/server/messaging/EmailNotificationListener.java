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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoMailSender;
import com.thoughtworks.go.config.GoSmtpMailSender;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.service.GoConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationListener implements GoMessageListener<SendEmailMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationListener.class);
    private final GoConfigService goConfigService;
    private GoMailSenderFactory factory;

    @Autowired
    public EmailNotificationListener(EmailNotificationTopic emailNotificationTopic,
                                     GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
        this.factory = new GoMailSenderFactory(goConfigService);
        emailNotificationTopic.addListener(this);
    }

    /**
     * only used for test
     *
     * @deprecated
     */
    EmailNotificationListener(GoConfigService goConfigService, GoMailSenderFactory factory) {
        this.goConfigService = goConfigService;
        this.factory = factory;
    }

    @Override
    public void onMessage(SendEmailMessage message) {
        CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
        if (!cruiseConfig.isMailHostConfigured()) {
            return;
        }
        GoMailSender mailSender = factory.createSender();
        ValidationBean validationBean = mailSender.send(message.getSubject(), message.getBody(), message.getTo());
        if (!validationBean.isValid()) {
            LOGGER.error(validationBean.getError());
        }
    }

    static class GoMailSenderFactory {
        private final GoConfigService goConfigService;

        public GoMailSenderFactory(GoConfigService goConfigService) {
            this.goConfigService = goConfigService;
        }

        public GoMailSender createSender() {
            return GoSmtpMailSender.createSender(goConfigService.currentCruiseConfig().mailHost());
        }
    }

}
