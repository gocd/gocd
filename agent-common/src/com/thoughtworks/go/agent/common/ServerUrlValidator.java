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

package com.thoughtworks.go.agent.common;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.net.MalformedURLException;
import java.net.URL;

public class ServerUrlValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        URL serverUrl;
        try {
            serverUrl = new URL(value);
        } catch (MalformedURLException e) {
            throw new ParameterException(name + " is not a valid url");
        }

        if (!serverUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new ParameterException(name + " must be an HTTPS url and must begin with https://");
        }

        if (!serverUrl.toString().endsWith("/go") && !serverUrl.toString().endsWith("/go/")) {
            throw new ParameterException(name + " must end with '/go' (https://localhost:8154/go)");
        }
    }
}
