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
package com.thoughtworks.go.server.web.ansi;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;

import static java.lang.String.format;

public class AnsiAttributeElement {

    public enum AnsiAttrType {
        DEFAULT, BOLD, UNDERLINE, INVISIBLE, FG, BG
    }

    public interface Emitter {
        void emitHtml(String html) throws IOException;
    }

    private final AnsiAttrType ansiAttrType;
    private final String name;
    private final String[] classes;

    public AnsiAttributeElement(AnsiAttrType ansiAttrType, String name, String... classes) {
        this.ansiAttrType = ansiAttrType;
        this.name = name;
        this.classes = classes;
    }

    public void emitOpen(Emitter emitter) throws IOException {
        final String openingTagHtml = format("<%s class=\"%s\">", name, StringUtils.join(classes, " "));
        emitter.emitHtml(openingTagHtml);
    }

    public void emitClose(Emitter emitter) throws IOException {
        String closingTagHtml = format("</%s>", name);
        emitter.emitHtml(closingTagHtml);
    }

    public boolean isSameAttributeAs(AnsiAttrType ansiAttrType) {
        return this.ansiAttrType == ansiAttrType;
    }
}
