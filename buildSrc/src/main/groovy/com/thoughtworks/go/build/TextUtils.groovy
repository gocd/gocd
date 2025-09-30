/*
 * Copyright Thoughtworks, Inc.
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

import org.gradle.internal.hash.Hashing

import java.util.regex.Pattern
import java.util.stream.Collectors

class TextUtils {
  private static final def NEW_LINES = Pattern.compile(/\r\n|\r|\n/)
  private static final def NEW_LINE = Pattern.compile(/\n/)

  static toPlatformLineSeparators(String input) {
    return NEW_LINES.matcher(input).replaceAll(System.lineSeparator())
  }

  static indent(String input, String indentPrefix) {
    return NEW_LINE.splitAsStream(input).map { line -> indentPrefix + line }.collect(Collectors.joining("\n"))
  }

  static def substringAfter(String str, String find) {
    if (!str) return str
    if (!find) return ''
    def pos = str.indexOf(find)
    return pos == -1 ? '' : str.substring(pos + find.length())
  }

  static sha256Hex(InputStream input) {
    return Hashing.sha256().hashStream(input).toString()
  }

  static md5Hex(String input) {
    return Hashing.md5().hashString(input).toString()
  }
}
