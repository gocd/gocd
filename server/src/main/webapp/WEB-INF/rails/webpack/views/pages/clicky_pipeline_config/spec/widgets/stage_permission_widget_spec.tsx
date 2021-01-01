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

import m from "mithril";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {StagePermissionWidget} from "views/pages/clicky_pipeline_config/widgets/stage_permission_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("StageSettingsTab", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render radio button to inherit the permission from pipeline group or define locally", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    mount(stage);
  });

  function mount(stage: Stage) {
    helper.mount(() => <StagePermissionWidget stage={stage}/>);
  }
});
