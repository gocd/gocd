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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HtmlAnsiOutputStreamTest {

    @Test
    public void testEmpty() throws IOException {
        assertThatAnnotateIs("", "");
    }

    @Test
    public void testNoMarkup() throws IOException {
        assertThatAnnotateIs("line", "line");
    }

    @Test
    public void testClearBlank() throws IOException {
        assertThatAnnotateIs("\033[0m", "");
    }

    @Test
    public void testClear() throws IOException {
        assertThatAnnotateIs("\033[0m\033[K", "");
    }

    @Test
    public void testConceal() throws IOException {
        assertThatAnnotateIs(
                "there is concealed text here, \033[8mCONCEAL\033[0m, and it should vanish.",
                "there is concealed text here, <span class=\"invisible\">CONCEAL</span>, and it should vanish."
        );
    }

    @Test
    public void testBold() throws IOException {
        assertThatAnnotateIs("\033[1mhello world", "<span class=\"bold\">hello world</span>");
    }

    @Test
    public void testUnderline() throws IOException {
        assertThatAnnotateIs("\033[4mhello world", "<span class=\"underline\">hello world</span>");
    }

    @Test
    public void testInvisible() throws IOException {
        assertThatAnnotateIs("\033[8mhello world", "<span class=\"invisible\">hello world</span>");
    }

    @Test
    public void testUnderlineDouble() throws IOException {
        assertThatAnnotateIs("\033[21mhello world", "<span class=\"underline\">hello world</span>");
    }

    @Test
    public void testGreen() throws IOException {
        assertThatAnnotateIs("\033[32mhello world", "<span class=\"fg-green\">hello world</span>");
    }

    @Test
    public void testGreenCSS() throws IOException {
        assertThat(annotate("\033[32mhello world"),
                is("<span class=\"fg-green\">hello world</span>"));
    }


    @Test
    public void testGreenOnWhite() throws IOException {
        assertThat(
                annotate("\033[47;32mhello world"),
                is("<span class=\"bg-white\"><span class=\"fg-green\">hello world</span></span>"));
    }

    @Test
    public void testGreenOnWhiteCSS() throws IOException {
        assertThat(
                annotate("\033[47;32mhello world"),
                is("<span class=\"bg-white\"><span class=\"fg-green\">hello world</span></span>"));
    }

    @Test
    public void testResetForegroundColor() throws IOException {
        assertThatAnnotateIs("\033[32mtic\033[1mtac\033[39mtoe",
                "<span class=\"fg-green\">tic<span class=\"bold\">tac</span></span><span class=\"bold\">toe</span>");
    }

    @Test
    public void testResetBackgroundColor() throws IOException {
        assertThatAnnotateIs("\033[42mtic\033[1mtac\033[49mtoe",
                "<span class=\"bg-green\">tic<span class=\"bold\">tac</span></span><span class=\"bold\">toe</span>");
    }

    @Test
    public void testResetOnOpen() throws IOException {
        assertThat(
                annotate("\033[0;31;49mred\033[0m"),
                is("" +
                        "<span class=\"fg-red\">red" +
                        "</span>")
        );
    }

    @Test
    public void testUnicode() throws IOException {
        assertThatAnnotateIs("\033[32mmünchen", "<span class=\"fg-green\">münchen</span>");
    }

    @Test
    public void testJapanese() throws IOException {
        assertThatAnnotateIs("\033[32mこんにちは", "<span class=\"fg-green\">こんにちは</span>");
    }

    @Test
    public void testOverlapping() throws IOException {
        assertThatAnnotateIs("plain\033[32mgreen\033[1mboldgreen\033[4mulboldgreen\033[31mulboldred\033[22mulred\033[24mred",
                "plain" +
                        "<span class=\"fg-green\">" +                    // + green
                        "green" +
                        "<span class=\"bold\">" +                        // +bold (now green,bold)
                        "boldgreen" +
                        "<span class=\"underline\">" +                   // +underline (now green,bold,ul)
                        "ulboldgreen" +
                        "</span></span></span><span class=\"bold\"><span class=\"underline\">" +  // -green (now bold,ul)
                        "<span class=\"fg-red\">" +                    // +red (now bold,ul,red)
                        "ulboldred" +
                        "</span></span></span><span class=\"underline\"><span class=\"fg-red\">" +  // -bold (now ul,red)
                        "ulred" +
                        "</span></span><span class=\"fg-red\">" +         // -underline (now red)
                        "red" +
                        "</span>"                                         // close all.
        );
    }

    private void assertThatAnnotateIs(String ansi, String html) throws IOException {
        assertThat(annotate(ansi), is(html));
    }

    private String annotate(String text) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HtmlAnsiOutputStream ansi = new HtmlAnsiOutputStream(bos, new AnsiAttributeElement.Emitter() {
            public void emitHtml(String html) {
                try {
                    bos.write(html.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException("error emitting HTML", e);
                }
            }
        });
        ansi.write(text.getBytes());
        ansi.close();
        return bos.toString();
    }

}