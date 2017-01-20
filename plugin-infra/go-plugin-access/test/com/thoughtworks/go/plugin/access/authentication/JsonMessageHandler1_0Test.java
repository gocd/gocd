/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.authentication;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.authentication.models.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JsonMessageHandler1_0Test {
    private JsonMessageHandler1_0 messageHandler;

    @Before
    public void setUp() throws Exception {
        messageHandler = new JsonMessageHandler1_0();
    }

    @Test
    public void shouldHandleResponseMessageForPluginConfiguration() throws Exception {
        AuthenticationPluginConfiguration configuration = messageHandler.responseMessageForPluginConfiguration("{\"display-name\":\"display-name\",\"display-image-url\":\"display-image-url\",\"supports-web-based-authentication\":true,\"supports-password-based-authentication\":true}");
        assertThat(configuration, is(new AuthenticationPluginConfiguration("display-name", "display-image-url", true, true)));
    }

    @Test
    public void shouldHandleEmptyResponseMessageForPluginConfiguration() throws Exception {
        AuthenticationPluginConfiguration configuration = messageHandler.responseMessageForPluginConfiguration("{}");
        assertThat(configuration, is(new AuthenticationPluginConfiguration(null, null, false, false)));
    }

    @Test
    public void shouldBuildRequestBodyForAuthenticateUser() throws Exception {
        String requestMessage = messageHandler.requestMessageForAuthenticateUser("username", "password");
        Object o = new GsonBuilder().create().fromJson(requestMessage, Object.class);

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("username", "username");
        requestMap.put("password", "password");
        assertEquals(o, requestMap);
    }

    @Test
    public void shouldHandleResponseMessageForAuthenticateUser() throws Exception {
        User user = messageHandler.responseMessageForAuthenticateUser("{\"user\":{\"username\":\"username\",\"display-name\":\"display-name\",\"email-id\":\"test@test.com\"}}");
        assertThat(user, is(new User("username", "display-name", "test@test.com")));
    }

    @Test
    public void shouldHandleMissingDataInResponseMessageForAuthenticateUser() throws Exception {
        User user = messageHandler.responseMessageForAuthenticateUser("{\"user\":{\"username\":\"username\"}}");
        assertThat(user, is(new User("username", null, null)));
    }

    @Test
    public void shouldHandleEmptyResponseMessageForAuthenticateUser() throws Exception {
        User user1 = messageHandler.responseMessageForAuthenticateUser("");
        assertThat(user1, is(nullValue()));

        User user2 = messageHandler.responseMessageForAuthenticateUser("{}");
        assertThat(user2, is(nullValue()));
    }

    @Test
    public void shouldBuildRequestBodyForSearchUser() throws Exception {
        String requestMessage = messageHandler.requestMessageForSearchUser("search-term");
        assertThat(requestMessage, is("{\"search-term\":\"search-term\"}"));
    }

    @Test
    public void shouldHandleResponseMessageForSearchUser() throws Exception {
        String user1Json = "{\"username\":\"username1\",\"display-name\":\"user 1\",\"email-id\":\"test1@test.com\"}";
        String user2Json = "{\"username\":\"username2\",\"display-name\":\"user 2\",\"email-id\":\"test2@test.com\"}";
        List<User> users = messageHandler.responseMessageForSearchUser(String.format("[%s,%s]", user1Json, user2Json));
        assertThat(users, is(Arrays.asList(new User("username1", "user 1", "test1@test.com"), new User("username2", "user 2", "test2@test.com"))));
    }

    @Test
    public void shouldHandleEmptyResponseMessageForSearchUser() throws Exception {
        List<User> users1 = messageHandler.responseMessageForSearchUser("");
        assertThat(users1.isEmpty(), is(true));

        List<User> users2 = messageHandler.responseMessageForSearchUser("[]");
        assertThat(users2.isEmpty(), is(true));
    }

    @Test
    public void shouldValidateIncorrectJsonForPluginConfiguration() {
        assertThat(errorMessageForPluginConfiguration("[]"), is("Plugin configuration should be returned as a map"));
        assertThat(errorMessageForPluginConfiguration("{\"display-name\":true}"), is("Configuration 'display-name' should be of type string"));
        assertThat(errorMessageForPluginConfiguration("{\"display-name\":\"name\",\"display-image-url\":true}"), is("Configuration 'display-image-url' should be of type string"));
        assertThat(errorMessageForPluginConfiguration("{\"display-name\":\"name\",\"supports-web-based-authentication\":\"test\"}"), is("Configuration 'supports-web-based-authentication' should be of type boolean"));
        assertThat(errorMessageForPluginConfiguration("{\"display-name\":\"name\",\"supports-password-based-authentication\":\"test\"}"), is("Configuration 'supports-password-based-authentication' should be of type boolean"));
    }

    @Test
    public void shouldValidateIncorrectJsonForAuthenticateUser() {
        assertThat(errorMessageForAuthenticateUser("[]"), is("User should be returned as a map"));
        assertThat(errorMessageForAuthenticateUser("{\"user\":[]}"), is("User should be returned as a map"));
        assertThat(errorMessageForAuthenticateUser("{\"user\":{\"username\":true}}"), is("User 'username' should be of type string"));
        assertThat(errorMessageForAuthenticateUser("{\"user\":{\"username\":\"\"}}"), is("User 'username' cannot be empty"));
        assertThat(errorMessageForAuthenticateUser("{\"user\":{\"username\":\"name\",\"display-name\":true}}"), is("User 'display-name' should be of type string"));
        assertThat(errorMessageForAuthenticateUser("{\"user\":{\"username\":\"name\",\"display-name\":\"display\",\"email-id\":true}}"), is("User 'email-id' should be of type string"));
    }

    @Test
    public void shouldValidateIncorrectJsonForSearchUser() {
        assertThat(errorMessageForSearchUser("{}"), is("Search results should be returned as a list"));
    }

    private String errorMessageForPluginConfiguration(String message) {
        try {
            messageHandler.responseMessageForPluginConfiguration(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForAuthenticateUser(String message) {
        try {
            messageHandler.responseMessageForAuthenticateUser(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSearchUser(String message) {
        try {
            messageHandler.responseMessageForSearchUser(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }
}
