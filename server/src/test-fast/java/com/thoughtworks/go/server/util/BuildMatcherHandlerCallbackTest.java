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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.domain.BuildOutputMatcher;
import com.thoughtworks.go.server.dao.handlers.BuildMatcherHandlerCallback;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class BuildMatcherHandlerCallbackTest {

    @Test public void shouldParseClassNameToInstanciateMatcher() {
        BuildMatcherHandlerCallback callback = new BuildMatcherHandlerCallback();
        assertThat(callback.valueOf("com.thoughtworks.go.domain.BuildOutputMatcher"),
                instanceOf(BuildOutputMatcher.class));
    }
}
