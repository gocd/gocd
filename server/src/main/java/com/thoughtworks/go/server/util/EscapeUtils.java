/*
 * Copyright 2022 Thoughtworks, Inc.
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
package com.thoughtworks.go.server.util;

import org.apache.commons.text.StringEscapeUtils;

public class EscapeUtils {
    public String javascript(Object string) {
        return string == null ? null : StringEscapeUtils.escapeEcmaScript(String.valueOf(string));
    }

    public String html(Object string) {
        return string == null ? null : StringEscapeUtils.escapeHtml4(String.valueOf(string));
    }
}
