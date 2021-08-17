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
package com.thoughtworks.go.domain;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JobResultTest {

    @Test public void shouldConvertToCctrayStatus() throws Exception {
        assertThat(JobResult.Passed.toCctrayStatus(), is("Success"));
        assertThat(JobResult.Failed.toCctrayStatus(), is("Failure"));
        assertThat(JobResult.Cancelled.toCctrayStatus(), is("Failure"));
        assertThat(JobResult.Unknown.toCctrayStatus(), is("Success"));
    }


    @Test
    public void compatorShouldOrderBy_failed_passed_and_then_unknown() throws Exception {
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Failed),is(1));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Passed),is(-1));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Failed, JobResult.Passed),is(-1));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Passed, JobResult.Failed),is(1));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Passed, JobResult.Passed),is(0));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Cancelled, JobResult.Failed),is(0));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Cancelled, JobResult.Passed),is(-1));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Cancelled, JobResult.Unknown),is(-1));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Unknown),is(0));
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Cancelled),is(1));
    }


}
