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