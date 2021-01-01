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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.regex.Pattern;

@ConfigTag(value = "ignore")
public class IgnoredFiles implements Serializable, Validatable {
    @ConfigAttribute(value = "pattern")
    private String pattern;
    private String processedPattern;
    private ConfigErrors configErrors = new ConfigErrors();
    private final Pattern punctuationRegex = Pattern.compile("\\p{Punct}");

    public static final String PATTERN = "pattern";
    public IgnoredFiles() {
    }

    public IgnoredFiles(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IgnoredFiles ignore = (IgnoredFiles) o;
        return !(pattern != null ? !pattern.equals(ignore.pattern) : ignore.pattern != null);
    }

    @Override
    public int hashCode() {
        return (pattern != null ? pattern.hashCode() : 1);
    }

    //our algorithom is replace the ** with ([^/]*/)* and replace the * with [^/]*
    public boolean shouldIgnore(MaterialConfig materialConfig, String name) {
        return materialConfig.matches(FilenameUtils.separatorsToUnix(name), processedPattern());
    }

    private String processedPattern() {
        if (this.processedPattern == null) {
            String[] parts = FilenameUtils.separatorsToUnix(pattern).split("/");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                part = escape(part);
                if ("**".equals(part)) {
                    sb.append(part.replace("**", "([^/]*/)*"));
                } else if (part.contains("*")) {
                    sb.append(part.replace("*", "[^/]*"));
                    sb.append("/");
                } else {
                    sb.append(part);
                    sb.append("/");
                }
            }
            this.processedPattern = StringUtils.removeEnd(sb.toString(), "/");
        }
        return this.processedPattern;
    }

    private String escape(String pattern) {
        StringBuilder result = new StringBuilder();
        for(char c : pattern.toCharArray()){
            if( c != '*' && punctuationRegex.matcher(String.valueOf(c)).matches()){
                result.append("\\").append(c);
            }else{
                result.append(String.valueOf(c));
            }
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "The ignore pattern is [" + pattern + "]";
    }

    public String getPattern() {
        return pattern;
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
}
