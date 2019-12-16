/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv3.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.PartialConfigParseResult;

public class PartialConfigParseResultRepresenter {
    public static void toJSON(OutputWriter json, PartialConfigParseResult result) {
        if (null == result) {
            return;
        }

        if (result.getLatestParsedModification() != null) {
            json.addChild("latest_parsed_modification", outputWriter -> {
                outputWriter.add("username", result.getLatestParsedModification().getUserName());
                outputWriter.add("email_address", result.getLatestParsedModification().getEmailAddress());
                outputWriter.add("revision", result.getLatestParsedModification().getRevision());
                outputWriter.add("comment", result.getLatestParsedModification().getComment());
                outputWriter.add("modified_time", result.getLatestParsedModification().getModifiedTime());
            });
        } else {
            json.add("latest_parsed_modification", (String) null);
        }

        if (result.getGoodModification() != null) {
            json.addChild("good_modification", outputWriter -> {
                outputWriter.add("username", result.getGoodModification().getUserName());
                outputWriter.add("email_address", result.getGoodModification().getEmailAddress());
                outputWriter.add("revision", result.getGoodModification().getRevision());
                outputWriter.add("comment", result.getGoodModification().getComment());
                outputWriter.add("modified_time", result.getGoodModification().getModifiedTime());
            });
        } else {
            json.add("good_modification", (String) null);
        }

        json.add("error", result.isSuccessful() ? null : result.getLastFailure().getMessage());
    }
}
