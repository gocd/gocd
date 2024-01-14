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
package com.thoughtworks.go.config.materials.perforce;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class P4MaterialViewTest {
    private static final String CLIENT_NAME = "cruise-ccedev01-mingle";

    @Test
    public void shouldReplaceClientNameOnView() {
        P4MaterialView view = new P4MaterialView("//depot/... //something/...");
        assertThat(view.viewUsing(CLIENT_NAME), containsString("//depot/... //cruise-ccedev01-mingle/..."));
    }

    @Test
    public void shouldNotRelyOnDepotInTheViews() {
        P4MaterialView view = new P4MaterialView("//SZOPT/... //MDYNYCMDCDEV03/SZOPT/...");
        assertThat(view.viewUsing(CLIENT_NAME), containsString("//SZOPT/... //cruise-ccedev01-mingle/SZOPT/..."));
    }

    @Test
    public void shouldSetCorrectTabs() {
        String from = """

                    //depot/dir1/... //cws/...
                //depot/dir1/... //cws/...
                //foo/dir1/... //cws/...
                //foo/dir2/... //cws/foo2/...
                    //depot/dir1/... //cws/...\r
                    //depot/dir1/...    //cws/...
                \t\t//depot/rel1/... //cws/release1/...""";
        String to = ("""

                \t//depot/dir1/... //%s/...
                \t//depot/dir1/... //%s/...
                \t//foo/dir1/... //%s/...
                \t//foo/dir2/... //%s/foo2/...
                \t//depot/dir1/... //%s/...
                \t//depot/dir1/... //%s/...
                \t//depot/rel1/... //%s/release1/...""").formatted(CLIENT_NAME, CLIENT_NAME, CLIENT_NAME, CLIENT_NAME, CLIENT_NAME, CLIENT_NAME, CLIENT_NAME);
        assertMapsTo(from, to);
    }

    @Test
    public void shouldSupportExcludedAndIncludeMappings() {
        String from = """
                //depot/dir1/... //cws/...
                -//depot/dir1/exclude/... //cws/dir1/exclude/...
                +//depot/dir1/include/... //cws/dir1/include/...""";
        String to = ("""

                \t//depot/dir1/... //%s/...
                \t-//depot/dir1/exclude/... //%s/dir1/exclude/...
                \t+//depot/dir1/include/... //%s/dir1/include/...""").formatted(CLIENT_NAME, CLIENT_NAME, CLIENT_NAME);
        assertMapsTo(from, to);
    }

    @Test
    public void shouldSupportMappingsWithSpecialCharacters() {
        String from = """
                //depot/dir1/old.* //cws/renamed/new.*
                //depot/dir1/%1.%2 //cws/dir1/%2.%1
                \t//foobar/dir1/%1.%2 //cws/dir1/%2.%1
                "-//depot/with spaces/..." "//cws/with spaces/..."

                """;
        String to = ("""

                \t//depot/dir1/old.* //%s/renamed/new.*
                \t//depot/dir1/%%1.%%2 //%s/dir1/%%2.%%1
                \t//foobar/dir1/%%1.%%2 //%s/dir1/%%2.%%1
                \t"-//depot/with spaces/..." "//%s/with spaces/..."

                """).formatted(CLIENT_NAME, CLIENT_NAME, CLIENT_NAME, CLIENT_NAME);
        assertMapsTo(from, to);
    }

    @Test
    public void shouldAddErrorsToTheErrorCollection() {
        P4MaterialView view = new P4MaterialView("//depot/... //something/...");
        view.addError("key", "some error");
        assertThat(view.errors().on("key"), is("some error"));
    }

    private void assertMapsTo(String from, String to) {
        P4MaterialView view = new P4MaterialView(from);
        String actual = view.viewUsing(CLIENT_NAME);
        assertThat(actual, is(to));
    }
}
