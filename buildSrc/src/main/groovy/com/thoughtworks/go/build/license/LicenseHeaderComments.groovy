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

package com.thoughtworks.go.build.license

import dev.yumi.gradle.licenser.api.comment.CStyleHeaderComment
import dev.yumi.gradle.licenser.api.comment.HeaderComment
import dev.yumi.gradle.licenser.api.comment.XmlStyleHeaderComment

class PrefixedHeaderComment implements HeaderComment, Serializable {
  private final String startComment
  private final String endComment
  private final String prefix

  PrefixedHeaderComment(String startComment, String endComment = startComment, String prefix) {
    this.startComment = startComment
    this.endComment = endComment
    this.prefix = prefix
  }

  @Override
  Result readHeaderComment(String source) {
    def separator = extractLineSeparator(source)
    def startComment = "$startComment$separator"
    def endComment = "$endComment$separator"

    def start = source.indexOf(startComment)
    def end = source.indexOf(endComment, start + startComment.length())
    if (start < 0 || end < 0) return new Result(0, 0, null, separator);

    def linePrefixMatcher = "^$prefix?"
    def lines = source.substring(start, end + endComment.length()).split(separator).collect { it.replaceAll(linePrefixMatcher, "") }
    if (lines.size > 2) {
      lines.removeFirst()
      lines.removeLast()
    }
    new Result(start, end + endComment.length(), lines, separator)
  }

  @Override
  String writeHeaderComment(List<String> header, String separator) {
    return ([startComment] + header.collect { "$prefix$it".stripTrailing() } + [endComment]).join(separator) + separator
  }
}

class ScriptStyleHeaderComment extends PrefixedHeaderComment {
  ScriptStyleHeaderComment() {
    super('#', '#\n', '# ')
  }
}

class SkipLinesCStyleHeaderComment implements HeaderComment, Serializable {
  private final String skipLinesPattern

  SkipLinesCStyleHeaderComment(String skipLinesPattern) {
    this.skipLinesPattern = skipLinesPattern
  }

  @Override
  Result readHeaderComment(String source) {
    if (!source.contains('\n')) return new Result(0, 0, [source], '\n')
    return CStyleHeaderComment.INSTANCE.readHeaderComment(source.replaceAll(skipLinesPattern, ""))
  }

  @Override
  String writeHeaderComment(List<String> header, String separator) {
    return CStyleHeaderComment.INSTANCE.writeHeaderComment(header, separator)
  }
}

class TildeXmlStyleHeaderComment implements HeaderComment, Serializable {
  @Override
  Result readHeaderComment(String source) {
    if (!source.contains('\n')) return new Result(0, 0, [source], '\n')

    Result interim = XmlStyleHeaderComment.INSTANCE.readHeaderComment(source.replaceAll("(?m)^\\s*<\\?xml.*>\\s*", ""))
    return new Result(interim.start(), interim.end(), interim.existing().collect { it.replaceAll("^~( )?", "") }, interim.separator())
  }

  @Override
  String writeHeaderComment(List<String> header, String separator) {
    return (["<!--"] + (header.collect { "  ~ $it".stripTrailing() }) + ["  -->"]).join(separator)
  }
}