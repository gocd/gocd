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
package com.thoughtworks.go.server.ui;


import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SearchCriteriaTest {

    @Test
    public void shouldReturnTrueIfTheSearchTermMatches() throws Exception {
        SearchCriteria criteria = new SearchCriteria("win");
        assertThat(criteria.matches("windows"), is(true));
        assertThat(criteria.matches("win"), is(true));
        criteria = new SearchCriteria("windows");
        assertThat(criteria.matches("windows"), is(true));
        assertThat(criteria.matches("win"), is(false));
    }

    @Test
    public void shouldPerformExactMatch() throws Exception {
        SearchCriteria criteria = new SearchCriteria("\"win\"");
        assertThat(criteria.matches("windows"), is(false));
        assertThat(criteria.matches("win"), is(true));
    }
    @Test
    public void shouldHandleInvalidInput() {
        SearchCriteria criteria = new SearchCriteria("\"\"\"");
        assertThat(criteria.matches(""), is(false));
        criteria = new SearchCriteria("\"");
        assertThat(criteria.matches(""), is(false));

    }


}
