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

import org.fusesource.jansi.AnsiOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Stack;

import static com.thoughtworks.go.server.web.ansi.AnsiAttributeElement.AnsiAttrType;

public class HtmlAnsiOutputStream extends AnsiOutputStream {
    private final AnsiAttributeElement.Emitter emitter;
    private final ArrayList<AnsiAttributeElement> openTags = new ArrayList<AnsiAttributeElement>();

    public HtmlAnsiOutputStream(final OutputStream os, final AnsiAttributeElement.Emitter emitter) {
        super(os);
        this.emitter = emitter;
    }

    private void openTag(AnsiAttributeElement tag) throws IOException {
        openTags.add(tag);
        tag.emitOpen(emitter);
    }

    private void closeOpenTags(AnsiAttrType until) throws IOException {
        while (!openTags.isEmpty()) {
            int index = openTags.size() - 1;
            if (until != null && openTags.get(index).isSameAttributeAs(until))
                break;

            openTags.remove(index).emitClose(emitter);
        }
    }

    /* ANSI Attributes, unlike HTML elements, can overlap.
     * This method implements the unwinding of elements up until the element of the requested type.
     * That last element is just closed, while the others before it have to be reopened.
     *
     * Nothing happens when trying to close an element which has never been opened (i.e. "bold off" when there
     * was no "bold" before). */
    private void closeTagOfType(AnsiAttrType ansiAttrType) throws IOException {
        int sameTypePos;

        // Search for an element with matching type.
        for (sameTypePos = openTags.size(); sameTypePos > 0; sameTypePos--) {
            if (openTags.get(sameTypePos - 1).isSameAttributeAs(ansiAttrType)) {
                break;
            }
        }

        if (sameTypePos == 0) {
            // No need to unwind anything if the attribute has not been touched yet.
            return;
        }

        Stack<AnsiAttributeElement> reopen = new Stack<AnsiAttributeElement>();

        // Unwind ...
        for (int unwindAt = openTags.size(); unwindAt > sameTypePos; unwindAt--) {
            AnsiAttributeElement tag = openTags.remove(unwindAt - 1);
            tag.emitClose(emitter);
            reopen.push(tag);
        }

        // ... close matching element ...
        AnsiAttributeElement offendingTag = openTags.remove(sameTypePos - 1);
        offendingTag.emitClose(emitter);

        // ... reopen.
        while (!reopen.isEmpty()) {
            AnsiAttributeElement tag = reopen.pop();
            tag.emitOpen(emitter);
            openTags.add(tag);
        }
    }

    @Override
    public void close() throws IOException {
        closeOpenTags(null);
        super.close();
    }

    @Override
    protected void processSetAttribute(int attribute) throws IOException {
        switch (attribute) {
            case ATTRIBUTE_CONCEAL_OFF:
                closeTagOfType(AnsiAttrType.INVISIBLE);
                break;
            case ATTRIBUTE_CONCEAL_ON:
                closeTagOfType(AnsiAttrType.INVISIBLE);
                openTag(new AnsiAttributeElement(AnsiAttrType.INVISIBLE, "span", "invisible"));
                break;
            case ATTRIBUTE_INTENSITY_BOLD:
                closeTagOfType(AnsiAttrType.BOLD);
                openTag(new AnsiAttributeElement(AnsiAttrType.BOLD, "span", "bold"));
                break;
            case ATTRIBUTE_INTENSITY_NORMAL:
                closeTagOfType(AnsiAttrType.BOLD);
                break;
            case ATTRIBUTE_UNDERLINE:
                closeTagOfType(AnsiAttrType.UNDERLINE);
                openTag(new AnsiAttributeElement(AnsiAttrType.UNDERLINE, "span", "underline"));
                break;
            case ATTRIBUTE_UNDERLINE_DOUBLE:
                closeTagOfType(AnsiAttrType.UNDERLINE);
                openTag(new AnsiAttributeElement(AnsiAttrType.UNDERLINE, "span", "underline"));
                break;
            case ATTRIBUTE_UNDERLINE_OFF:
                closeTagOfType(AnsiAttrType.UNDERLINE);
                break;
        }
    }

    @Override
    protected void processAttributeRest() throws IOException {
        closeOpenTags(AnsiAttrType.DEFAULT);
    }

    @Override
    protected void processSetForegroundColor(int color) throws IOException {
        closeTagOfType(AnsiAttrType.FG);
        String style = "fg-" + getColor(color);
        openTag(new AnsiAttributeElement(AnsiAttrType.FG, "span", style));
    }

    @Override
    protected void processSetBackgroundColor(int color) throws IOException {
        closeTagOfType(AnsiAttrType.BG);
        String style = "bg-" + getColor(color);
        openTag(new AnsiAttributeElement(AnsiAttrType.BG, "span", style));
    }

    @Override
    protected void processDefaultTextColor() throws IOException {
        closeTagOfType(AnsiAttrType.FG);
    }

    @Override
    protected void processDefaultBackgroundColor() throws IOException {
        closeTagOfType(AnsiAttrType.BG);
    }

    private String getColor(int color) {
        switch (color) {
            case BLACK:
                return "black";
            case RED:
                return "red";
            case GREEN:
                return "green";
            case YELLOW:
                return "yellow";
            case BLUE:
                return "blue";
            case MAGENTA:
                return "magenta";
            case CYAN:
                return "cyan";
            case WHITE:
                return "white";
            default:
                return "white";
        }
    }
}