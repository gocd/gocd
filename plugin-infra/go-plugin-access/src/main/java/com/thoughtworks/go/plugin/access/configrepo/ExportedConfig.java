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
package com.thoughtworks.go.plugin.access.configrepo;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static java.lang.String.format;

public class ExportedConfig {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String X_EXPORT_FILENAME = "X-Export-Filename";

    private String content;
    private String contentType;
    private String filename;

    public ExportedConfig() {
    }

    private ExportedConfig(String content, Map<String, String> headers) {
        this.content = content;
        this.contentType = headers.get(CONTENT_TYPE);
        this.filename = headers.get(X_EXPORT_FILENAME);
    }

    public static ExportedConfig from(String content, Map<String, String> headers) {
        mustContainHeader(headers, CONTENT_TYPE);
        mustContainHeader(headers, X_EXPORT_FILENAME);
        return new ExportedConfig(content, headers);
    }

    private static void mustContainHeader(Map<String, String> headers, String key) {
        if (!headers.containsKey(key)) {
            throw new IllegalArgumentException(format("You must provide the response header: `%s`", key));
        }

        if (StringUtils.isBlank(headers.get(key))) {
            throw new IllegalArgumentException(format("Response header `%s` cannot be blank", key));
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
