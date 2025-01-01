/*
 * Copyright Thoughtworks, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

public class JobResultTest {

    @Test
    public void shouldConvertToCctrayStatus() {
        assertThat(JobResult.Passed.toCctrayStatus()).isEqualTo("Success");
        assertThat(JobResult.Failed.toCctrayStatus()).isEqualTo("Failure");
        assertThat(JobResult.Cancelled.toCctrayStatus()).isEqualTo("Failure");
        assertThat(JobResult.Unknown.toCctrayStatus()).isEqualTo("Success");
    }


    @Test
    public void compatorShouldOrderBy_failed_passed_and_then_unknown() {
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Failed)).isEqualTo(1);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Passed)).isEqualTo(-1);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Failed, JobResult.Passed)).isEqualTo(-1);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Passed, JobResult.Failed)).isEqualTo(1);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Passed, JobResult.Passed)).isEqualTo(0);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Cancelled, JobResult.Failed)).isEqualTo(0);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Cancelled, JobResult.Passed)).isEqualTo(-1);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Cancelled, JobResult.Unknown)).isEqualTo(-1);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Unknown)).isEqualTo(0);
        assertThat(JobResult.JOB_RESULT_COMPARATOR.compare(JobResult.Unknown, JobResult.Cancelled)).isEqualTo(1);
    }


}
