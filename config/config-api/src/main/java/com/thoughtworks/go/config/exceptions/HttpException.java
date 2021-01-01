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
package com.thoughtworks.go.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Objects;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringEscapeUtils.escapeXml11;

public abstract class HttpException extends RuntimeException {
    private final HttpStatus status;

    protected HttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String asXML() {
        String tag = status.name().toLowerCase().replaceAll("_", "-");
        return format("<%s>\n  <message>%s</message>\n</%s>\n", tag, escapeXml11(getMessage()), tag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpException that = (HttpException) o;
        return status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }
}
