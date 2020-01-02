/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.builder.FetchPluggableArtifactBuilder;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.UpstreamPipelineResolver;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.thoughtworks.go.remote.work.artifact.ArtifactsPublisher.PLUGGABLE_ARTIFACT_METADATA_FOLDER;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class FetchTaskBuilder implements TaskBuilder<AbstractFetchTask> {
    private final GoConfigService goConfigService;

    @Autowired
    public FetchTaskBuilder(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    @Override
    public Builder createBuilder(BuilderFactory builderFactory, AbstractFetchTask task, Pipeline pipeline, UpstreamPipelineResolver resolver) {
        final JobIdentifier fetchFrom = resolveTargetJob(task, pipeline, resolver);
        final Builder cancelBuilder = builderFactory.builderFor(task.cancelTask(), pipeline, resolver);
        final ChecksumFileHandler checksumHandler = getChecksumHandler(task, pipeline.getName());

        if (task instanceof FetchTask) {
            return createFetchTaskBuilder((FetchTask) task, pipeline, fetchFrom, cancelBuilder, checksumHandler);
        } else {
            return createPluggableFetchTaskBuilder((FetchPluggableArtifactTask) task, pipeline, fetchFrom, cancelBuilder, checksumHandler);
        }
    }

    private Builder createFetchTaskBuilder(FetchTask task, Pipeline pipeline, JobIdentifier fetchFrom, Builder cancelBuilder, ChecksumFileHandler checksumHandler) {
        final FetchHandler fetchHandler = getHandler(task, pipeline.getName());
        return new FetchArtifactBuilder(task.getConditions(), cancelBuilder, task.describe(), fetchFrom,
                task.getSrc(), task.getDest(), fetchHandler, checksumHandler);
    }

    private Builder createPluggableFetchTaskBuilder(FetchPluggableArtifactTask task, Pipeline pipeline, JobIdentifier fetchFrom, Builder cancelBuilder, ChecksumFileHandler checksumHandler) {

        final ArtifactStore artifactStore = getArtifactStoreFor(task, fetchFrom);
        final String fileName = format("%s.json", artifactStore.getPluginId());
        final String metadataFileLocationOnServer = format("%s/%s", PLUGGABLE_ARTIFACT_METADATA_FOLDER, fileName);
        final File metadataFileDest = task.artifactDest(pipeline.getName(), fileName);

        return new FetchPluggableArtifactBuilder(task.getConditions(), cancelBuilder, task.describe(), fetchFrom, artifactStore,
                task.getConfiguration(), task.getArtifactId(), metadataFileLocationOnServer, metadataFileDest, checksumHandler);
    }

    private ArtifactStore getArtifactStoreFor(FetchPluggableArtifactTask task, JobIdentifier fetchFrom) {
        final JobConfig job = goConfigService.getCurrentConfig().findJob(fetchFrom.getPipelineName(), fetchFrom.getStageName(), fetchFrom.getBuildName());
        final PluggableArtifactConfig artifactConfig = job.artifactTypeConfigs().findByArtifactId(task.getArtifactId());

        if (artifactConfig == null) {
            throw new RuntimeException(format("Pluggable artifact with id `%s` does not exist.", task.getArtifactId()));
        }

        final ArtifactStore artifactStore = goConfigService.artifactStores().find(artifactConfig.getStoreId());

        if (artifactStore == null) {
            throw new RuntimeException(format("Artifact store with id `%s` does not exist.", task.getArtifactId()));
        }
        return artifactStore;
    }

    private ChecksumFileHandler getChecksumHandler(AbstractFetchTask task, String pipelineName) {
        return new ChecksumFileHandler(task.artifactDest(pipelineName, task.checksumPath()));
    }

    FetchHandler getHandler(FetchTask task, String pipelineName) {
        return isNotEmpty(task.getRawSrcdir()) ? new DirHandler(task.getRawSrcdir(), task.destOnAgent(pipelineName)) : new FileHandler(
                task.artifactDest(pipelineName, FilenameUtils.getName(task.getRawSrcfile())), task.getSrc());
    }

    private JobIdentifier resolveTargetJob(AbstractFetchTask task, Pipeline currentPipeline, UpstreamPipelineResolver resolver) {
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
                        throw bomb(format("Pipeline [%s] could not fetch artifact [%s]. Unable to resolve revision for [%s] from build cause", currentPipeline.getName(), task, parentPipelineName));
                    }
                    buildCause = resolver.buildCauseFor(dependencyMaterialRevision.getPipelineName(), dependencyMaterialRevision.getPipelineCounter());
                }
                revision = dmrForPipeline(pipelineNamePathFromAncestor.getAncestorName(), buildCause);
                if (revision == null) {
                    throw bomb(format("Pipeline [%s] could not fetch artifact [%s]. Unable to resolve revision for [%s] from build cause", currentPipeline.getName(), task, pipelineNamePathFromAncestor.getAncestorName()));
                }
            } else {
                revision = dmrForPipeline(pipelineNamePathFromAncestor.getPath(), currentPipeline.getBuildCause());
                if (revision == null) {
                    throw bomb(format("Pipeline [%s] tries to fetch artifact from job [%s/%s/%s] "
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
