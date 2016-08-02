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
import com.thoughtworks.go.server.messaging.SendEmailMessage;

public interface GoMailSender {
    String ERROR_MESSAGE = "Failed to send an email. Please check the GoCD server logs for any extra information that might be present.";

    ValidationBean send(String subject, String body, String to);

    ValidationBean send(SendEmailMessage message);
}
