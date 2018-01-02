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

package com.thoughtworks.go.plugin.api.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents result of a plugin method invocation
 */
@Deprecated
//Will be moved to internal scope
public class Result {

    private Status status = Status.SUCCESS;
    private List<String> messages = new ArrayList<>();

    /**
     * Checks if result is successful
     * @return true if result successful
     */
    public boolean isSuccessful() {
        return Status.SUCCESS == status;
    }

    /**
     * Gets error messages associated with result
     * @return error messages associated with result
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * Creates instance of result with specified errors
     * @param errors the errors with which instance should be created
     * @return created instance
     */
    public Result withErrorMessages(String... errors) {
        return withErrorMessages(Arrays.asList(errors));
    }

    /**
     * Creates instance of result with specified errors
     * @param errorList the errors with which instance should be created
     * @return created instance
     */
    public Result withErrorMessages(List<String> errorList) {
        return withStatusAndMessages(Status.FAILURE, errorList);
    }

    /**
     * Creates instance of result with specified success messages
     * @param successMessages the success messages with which instance should be created
     * @return created instance
     */
    public Result withSuccessMessages(String... successMessages) {
        List<String> msgList = Arrays.asList(successMessages);
        return withSuccessMessages(msgList);
    }

    /**
     * Creates instance of result with specified success messages
     * @param successMessages the success messages with which instance should be created
     * @return created instance
     */
    public Result withSuccessMessages(List<String> successMessages) {
        return withStatusAndMessages(Status.SUCCESS, successMessages);
    }

    private Result withStatusAndMessages(Status status, List<String> messages) {
        this.status = status;
        this.messages.addAll(messages);
        return this;
    }

    /**
     * Formats the messages in the result to a suitable form
     * so that it can be used in user interface.
     *
     * @return a string containing the formatted message.
     */
    public String getMessagesForDisplay() {
        if(messages.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String message : messages) {
            stringBuilder.append(message);
            stringBuilder.append("\n");
        }
        String tempStr = stringBuilder.toString();
        stringBuilder.deleteCharAt(tempStr.length() - 1);
        return stringBuilder.toString();
    }

    private enum Status {
        SUCCESS,
        FAILURE,
    }
}
