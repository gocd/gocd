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

package com.thoughtworks.go.config.materials.perforce;

import static org.junit.Assert.assertThat;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;

public class P4MaterialViewTest {
    private static final String CLIENT_NAME = "cruise-ccedev01-mingle";

    @Test public void shouldReplaceClientNameOnView() throws Exception {
        P4MaterialView view = new P4MaterialView("//depot/... //something/...");
        assertThat(view.viewUsing(CLIENT_NAME), containsString("//depot/... //cruise-ccedev01-mingle/..."));
    }

    @Test public void shouldNotrelyOnDepotInTheViews() throws Exception {
        P4MaterialView view = new P4MaterialView("//SZOPT/... //MDYNYCMDCDEV03/SZOPT/...");
        assertThat(view.viewUsing(CLIENT_NAME), containsString("//SZOPT/... //cruise-ccedev01-mingle/SZOPT/..."));
    }

    @Test public void shouldSetCorrectTabs() throws Exception {
        String from = "\n"
                + "    //depot/dir1/... //cws/...\n"
                + "//depot/dir1/... //cws/...\n"
                + "//foo/dir1/... //cws/...\n"
                + "//foo/dir2/... //cws/foo2/...\n"
                + "    //depot/dir1/... //cws/...\r\n"
                + "    //depot/dir1/...    //cws/...\n"
                + "\t\t//depot/rel1/... //cws/release1/...";
        String to = "\n"
                + "\t//depot/dir1/... //" + CLIENT_NAME + "/...\n"
                + "\t//depot/dir1/... //" + CLIENT_NAME + "/...\n"
                + "\t//foo/dir1/... //" + CLIENT_NAME + "/...\n"
                + "\t//foo/dir2/... //" + CLIENT_NAME + "/foo2/...\n"
                + "\t//depot/dir1/... //" + CLIENT_NAME + "/...\n"
                + "\t//depot/dir1/... //" + CLIENT_NAME + "/...\n"
                + "\t//depot/rel1/... //" + CLIENT_NAME + "/release1/...";
        assertMapsTo(from, to);
    }

    @Test public void shouldSupportExcludedAndIncludeMappings() throws Exception {
        String from = "//depot/dir1/... //cws/...\n"
                + "-//depot/dir1/exclude/... //cws/dir1/exclude/...\n"
                + "+//depot/dir1/include/... //cws/dir1/include/...";
        String to = "\n"
                + "\t//depot/dir1/... //" + CLIENT_NAME + "/...\n"
                + "\t-//depot/dir1/exclude/... //" + CLIENT_NAME + "/dir1/exclude/...\n"
                + "\t+//depot/dir1/include/... //" + CLIENT_NAME + "/dir1/include/...";
        assertMapsTo(from, to);
    }

    @Test public void shouldSupportMappingsWithSpecialCharacters() throws Exception {
        String from = "//depot/dir1/old.* //cws/renamed/new.*\n"
                + "//depot/dir1/%1.%2 //cws/dir1/%2.%1\n"
                + "\t//foobar/dir1/%1.%2 //cws/dir1/%2.%1\n"
                + "\"-//depot/with spaces/...\" \"//cws/with spaces/...\"\n\n";
        String to = "\n"
                + "\t//depot/dir1/old.* //" + CLIENT_NAME + "/renamed/new.*\n"
                + "\t//depot/dir1/%1.%2 //" + CLIENT_NAME + "/dir1/%2.%1\n"
                + "\t//foobar/dir1/%1.%2 //" + CLIENT_NAME + "/dir1/%2.%1\n"
                + "\t\"-//depot/with spaces/...\" \"//" + CLIENT_NAME + "/with spaces/...\"\n\n";
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
