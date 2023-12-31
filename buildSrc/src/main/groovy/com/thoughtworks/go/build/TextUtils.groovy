/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.build

import java.util.regex.Pattern
import java.util.stream.Collectors

class TextUtils {
  private static final Pattern NEW_LINES = Pattern.compile(/\r\n|\r|\n/)
  private static final Pattern NEW_LINE = Pattern.compile(/\n/)

  static String toPlatformLineSeparators(String input) {
    return NEW_LINES.matcher(input).replaceAll(System.lineSeparator())
  }

  static String indent(String input, String indentPrefix) {
    return NEW_LINE.splitAsStream(input).map { line -> indentPrefix + line }.collect(Collectors.joining("\n"))
  }
}
