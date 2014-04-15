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

package com.thoughtworks.go.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.FileUtil.deleteFolder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArtifactPlanTest {
    private final Mockery context = new ClassMockery();
    private File testFolder;
    private File srcFolder;

    @Before
    public void setUp() {
        testFolder = new File("test.com");
        srcFolder = new File(testFolder, "src");
        srcFolder.mkdirs();
    }

    @After
    public void tearDown() {
        deleteFolder(testFolder);
    }

    @Test public void shouldPublishArtifacts() throws Exception {
        final DefaultGoPublisher publisher = context.mock(DefaultGoPublisher.class);
        final ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "src", "dest");
        context.checking(new Expectations() {
            {
                one(publisher).upload(new File(testFolder, "src"), "dest");
            }
        });

        artifactPlan.publish(publisher, testFolder);
        context.assertIsSatisfied();
    }

    @Test
    public void shouldNormalizePath() throws Exception {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "folder\\src", "folder\\dest");
        assertThat(artifactPlan.getSrc(), is("folder/src"));
        assertThat(artifactPlan.getDest(), is("folder/dest"));
    }

    @Test
    public void shouldProvideAppendFilePathToDest() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "test/**/*/a.log", "logs");
        assertThat(artifactPlan.destURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/a/b/a.log")), is("logs/a/b"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenUsingDoubleStart() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "**/*/a.log", "logs");
        assertThat(artifactPlan.destURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/a/b/a.log")), is("logs/test/a/b"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenPathProvidedAreSame() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "test/a/b/a.log", "logs");
        assertThat(artifactPlan.destURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/b/a.log")), is("logs"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenUsingSingleStarToMatchFile() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "test/a/b/*.log", "logs");
        assertThat(artifactPlan.destURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/test/a/b/a.log")), is("logs"));
    }

    @Test
    public void shouldProvideAppendFilePathToDestWhenPathMatchingAtTheRoot() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "*.jar", "logs");
        assertThat(artifactPlan.destURL(new File("pipelines/pipelineA"),
                new File("pipelines/pipelineA/a.jar")), is("logs"));
    }

    @Test
    public void shouldTrimThePath() {
        assertThat(new ArtifactPlan(ArtifactType.file, "pkg   ", "logs "),
                is(new ArtifactPlan(ArtifactType.file, "pkg", "logs")));
    }

    @Test
    public void shouldGiveTheEffectiveDestinationPath() {
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/bar", "").effectiveDestinationPath(), is("bar"));
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/bar", "blah/foo").effectiveDestinationPath(), is("blah/foo/bar"));
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/bar.xml", "").effectiveDestinationPath(), is("bar.xml"));
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/bar/blah.xml", "boo/baz").effectiveDestinationPath(), is("boo/baz/blah.xml"));
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/**/*blah.xml", "boo/baz").effectiveDestinationPath(), is("boo/baz/*blah.xml"));
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/**/*.xml", "boo/baz").effectiveDestinationPath(), is("boo/baz/*.xml"));
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/**/*", "boo/baz").effectiveDestinationPath(), is("boo/baz/*"));
        assertThat(new ArtifactPlan(ArtifactType.file, "foo/**/*", "boo\\baz").effectiveDestinationPath(), is("boo/baz/*"));
    }

    @Test
    public void validate_shouldFailIfSourceIsEmpty() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "", "bar");
        artifactPlan.validate(ValidationContext.forChain(new JobConfig("jobname")));
        assertThat(artifactPlan.errors().on(ArtifactPlan.SRC), is("Job 'jobname' has an aritfact with an empty source"));
    }

    @Test
   public void validate_shouldFailIfDestDoesNotMatchAFilePattern() {
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "foo/bar", "..");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().on(ArtifactPlan.DEST), is("Invalid destination path. Destination path should match the pattern ([^. ].+[^. ])|([^. ][^. ])|([^. ])"));
    }

    @Test
    public void validate_shouldNotFailWhenDestinationIsNotSet() {
        ArtifactPlan artifactPlan = new ArtifactPlan();
        artifactPlan.setSrc("source");
        artifactPlan.validate(null);
        assertThat(artifactPlan.errors().isEmpty(), is(true));
    }
    
    @Test
    public void shouldErrorOutWhenDuplicateArtifactPlansExists() {
        List<ArtifactPlan> plans = new ArrayList<ArtifactPlan>();
        ArtifactPlan existingPlan = new ArtifactPlan(ArtifactType.file, "src", "dest");
        plans.add(existingPlan);
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactType.file, "src", "dest");

        artifactPlan.validateUniqueness(plans);

        assertThat(artifactPlan.errors().isEmpty(), is(false));
        assertThat(artifactPlan.errors().on(ArtifactPlan.SRC), is("Duplicate artifacts defined."));
        assertThat(artifactPlan.errors().on(ArtifactPlan.DEST), is("Duplicate artifacts defined."));
        assertThat(existingPlan.errors().isEmpty(), is(false));
        assertThat(existingPlan.errors().on(ArtifactPlan.SRC), is("Duplicate artifacts defined."));
        assertThat(existingPlan.errors().on(ArtifactPlan.DEST), is("Duplicate artifacts defined."));

    }
}
