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

package com.thoughtworks.go.config.update;

import java.util.*;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.util.json.JsonHelper;

public final class ConfigUpdateAjaxResponse {

    @Expose
    private Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
    @Expose
    private List<String> globalErrors = new ArrayList<>();
    @Expose
    private String message;
    @Expose
    private boolean isSuccessful;
    @Expose
    private String subjectIdentifier;
    @Expose
    private String redirectUrl;
    private int statusCode;

    private ConfigUpdateAjaxResponse(String id, int httpStatusCode, String message) {
        this.message = message;
        isSuccessful = true;
        this.subjectIdentifier = id;
        this.statusCode = httpStatusCode;
    }

    private ConfigUpdateAjaxResponse(String id, int httpStatusCode, String message, Map<String, List<String>> fieldErrors, List<String> globalErrors) {
        this.fieldErrors = fieldErrors;
        this.globalErrors = globalErrors;
        isSuccessful = false;
        this.subjectIdentifier = id;
        this.message = message;
        this.statusCode = httpStatusCode;
    }

    public static ConfigUpdateAjaxResponse success(String id, int httpStatusCode, String message) {
        return new ConfigUpdateAjaxResponse(id, httpStatusCode, message);
    }

    public static ConfigUpdateAjaxResponse failure(String id, int httpStatusCode, String message, Map<String, List<String>> fieldErrors, List<String> globalErrors) {
        return new ConfigUpdateAjaxResponse(id, httpStatusCode, message, fieldErrors, globalErrors);
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }

    public List<String> getGlobalErrors() {
        return globalErrors;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String toJson() {
        return JsonHelper.toJsonString(this);
    }

    public void setSubjectIdentifier(String subjectIdentifier) {
        this.subjectIdentifier = subjectIdentifier;
    }

    public String getSubjectIdentifier() {
        return subjectIdentifier;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
