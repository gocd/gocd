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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.exceptions.BadRequestException;

import static java.lang.String.format;

public class ServiceConstants {
    public static class History {
        public static final String BAD_CURSOR_MSG = "The query parameter '%s', if specified, must be a positive integer.";

        static boolean validateCursor(Long cursor, String key) {
            if (cursor == 0) return false;
            if (cursor < 0) {
                throw new BadRequestException(format(BAD_CURSOR_MSG, key));
            }
            return true;
        }
    }
}
