/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.util.json.JsonAware;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static java.lang.String.valueOf;

public class ValidationBean implements JsonAware {
    private final boolean isValid;
    private final String message;

    private ValidationBean(boolean valid, String message) {
        this.isValid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getError() {
        return message.replaceFirst("(.)*Exception:(\\s)+", "");
    }

    @Deprecated
    /*
        * Used in tests
     */
    public String getMessage() {
        return message;
    }

    /**
     * @deprecated This method is obsolete
     */
    public Map<String, Object> toJson() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("isValid", valueOf(isValid));
        jsonMap.put(ERROR_FOR_JSON, getError());
        return jsonMap;
    }

    public static ValidationBean valid() {
        return new ValidationBean(true, "");
    }

    public static ValidationBean notValid(String message) {
        return new ValidationBean(false, message);
    }

    public static ValidationBean notValid(Throwable e) {
        return new ValidationBean(false, e.getMessage());
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ValidationBean that = (ValidationBean) o;

        if (isValid != that.isValid) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (isValid ? 1 : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
