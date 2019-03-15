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

package com.thoughtworks.go.util.command;

import com.thoughtworks.go.config.ConfigAttributeValue;

@ConfigAttributeValue(fieldName = "secret")
public class PasswordArgument extends CommandArgument implements SecretString {
    private String secret;

    public PasswordArgument(String secret) {
        this.secret = secret;
    }

    //TODO: Resolve is this is set to use secret param
    public String forCommandLine() {
        return secret == null ? "" : secret;
    }

    public String forDisplay() {
        return secret == null ? null : "******";
    }

    @Override
    public String rawUrl() {
        return secret == null ? "" : secret;
    }
}
