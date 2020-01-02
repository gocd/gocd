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
package com.thoughtworks.go.server.messaging;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SendEmailMessage implements GoMessage {
    private final String subject;
    private final String body;
    private final String to;

    public SendEmailMessage(String subject, String body, String to) {
        this.subject = subject;
        this.body = body;
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SendEmailMessage that = (SendEmailMessage) o;

        if (body != null ? !body.equals(that.body) : that.body != null) {
            return false;
        }
        if (subject != null ? !subject.equals(that.subject) : that.subject != null) {
            return false;
        }
        if (to != null ? !to.equals(that.to) : that.to != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }

    public String getBody() {
        return body;
    }

    public String getSubject() {
        return subject;
    }

    public String getTo() {
        return to;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
