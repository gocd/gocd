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
package com.thoughtworks.go.server.util;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class WebUtilsTest {
    @Test
    public void shouldReturn35CharsAndDotWhenStringIsLongerThan35() throws Exception {
        String limited = new WebUtils().limit(construct(36), 35);
        assertThat(limited, is(construct(35) + "..."));
    }

    @Test
    public void shouldReturn5CharsAndDotWhenStringOnlyContains5Chars() throws Exception {
        String code = construct(5);
        String limited = new WebUtils().limit(code, 35);
        assertThat(limited, is(code));
    }

    @Test
    public void shouldReturnEmptyString() throws Exception {
        String limited = new WebUtils().limit("", 10);
        assertThat(limited, is(""));
    }

    @Test
    public void shouldReturnEmptyWhenStringIsNull() throws Exception {
        String limited = new WebUtils().limit(null, 10);
        assertThat(limited, is(""));
    }

    private String construct(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("c");
        }
        return sb.toString();
    }
}
