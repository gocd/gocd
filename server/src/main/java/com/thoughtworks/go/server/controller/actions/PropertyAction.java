/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.controller.actions;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.server.controller.PropertiesController;

public class PropertyAction extends BasicRestfulAction {

    private PropertyAction(int statusCode, String message) {
        super(statusCode, message);
    }

    public static BasicRestfulAction created(Property property) {
        return new PropertyAction(SC_CREATED,
                "Property '" + property.getKey() + "' created with value '" + property.getValue() + "'");
    }

    public static BasicRestfulAction alreadySet(String propertyName) {
        return new PropertyAction(SC_CONFLICT, "Property '" + propertyName + "' is already set.");
    }

    public static RestfulAction instanceNotFound(String errorMessage) {
        return notFound(errorMessage);
    }

    public static RestfulAction propertyNotFound(String propertyName) {
        return notFound("Property '" + propertyName + "' not found.");
    }

    public static BasicRestfulAction retrieved(String value) {
        return new PropertyAction(SC_OK, value);
    }

    public static BasicRestfulAction propertyNameToLarge() {
        return new PropertyAction(SC_FORBIDDEN, PropertiesController.NAME_TOO_LONG);
    }

    public static BasicRestfulAction propertyValueToLarge() {
        return new PropertyAction(SC_FORBIDDEN, PropertiesController.VALUE_TOO_LONG);
    }

    public static BasicRestfulAction propertyContainsInvalidChars() {
        return new PropertyAction(SC_FORBIDDEN, PropertiesController.INVALID_VALUE);
    }
}
