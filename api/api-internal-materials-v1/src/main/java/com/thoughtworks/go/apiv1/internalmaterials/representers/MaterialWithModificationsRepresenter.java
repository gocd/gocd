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

package com.thoughtworks.go.apiv1.internalmaterials.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv1.internalmaterials.models.MaterialInfo;
import com.thoughtworks.go.apiv1.internalmaterials.representers.materials.MaterialsRepresenter;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MaterialWithModificationsRepresenter {
    public static void toJSON(OutputWriter outputWriter, Map<MaterialConfig, MaterialInfo> modificationsMap) {
        outputWriter.addLinks(links());
        if (modificationsMap.isEmpty()) {
            outputWriter.addChildList("materials", Collections.emptyList());
            return;
        }
        outputWriter.addChildList("materials", materialWriter -> {
            modificationsMap.forEach((material, info) -> materialWriter.addChild(childWriter -> {
                childWriter.addChild("config", MaterialsRepresenter.toJSON(material))
                        .add("can_trigger_update", info.isHasOperatePermission())
                        .add("material_update_in_progress", info.isUpdateInProgress())
                        .addIfNotNull("material_update_start_time", info.getUpdateStartTime());
                if (info.getModification() == null) {
                    childWriter.renderNull("modification");
                } else {
                    childWriter.addChild("modification", modWriter -> ModificationRepresenter.toJSON(modWriter, info.getModification()));
                }
                renderLogs(childWriter, info.getLogs());
            }));
        });
    }

    private static void renderLogs(OutputWriter outputWriter, List<ServerHealthState> logs) {
        outputWriter.addChildList("messages", writer -> {
            for (ServerHealthState log : logs) {
                writer.addChild(childWriter -> {
                    childWriter.add("level", log.getLogLevel().name())
                            .add("message", log.getMessage())
                            .add("description", log.getDescription());
                });
            }
        });
    }

    private static Consumer<OutputLinkWriter> links() {
        return outputLinkWriter -> outputLinkWriter.addLink("self", Routes.InternalMaterialConfig.INTERNAL_BASE)
                .addAbsoluteLink("doc", Routes.MaterialConfig.DOC);
    }
}
