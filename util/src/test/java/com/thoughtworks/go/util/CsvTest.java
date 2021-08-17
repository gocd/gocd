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
package com.thoughtworks.go.util;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

public class CsvTest {

    @Test
    public void shouldOutputFieldsAndRowsAsString() throws Exception {
        Csv csv = new Csv();
        csv.newRow()
                .put("a", "1")
                .put("b", "2");
        csv.newRow()
                .put("a", "3")
                .put("b", "4");
        assertThat(csv.toString(), is(
                "a,b\n"
                        + "1,2\n"
                        + "3,4\n"
        ));
    }

    @Test
    public void shouldSupportRowsContainingDifferentColumns() throws Exception {
        Csv csv = new Csv();
        csv.newRow()
                .put("a", "1");
        csv.newRow()
                .put("b", "2");
        assertThat(csv.toString(), is(
                "a,b\n"
                        + "1,\n"
                        + ",2\n"
        ));
    }

    @Test
    public void shouldParseCsvFromString() throws Exception {
        Csv csv = Csv.fromString("a,b\n1,2");
        assertThat(csv.rowCount(), is(1));
        assertThat(csv.containsRow(new HashMap<String, String>() {
            {
                put("a", "1");
                put("b", "2");
            }
        }), is(true));
    }

}
