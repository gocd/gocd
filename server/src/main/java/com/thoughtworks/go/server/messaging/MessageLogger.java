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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageLogger implements GoMessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(MessageLogger.class);

    @Autowired
    public MessageLogger(EmailNotificationTopic emailNotificationTopic, JobStatusTopic jobStatusTopic) {
        emailNotificationTopic.addListener(this);
        jobStatusTopic.addListener(this);
    }

    @Override
    public void onMessage(GoMessage message) {
        LOG.debug("{}", message);
    }
}
