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
package com.thoughtworks.go.domain.materials.mercurial;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.ConsoleResult;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HgModificationSplitterTest {

    @Test
    public void shouldBeAbleToParseModifications() {
        ConsoleResult result = new ConsoleResult(0, List.of(("""
                <changeset>
                <node>ca3ebb67f527c0ad7ed26b789056823d8b9af23f</node>
                <author>cruise</author>
                <date>Tue, 09 Dec 2008 18:56:14 +0800</date>
                <desc>test</desc>
                <files>
                <modified>
                <file>end2end/file</file>
                </modified>
                <added>
                <file>end2end/file</file>
                </added>
                <deleted>
                </deleted>
                </files>
                </changeset>""").split("\n")), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        HgModificationSplitter splitter = new HgModificationSplitter(result);
        List<Modification> list = splitter.modifications();
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getModifiedTime(), is(new DateTime("2008-12-09T18:56:14+08:00").toDate()));
    }


}
