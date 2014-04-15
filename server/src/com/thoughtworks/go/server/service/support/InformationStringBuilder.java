/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.support;

class InformationStringBuilder {
    private StringBuilder builder;

    public InformationStringBuilder(StringBuilder builder) {
        this.builder = builder;
    }

    public InformationStringBuilder append(Object message) {
        builder.append(message);
        return this;
    }

    public InformationStringBuilder addSection(String title) {
        builder.append("\n\n").append(str("=", title.length())).append("\n").append(title).append("\n").append(str("=", title.length())).append("\n\n");
        return this;
    }

    public InformationStringBuilder addSubSection(String title) {
        builder.append("\n").append(title).append("\n").append(str("-", title.length())).append("\n");
        return this;
    }

    public String value() {
        return builder.toString();
    }

    private String str(String charToUse, int length) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < length; i++) {
            str.append(charToUse);
        }
        return str.toString();
    }
}
