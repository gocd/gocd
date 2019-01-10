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

package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.ConfigSaveState;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GoConfigValidityTest {

    @Test
    public void shouldIndicateMergedBasedOnConfigSaveState(){
        assertThat(GoConfigValidity.valid(ConfigSaveState.UPDATED).wasMerged(), is(false));
        assertThat(GoConfigValidity.valid(ConfigSaveState.MERGED).wasMerged(), is(true));
        assertThat(GoConfigValidity.invalid("error!").wasMerged(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenMergeConflictIsEncountered() throws Exception {
        GoConfigValidity validity = GoConfigValidity.invalid("error").mergeConflict();
        assertThat(validity.isMergeConflict(), is(true));
    }

    @Test
    public void shouldReturnFalseWhenMergeConflictIsEncounteredButIsValid() throws Exception {
        GoConfigValidity validity = GoConfigValidity.valid().mergeConflict();
        assertThat(validity.isMergeConflict(), is(false));
    }

    @Test
    public void shouldReturnTrueWhenPostValidationErrorIsEncountered() throws Exception {
        GoConfigValidity validity = GoConfigValidity.invalid("error").mergePostValidationError();
        assertThat(validity.isPostValidationError(), is(true));
    }

    @Test
    public void shouldReturnFalseWhenPostValidationErrorIsEncounteredButIsValid() throws Exception {
        GoConfigValidity validity = GoConfigValidity.valid().mergePostValidationError();
        assertThat(validity.isPostValidationError(), is(false));
    }

}
