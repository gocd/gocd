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

package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.i18n.LocalizedMessage;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

public class SubsectionLocalizedOperationResultTest {
    @Test public void shouldBeSuccessfulByDefault() throws Exception {
        assertThat("isSuccessful", new SubsectionLocalizedOperationResult().isSuccessful(), is(true));
    }

    @Test public void shouldUnderstandErrorStates() throws Exception {
        SubsectionLocalizedOperationResult result = new SubsectionLocalizedOperationResult();
        result.connectionError(LocalizedMessage.string("foo"));
        assertThat("not successful", result.isSuccessful(), is(false));
        Localizer localizer = mock(Localizer.class);
        when(localizer.localize(any(String.class), anyVararg())).thenReturn("could not connect");
        assertThat(result.replacementContent(localizer), is("could not connect"));
    }
}
