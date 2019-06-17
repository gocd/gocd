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

package com.thoughtworks.go.apiv8.admin.pipelineconfig.representers.trackingtool;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.TrackingTool;


public class ExternalTrackingToolRepresenter {

  public static void toJSON(OutputWriter jsonWriter, TrackingTool trackingTool) {
    jsonWriter.add("url_pattern", trackingTool.getLink());
    jsonWriter.add("regex", trackingTool.getRegex());
  }

  public static TrackingTool fromJSON(JsonReader jsonReader) {
    TrackingTool trackingTool = new TrackingTool();
    jsonReader.readStringIfPresent("url_pattern", trackingTool::setLink);
    jsonReader.readStringIfPresent("regex", trackingTool::setRegex);
    return trackingTool;
  }
}
