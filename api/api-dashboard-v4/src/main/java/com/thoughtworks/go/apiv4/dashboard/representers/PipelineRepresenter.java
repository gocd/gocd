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
package com.thoughtworks.go.apiv4.dashboard.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.presentation.pipelinehistory.EmptyPipelineInstanceModel;
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.spark.Routes;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

public class PipelineRepresenter {

    public static void toJSON(OutputWriter jsonOutputWriter, GoDashboardPipeline model, Username username) {
        String usernameString = username.getUsername().toString();

        boolean isConfiguredFromConfigRepo = !model.isLocal();
        jsonOutputWriter
                .addLinks(linksWriter -> addLinks(linksWriter, model))
                .add("name", model.name().toString())
                .add("last_updated_timestamp", model.getLastUpdatedTimeStamp())
                .add("locked", model.model().getLatestPipelineInstance().isCurrentlyLocked())
                .addChild("pause_info", getPauseInfoNEW(model))
                .add("can_operate", model.isPipelineOperator(usernameString))
                .add("can_administer", model.canBeAdministeredBy(usernameString))
                .add("can_unlock", model.canBeOperatedBy(usernameString))
                .add("can_pause", model.canBeOperatedBy(usernameString))
                .add("from_config_repo", isConfiguredFromConfigRepo);

        if (isConfiguredFromConfigRepo) {
            RepoConfigOrigin configRepo = (RepoConfigOrigin) model.getOrigin();
            jsonOutputWriter
                    .add("config_repo_id", configRepo.getConfigRepo().getId())
                    .add("config_repo_material_url", configRepo.getMaterial().getUriForDisplay());
        }

        if (model.getTrackingTool().isPresent()) {
            TrackingTool trackingTool = model.getTrackingTool().get();

            jsonOutputWriter.addChild("tracking_tool", childWriter -> {
                childWriter
                        .add("regex", trackingTool.getRegex())
                        .add("link", trackingTool.getLink());
            });
        }

        jsonOutputWriter.addChild("_embedded", childWriter -> {
            childWriter.addChildList("instances", writeInstances(model));
        });
    }

    private static Consumer<OutputListWriter> writeInstances(GoDashboardPipeline model) {
        return listWriter -> {
            model.model().getActivePipelineInstances().stream()
                    .filter(instanceModel -> !(instanceModel instanceof EmptyPipelineInstanceModel))
                    .forEach(instanceModel -> {
                        listWriter.addChild(childWriter -> PipelineInstanceRepresenter.toJSON(childWriter, instanceModel));
                    });
        };
    }

    private static Consumer<OutputWriter> getPauseInfoNEW(GoDashboardPipeline model) {
        return writer -> {
            PipelinePauseInfo pausedInfo = model.model().getPausedInfo();
            writer.add("paused", pausedInfo.isPaused());
            writer.add("paused_by", StringUtils.isBlank(pausedInfo.getPauseBy()) ? null : pausedInfo.getPauseBy());
            writer.add("pause_reason", StringUtils.isBlank(pausedInfo.getPauseCause()) ? null : pausedInfo.getPauseCause());
            writer.add("paused_at", pausedInfo.getPausedAt());
        };
    }

    private static void addLinks(OutputLinkWriter linksWriter, GoDashboardPipeline model) {
        String pipelineName = model.name().toString();
        linksWriter.addLink("self", Routes.PipelineInstance.history(pipelineName))
                .addAbsoluteLink("doc", Routes.Pipeline.DOC);
    }
}
