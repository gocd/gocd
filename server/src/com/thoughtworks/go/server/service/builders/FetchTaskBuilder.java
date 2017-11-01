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

package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.config.PathFromAncestor;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.ChecksumFileHandler;
import com.thoughtworks.go.domain.DirHandler;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.FetchHandler;
import com.thoughtworks.go.domain.FileHandler;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class FetchTaskBuilder implements TaskBuilder<FetchTask> {
    @Override
    public Builder createBuilder(BuilderFactory builderFactory, FetchTask task, Pipeline pipeline, UpstreamPipelineResolver resolver) {
        JobIdentifier fetchFrom = resolveTargetJob(task, pipeline, resolver);

        Builder cancelBuilder = builderFactory.builderFor(task.cancelTask(), pipeline, resolver);
        return new FetchArtifactBuilder(task.getConditions(), cancelBuilder, task.describe(), fetchFrom, task.getSrc(),
                task.getDest(), getHandler(task, pipeline.getName()), getChecksumHandler(task, pipeline.getName()));
    }

    private ChecksumFileHandler getChecksumHandler(FetchTask task, String pipelineName) {
        return new ChecksumFileHandler(task.artifactDest(pipelineName, task.checksumPath()));
    }

    FetchHandler getHandler(FetchTask task, String pipelineName) {
        return StringUtils.isNotEmpty(task.getRawSrcdir()) ? new DirHandler(task.getRawSrcdir(), task.destOnAgent(pipelineName)) : new FileHandler(
                task.artifactDest(pipelineName, FilenameUtils.getName(task.getRawSrcfile())), task.getSrc());
    }

    private JobIdentifier resolveTargetJob(FetchTask task, Pipeline currentPipeline, UpstreamPipelineResolver resolver) {
        PathFromAncestor pipelineNamePathFromAncestor = task.getPipelineNamePathFromAncestor();
        if (pipelineNamePathFromAncestor == null
                || CaseInsensitiveString.isBlank(pipelineNamePathFromAncestor.getPath())
                || CaseInsensitiveString.areEqual(new CaseInsensitiveString(currentPipeline.getName()), pipelineNamePathFromAncestor.getPath())) {
            task.setPipelineName(new CaseInsensitiveString(currentPipeline.getName()));

            String stageCounter = JobIdentifier.LATEST;
            if (currentPipeline.hasStageBeenRun(CaseInsensitiveString.str(task.getStage()))) {
                stageCounter = String.valueOf(currentPipeline.findStage(CaseInsensitiveString.str(task.getStage())).getCounter());
            }

            return new JobIdentifier(new StageIdentifier(currentPipeline.getName(), currentPipeline.getCounter(), currentPipeline.getLabel(), CaseInsensitiveString.str(task.getStage()), stageCounter), CaseInsensitiveString.str(task.getJob()));
        } else {
            DependencyMaterialRevision revision = null;
            if (pipelineNamePathFromAncestor.isAncestor()) {
                BuildCause buildCause = currentPipeline.getBuildCause();
                for (CaseInsensitiveString parentPipelineName : pipelineNamePathFromAncestor.pathToAncestor()) {
                    DependencyMaterialRevision dependencyMaterialRevision = dmrForPipeline(parentPipelineName, buildCause);
                    if (dependencyMaterialRevision == null) {
                        throw bomb(String.format("Pipeline [%s] could not fetch artifact [%s]. Unable to resolve revision for [%s] from build cause", currentPipeline.getName(), task, parentPipelineName));
                    }
                    buildCause = resolver.buildCauseFor(dependencyMaterialRevision.getPipelineName(), dependencyMaterialRevision.getPipelineCounter());
                }
                revision = dmrForPipeline(pipelineNamePathFromAncestor.getAncestorName(), buildCause);
                if (revision == null) {
                    throw bomb(String.format("Pipeline [%s] could not fetch artifact [%s]. Unable to resolve revision for [%s] from build cause", currentPipeline.getName(), task, pipelineNamePathFromAncestor.getAncestorName()));
                }
            } else {
                revision = dmrForPipeline(pipelineNamePathFromAncestor.getPath(), currentPipeline.getBuildCause());
                if (revision == null) {
                    throw bomb(String.format("Pipeline [%s] tries to fetch artifact from job [%s/%s/%s] "
                            + "which is not a dependency material", currentPipeline.getName(), pipelineNamePathFromAncestor, task.getStage(), task.getJob()));
                }
            }

            String stageCounter = JobIdentifier.LATEST;
            if (task.getStage().equals(new CaseInsensitiveString(revision.getStageName()))) {
                stageCounter = String.valueOf(revision.getStageCounter());
            }
            return new JobIdentifier(new StageIdentifier(CaseInsensitiveString.str(pipelineNamePathFromAncestor.getAncestorName()), revision.getPipelineCounter(), revision.getPipelineLabel(),
                    CaseInsensitiveString.str(task.getStage()), stageCounter), CaseInsensitiveString.str(task.getJob()));
        }
    }

    private DependencyMaterialRevision dmrForPipeline(CaseInsensitiveString pipelineName, BuildCause buildCause) {
        MaterialRevisions materialRevisions = buildCause.getMaterialRevisions();
        return materialRevisions.findDependencyMaterialRevision(CaseInsensitiveString.str(pipelineName));
    }
}
