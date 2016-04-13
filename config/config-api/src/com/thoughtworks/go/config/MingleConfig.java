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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.CommentRenderer;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.DefaultCommentRenderer;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.XmlUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @understands mingle project for pipeline
 */
@ConfigTag("mingle")
public class MingleConfig implements ParamsAttributeAware, Validatable, CommentRenderer {
    @ConfigAttribute(value = "baseUrl", optional = false)
    private String baseUrl;

    @ConfigAttribute(value = "projectIdentifier", optional = false)
    private String projectIdentifier;

    @ConfigSubtag
    private MqlCriteria mqlCriteria = new MqlCriteria();

    private final ConfigErrors configErrors = new ConfigErrors();

    private static final String DELIMITER = "/";

    public static final String BASE_URL = "baseUrl";
    public static final String PROJECT_IDENTIFIER = "projectIdentifier";
    public static final String MQL_GROUPING_CONDITIONS = "mqlCriteria";

    private static final String MINGLE_URL_PATTERN = "https://.+";
    private static final Pattern MINGLE_URL_PATTERN_REGEX = Pattern.compile(String.format("^(%s)$", MINGLE_URL_PATTERN));
    private static final String PROJECT_IDENTIFIER_PATTERN = "[^\\s]+";
    private static final Pattern PROJECT_IDENTIFIER_PATTERN_REGEX = Pattern.compile(String.format("^(%s)$", PROJECT_IDENTIFIER_PATTERN));

    public MingleConfig() {
    }

    public MingleConfig(String baseUrl, String projectIdentifier, String mql) {
        this(baseUrl, projectIdentifier);
        this.mqlCriteria = new MqlCriteria(mql);
    }

    public MingleConfig(String baseUrl, String projectIdentifier) {
        this.baseUrl = baseUrl;
        this.projectIdentifier = projectIdentifier;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        if (isDefined() && XmlUtils.doesNotMatchUsingXsdRegex(MINGLE_URL_PATTERN_REGEX, baseUrl)) {
            configErrors.add(BASE_URL, "Should be a URL starting with https://");
        }
        if (projectIdentifier != null && XmlUtils.doesNotMatchUsingXsdRegex(PROJECT_IDENTIFIER_PATTERN_REGEX, projectIdentifier)) {
            configErrors.add(PROJECT_IDENTIFIER, "Should be a valid mingle identifier.");
        }
    }

    public boolean isDefined() {
        return baseUrl != null;
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public String urlFor(String path) throws MalformedURLException, URISyntaxException {
        URIBuilder baseUri = new URIBuilder(baseUrl);
        String originalPath = baseUri.getPath();
        if (originalPath == null) {
            originalPath = "";
        }

        if (originalPath.endsWith(DELIMITER) && path.startsWith(DELIMITER)) {
            path = path.replaceFirst(DELIMITER, "");
        }

        return baseUri.setPath(originalPath + path).toString();
    }

    public String getProjectIdentifier() {
        return projectIdentifier;
    }

    public void setProjectIdentifier(String projectIdentifier) {
        this.projectIdentifier = projectIdentifier;
    }

    public String getQuotedMql() {
        String mqlString = mqlCriteria.equals(new MqlCriteria()) ? "" : mqlCriteria.getMql();
        return StringUtil.quoteJavascriptString(mqlString);
    }

    public String getQuotedProjectIdentifier() {
        return StringUtil.quoteJavascriptString(projectIdentifier);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public MqlCriteria getMqlCriteria() {
        return mqlCriteria;
    }

    public void setMqlCriteria(String mql) {
        this.mqlCriteria = new MqlCriteria(mql);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MingleConfig that = (MingleConfig) o;

        if (baseUrl != null ? !baseUrl.equals(that.baseUrl) : that.baseUrl != null) {
            return false;
        }
        if (mqlCriteria != null ? !mqlCriteria.equals(that.mqlCriteria) : that.mqlCriteria != null) {
            return false;
        }
        if (projectIdentifier != null ? !projectIdentifier.equals(that.projectIdentifier) : that.projectIdentifier != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = baseUrl != null ? baseUrl.hashCode() : 0;
        result = 31 * result + (projectIdentifier != null ? projectIdentifier.hashCode() : 0);
        result = 31 * result + (mqlCriteria != null ? mqlCriteria.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("baseUrl", baseUrl).
                append("projectName", projectIdentifier).
                append("mqlCriteria", mqlCriteria).
                toString();
    }

    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(BASE_URL)) {
            baseUrl = (String) attributeMap.get(BASE_URL);
        }
        if (attributeMap.containsKey(PROJECT_IDENTIFIER)) {
            projectIdentifier = (String) attributeMap.get(PROJECT_IDENTIFIER);
        }
        if (attributeMap.containsKey(MQL_GROUPING_CONDITIONS)) {
            mqlCriteria = (mqlCriteria == null) ? new MqlCriteria() : mqlCriteria;
            mqlCriteria.setConfigAttributes(attributeMap.get(MQL_GROUPING_CONDITIONS));
        }
    }

    public static MingleConfig create(Object attributes) {
        MingleConfig mingleConfig = new MingleConfig();
        mingleConfig.setConfigAttributes(attributes);
        return mingleConfig;
    }

    public boolean isDifferentFrom(MingleConfig other) {
        if (baseUrl != null ? !baseUrl.equals(other.baseUrl) : other.baseUrl != null) {
            return false;
        }
        if (projectIdentifier != null ? !projectIdentifier.equals(other.projectIdentifier) : other.projectIdentifier != null) {
            return false;
        }
        return true;
    }

    public String render(String text) {
        try {
            String urlPart = urlFor(String.format("/projects/%s/cards/", projectIdentifier));
            return new DefaultCommentRenderer(urlPart + "${ID}", "#(\\d+)").render(text);
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Could not construct the URL to generate the link.", e);
        }
    }
}
