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
package com.thoughtworks.go.api;

import com.thoughtworks.go.config.exceptions.BadRequestException;
import spark.Request;

import static org.apache.commons.lang3.StringUtils.isBlank;

public interface HistoryMethods {
    String BAD_PAGE_SIZE_MSG = "The query parameter 'page_size', if specified must be a number between 10 and 100.";
    String BAD_CURSOR_MSG = "The query parameter '%s', if specified, must be a positive integer.";

    default Integer getPageSize(Request request) {
        Integer offset;
        try {
            offset = Integer.valueOf(request.queryParamOrDefault("page_size", "10"));
            if (offset < 10 || offset > 100) {
                throw new BadRequestException(BAD_PAGE_SIZE_MSG);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException(BAD_PAGE_SIZE_MSG);
        }
        return offset;
    }

    default long getCursor(Request request, String key) {
        long cursor = 0;
        try {
            String value = request.queryParams(key);
            if (isBlank(value)) {
                return cursor;
            }
            cursor = Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            throw new BadRequestException(String.format(BAD_CURSOR_MSG, key));
        }
        return cursor;
    }
}
