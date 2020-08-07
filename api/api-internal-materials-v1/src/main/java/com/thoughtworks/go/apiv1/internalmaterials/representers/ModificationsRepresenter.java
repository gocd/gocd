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

package com.thoughtworks.go.apiv1.internalmaterials.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.spark.Routes;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ModificationsRepresenter {
    public static void toJSON(OutputWriter outputWriter, List<Modification> modifications, PipelineRunIdInfo latestAndOldestModId, String fingerprint, String pattern) {
        if (modifications == null || modifications.isEmpty()) {
            outputWriter.addChildList("modifications", emptyList());
            return;
        }
        if (latestAndOldestModId != null) {
            Modification latest = modifications.get(0);
            Modification oldest = modifications.get(modifications.size() - 1);
            String previousLink = null, nextLink = null;
            if (latest.getId() != latestAndOldestModId.getLatestRunId()) {
                previousLink = Routes.InternalMaterialConfig.previous(fingerprint, latest.getId(), pattern);
            }
            if (oldest.getId() != latestAndOldestModId.getOldestRunId()) {
                nextLink = Routes.InternalMaterialConfig.next(fingerprint, oldest.getId(), pattern);
            }
            if (isNotBlank(previousLink) || isNotBlank(nextLink)) {
                String finalPreviousLink = previousLink;
                String finalNextLink = nextLink;
                outputWriter.addLinks(outputLinkWriter -> {
                    outputLinkWriter.addLinkIfPresent("previous", finalPreviousLink);
                    outputLinkWriter.addLinkIfPresent("next", finalNextLink);
                });
            }
        }
        outputWriter.addChildList("modifications", childWriter -> {
            modifications.forEach((mod) -> childWriter.addChild(writer -> ModificationRepresenter.toJSON(writer, mod)));
        });
    }
}
