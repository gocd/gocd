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

package com.thoughtworks.go.helper;

import javax.mail.Message;

import com.thoughtworks.go.matchers.EmailMatchers;
import com.thoughtworks.go.utils.Pop3MailClient;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class Pop3Matchers {

    public static TypeSafeMatcher<Pop3MailClient> emailContentContains(final String content) {
        return EmailMatchers.emailContentContains(content);
    }

    public static TypeSafeMatcher<Pop3MailClient> emailSubjectContains(final String subject) {
        return EmailMatchers.emailSubjectContains(subject);
    }

    public static TypeSafeMatcher<Pop3MailClient> emailCount(final String subject, int count) {
        return new TypeSafeMatcher<Pop3MailClient>() {
            private String errorMessage = "";
            public boolean matchesSafely(Pop3MailClient client) {
                try {
                    Message message = client.findMessageWithSubject(subject);
                    boolean b = message != null;
                    if (b) {
                        errorMessage = "No message";
                    }
                    return b;
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            }
            public void describeTo(Description description) {
                description.appendText(String.format("Expected to find message with subject [%s], but got error: %s",
                        subject, errorMessage));
            }
        };
    }
}
