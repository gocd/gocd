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

package com.thoughtworks.go.server.domain.xml;

import java.util.Arrays;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.XmlWriterContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UsersXmlViewModelTest {

    @Test
    public void shouldConvertUsersToXmlDocument() throws Exception {
        final User userA = new User("userA", "User A", new String[]{"userA"}, "userA@example.com", true);
        userA.disable();
        User userB = new User("userB", "User B", new String[]{"userB", "user-B"}, "userB@example.com", false);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                + "<users type=\"array\">"
                                    + "<user>"
                                    + "<name>userA</name>"
                                    + "<displayName>User A</displayName>"
                                    + "<matcher>userA</matcher>"
                                    + "<email>userA@example.com</email>"
                                    + "<emailMe type=\"boolean\">true</emailMe>"
                                    + "<enabled type=\"boolean\">false</enabled>"
                                    + "</user>"
                                    + "<user>"
                                    + "<name>userB</name>"
                                    + "<displayName>User B</displayName>"
                                    + "<matcher>userB,user-B</matcher>"
                                    + "<email>userB@example.com</email>"
                                    + "<emailMe type=\"boolean\">false</emailMe>"
                                    + "<enabled type=\"boolean\">true</enabled>"
                                    + "</user>"
                                + "</users>" ;

        assertEquals(expectedXml, toXml(userA, userB));
    }

    @Test
    public void shouldConvertUsersWithoutEmailToXmlDocument() throws Exception {
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                + "<users type=\"array\">"
                                    + "<user>"
                                    + "<name>userA</name>"
                                    + "<displayName>User A</displayName>"
                                    + "<matcher>userA</matcher>"
                                    + "<email></email>"
                                    + "<emailMe type=\"boolean\">true</emailMe>"
                                    + "<enabled type=\"boolean\">true</enabled>"
                                    + "</user>"
                                + "</users>" ;

        String actualXml = toXml(new User("userA", "User A", new String[]{"userA"}, null, true));
        assertEquals(expectedXml, actualXml);
    }

    private String toXml(User... users) throws Exception {
        UsersXmlViewModel model = new UsersXmlViewModel(Arrays.asList(users));
        return model.toXml(new XmlWriterContext("http://baseurl/go", null, null, null, null)).asXML();
    }

}
