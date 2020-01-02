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
package com.thoughtworks.go.config.materials;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;

@ConfigTag("filter")
@ConfigCollection(IgnoredFiles.class)
public class Filter extends LinkedHashSet<IgnoredFiles> implements Validatable {
    private ConfigErrors configErrors = new ConfigErrors();

    public Filter() {
    }

    public Filter(IgnoredFiles... ignores) {
        for (IgnoredFiles ignore : ignores) {
            this.add(ignore);
        }
    }

    public Filter(List<IgnoredFiles> ignores) {
        super(ignores);
    }

    public static Filter create(String... files) {
        Filter f = new Filter();
        for (String file : files) {
            f.add(new IgnoredFiles(file));
        }
        return f;
    }

    public List<String> ignoredFileNames() {
        List<String> files = new ArrayList<>();
        for (IgnoredFiles ignoredFile : this) {
            files.add(ignoredFile.getPattern());
        }
        return files;
    }

    public String getStringForDisplay() {
        if (isEmpty()) {
            return "";
        }
        StringBuilder display = new StringBuilder();
        for (IgnoredFiles ignoredFiles : this) {
            display.append(ignoredFiles.getPattern()).append(",");
        }
        return display.substring(0, display.length() - 1);
    }

    public static Filter fromDisplayString(String displayString) {
        Filter filter = new Filter();
        String[] ignoredPatterns = displayString.split(",");
        for (String ignoredPattern : ignoredPatterns) {
            filter.add(new IgnoredFiles(ignoredPattern.trim()));
        }
        return filter;
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean shouldNeverIgnore() {
        return false;
    }
}
