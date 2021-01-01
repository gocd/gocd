/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.mercurial;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.ConsoleResult;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import org.joda.time.DateTime;

public class HgModificationSplitterTest {

    @Test public void shouldBeAbleToParseModifications() throws Exception {
        ConsoleResult result = new ConsoleResult(0, Arrays.asList(("<changeset>\n"
                + "<node>ca3ebb67f527c0ad7ed26b789056823d8b9af23f</node>\n"
                + "<author>cruise</author>\n"
                + "<date>Tue, 09 Dec 2008 18:56:14 +0800</date>\n"
                + "<desc>test</desc>\n"
                + "<files>\n"
                + "<modified>\n"
                + "<file>end2end/file</file>\n"
                + "</modified>\n"
                + "<added>\n"
                + "<file>end2end/file</file>\n"
                + "</added>\n"
                + "<deleted>\n"
                + "</deleted>\n"
                + "</files>\n"
                + "</changeset>").split("\n")), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        HgModificationSplitter splitter = new HgModificationSplitter(result);
        List<Modification> list = splitter.modifications();
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getModifiedTime(), is(new DateTime("2008-12-09T18:56:14+08:00").toDate()));
    }


}
