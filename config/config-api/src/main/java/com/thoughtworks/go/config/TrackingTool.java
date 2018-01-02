/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import java.util.Map;

import com.thoughtworks.go.domain.CommentRenderer;
import com.thoughtworks.go.domain.DefaultCommentRenderer;
import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.commons.lang.StringUtils;

@ConfigTag("trackingtool")
public class TrackingTool implements ParamsAttributeAware, Validatable, CommentRenderer {
    @ConfigAttribute(value = "link", optional = false)
    private String link = "";
    @ConfigAttribute(value = "regex", optional = false)
    private String regex = "";

    public static final String LINK = "link";
    public static final String REGEX = "regex";
    private ConfigErrors configErrors = new ConfigErrors();


    public TrackingTool() {
    }

    public TrackingTool(String link, String regex) {
        this.link = link;
        this.regex = regex;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String render(String text) {
        return new DefaultCommentRenderer(link, regex).render(text);
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes == null) return;
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(LINK)) {
            link = (String) attributeMap.get(LINK);
        }
        if (attributeMap.containsKey(REGEX)) {
            regex = (String) attributeMap.get(REGEX);
        }
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        if (StringUtils.isEmpty(link)) {
            configErrors.add(LINK, "Link should be populated");
        }
        if (StringUtils.isEmpty(regex)) {
            configErrors.add(REGEX, "Regex should be populated");
        }
        if (!link.contains("${ID}")) {
            configErrors.add(LINK, "Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time.");
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public static TrackingTool createTrackingTool(Map attributes) {
        TrackingTool trackingTool = new TrackingTool();
        trackingTool.setConfigAttributes(attributes);
        return trackingTool;
    }

    public boolean isDefined() {
        return !link.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TrackingTool that = (TrackingTool) o;
        if (link != null ? !link.equals(that.link) : that.link != null) {
            return false;
        }
        return !(regex != null ? !regex.equals(that.regex) : that.regex != null);

    }

    @Override
    public int hashCode() {
        int result = link != null ? link.hashCode() : 0;
        result = 31 * result + (regex != null ? regex.hashCode() : 0);
        return result;
    }
}
