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
package com.thoughtworks.go.plugin.access.notification.v3;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.notificationdata.AgentNotificationData;
import com.thoughtworks.go.domain.notificationdata.StageNotificationData;
import com.thoughtworks.go.plugin.access.notification.DataConverter;
import com.thoughtworks.go.plugin.access.notification.JsonMessageHandler;
import com.thoughtworks.go.plugin.api.response.Result;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class JsonMessageHandler3_0 implements JsonMessageHandler {
    @Override
    public List<String> responseMessageForNotificationsInterestedIn(String responseBody) {
        try {
            Map map;
            try {
                map = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Notifications interested in should be returned as map, with key represented as string and notification names represented as list");
            }
            if (map == null || map.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }

            List notificationNames = new ArrayList();
            if (map.containsKey("notifications") && map.get("notifications") != null) {
                Object notificationsObj = map.get("notifications");

                if (!(notificationsObj instanceof List)) {
                    throw new RuntimeException("'notifications' should be of type list of string");
                }

                notificationNames = (List) notificationsObj;

                for (Object message : notificationNames) {
                    if (!(message instanceof String)) {
                        throw new RuntimeException("Notification 'name' should be of type string");
                    }
                }
            }

            return notificationNames;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    @Override
    public <T> String requestMessageForNotify(T data) {
        return getConverter(data).createRequest();
    }

    private <T> DataConverter getConverter(T data) {
        if (data instanceof StageNotificationData) {
            return new StageConverter((StageNotificationData) data);
        }
        if (data instanceof AgentNotificationData) {
            return new AgentConverter((AgentNotificationData) data);
        }
        throw new NotImplementedException(String.format("Converter for %s not supported", data.getClass().getCanonicalName()));
    }

    @Override
    public Result responseMessageForNotify(String responseBody) {
        return toResult(responseBody);
    }

    private Map parseResponseToMap(String responseBody) {
        return (Map) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

    Result toResult(String responseBody) {
        try {
            Map map;
            try {
                map = parseResponseToMap(responseBody);
            } catch (Exception e) {
                throw new RuntimeException("Notify result should be returned as map, with key represented as string and messages represented as list");
            }
            if (map == null || map.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }

            String status;
            try {
                status = (String) map.get("status");
            } catch (Exception e) {
                throw new RuntimeException("Notify result 'status' should be of type string");
            }

            if (isEmpty(status)) {
                throw new RuntimeException("Notify result 'status' is a required field");
            }

            List messages = new ArrayList<>();
            if (map.containsKey("messages") && map.get("messages") != null) {
                Object messagesObj = map.get("messages");

                if (!(messagesObj instanceof List)) {
                    throw new RuntimeException("Notify result 'messages' should be of type list of string");
                }

                messages = (List) messagesObj;

                for (Object message : messages) {
                    if (!(message instanceof String)) {
                        throw new RuntimeException("Notify result 'message' should be of type string");
                    }
                }
            }
            Result result = new Result();
            if ("success".equalsIgnoreCase(status)) {
                result.withSuccessMessages(messages);
            } else {
                result.withErrorMessages(messages);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }
}
