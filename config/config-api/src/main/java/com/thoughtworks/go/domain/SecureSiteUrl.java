/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@ConfigTag("secureSiteUrl")
public class SecureSiteUrl extends ServerSiteUrlConfig implements Validatable {
    private ConfigErrors errors = new ConfigErrors();

    public SecureSiteUrl() {
    }

    public SecureSiteUrl(String url) {
        super(url);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (StringUtils.isBlank(url)) {
            return;
        }
        if (!Pattern.matches("(https://.+)?", url)) {
            errors().add("secureSiteUrl", String.format("Invalid format for secure site url. '%s' must start with https", url));
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }
}
