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

import {Pipelines} from "models/internal_pipeline_structure/pipeline_structure";
import data from "./test_data";

const xmlPipeline        = data.pipeline_association_in_xml_json();
const configRepoPipeline = data.pipeline_association_in_config_repo_json();

describe("Environments Model - Pipelines", () => {
  it("should deserialize from json", () => {
    const pipelines = Pipelines.fromJSON([xmlPipeline, configRepoPipeline]);

    expect(pipelines.length).toEqual(2);
    expect(pipelines[0].name()).toEqual(xmlPipeline.name);
    expect(pipelines[0].origin().type()).toEqual(xmlPipeline.origin.type);
    expect(pipelines[1].name()).toEqual(configRepoPipeline.name);
    expect(pipelines[1].origin().type()).toEqual(configRepoPipeline.origin.type);
  });

  it("should answer whether the collection contains a pipeline", () => {
    const pipelines = Pipelines.fromJSON([xmlPipeline, configRepoPipeline]);

    expect(pipelines.containsPipeline(xmlPipeline.name)).toBe(true);
    expect(pipelines.containsPipeline(configRepoPipeline.name)).toBe(true);

    expect(pipelines.containsPipeline("my-fancy-pipeline")).toBe(false);
  });
});
