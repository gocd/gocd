/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.git.builder;

public class OutputFormatter {
    private final StringBuilder builder = new StringBuilder();

    public static OutputFormatter newFormatter(Class<?> type) {
        OutputFormatter formatter = new OutputFormatter();
        formatter.builder.append(String.format("- !!%s", type.getCanonicalName()));
        return formatter;
    }

    public OutputFormatter withCommitHash() {
        return appendWithIndent("commitHash", "%H", 2);
    }

    public OutputFormatter withAuthorName() {
        return appendWithIndent("authorName", "%an", 2);
    }

    public OutputFormatter withAuthorEmail() {
        return appendWithIndent("authorEmail", "%ae", 2);
    }

    public OutputFormatter withDate() {
        return appendWithIndent("date", "%ai", 2);
    }

    public OutputFormatter withSubject() {
        return appendWithIndent("subject", "|-2%n%w(,4,4)%s", 2);
    }

    public OutputFormatter withRawBody() {
        return appendWithIndent("rawBody", "|-2%n%w(,4,4)%B", 2);
    }

    public OutputFormatter withAdditionalInfo() {
        return appendWithIndent("additionalInfo:", 2)
                .appendWithIndent("signed", "%G?", 4)
                .appendWithIndent("signerName", "%GS", 4)
                .appendWithIndent("signingKey", "%GK", 4)
                .appendWithIndent("signingMessage", "|-%n%w(,6,6)%GG", 4)
                .appendWithIndent("committerName", "%cn", 4)
                .appendWithIndent("committerEmail", "%ce", 4)
                .appendWithIndent("commitDate", "%ci", 4);
    }

    public String format() {
        return builder.toString();
    }

    private OutputFormatter appendWithIndent(String fieldName, String selector, int indent) {
        return appendWithIndent(String.format("%s: %s", fieldName, selector), indent);
    }

    private OutputFormatter appendWithIndent(String str, int indent) {
        builder.append("\n").append(indentation(indent)).append(str);
        return this;
    }

    private String indentation(int spaceCount) {
        return String.format("%%w(,%d,%d)", spaceCount, spaceCount);
    }
}
