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

package com.thoughtworks.go.server.web.i18n;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class ResolvableViewableStatusTest {
    @Test
    public void shouldRetureCode() throws Exception {
        CurrentStatus currentStatus = CurrentStatus.BUILDING;
        ResolvableViewableStatus resolvableViewableStatus = new ResolvableViewableStatus(currentStatus);
        String expectation = ResolvableViewableStatus.CODE_PREFIX + currentStatus.getStatus().toLowerCase();
        String real = resolvableViewableStatus.getCodes()[0];
        assertThat(real, is(expectation));
    }

    @Test
    public void shouldReturnDefaultMessage() throws Exception {
        CurrentStatus currentStatus = CurrentStatus.BUILDING;
        ResolvableViewableStatus resolvableViewableStatus = new ResolvableViewableStatus(currentStatus);
        assertThat(resolvableViewableStatus.getDefaultMessage(), is(currentStatus.getStatus()));
    }
}