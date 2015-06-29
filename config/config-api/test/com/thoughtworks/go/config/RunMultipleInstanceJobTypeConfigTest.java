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
package com.thoughtworks.go.config;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RunMultipleInstanceJobTypeConfigTest {
	private JobConfig jobConfig;

	@Before
	public void setup() {
		jobConfig = new JobConfig(new CaseInsensitiveString("job"));
		jobConfig.setRunInstanceCount(10);
	}

	@Test
	public void shouldTellIsInstanceOfCorrectly() throws Exception {
		assertThat(jobConfig.isInstanceOf("job-runInstance-1", false), is(true));
		assertThat(jobConfig.isInstanceOf("Job-runInstance-1", true), is(true));
		assertThat(jobConfig.isInstanceOf("Job-runInstance-1", false), is(false));
	}

	@Test
	public void shouldTranslatedJobNameCorrectly() throws Exception {
		assertThat(jobConfig.translatedName("crap-runInstance-1"), is("job-runInstance-1"));
		assertThat(jobConfig.translatedName("crap"), is("job"));
	}
}