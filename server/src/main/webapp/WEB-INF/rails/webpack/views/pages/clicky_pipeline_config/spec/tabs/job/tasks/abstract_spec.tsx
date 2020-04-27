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

import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {AntTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/ant";

describe("Abstract Task Modal", () => {
  let modal: AbstractTaskModal | undefined;
  afterEach(() => {
    modal = undefined;
  });

  it("should render buttons", () => {
    mount(false);
    expect(modal?.buttons()).toHaveLength(2);
  });

  it("should not render buttons in read only mode", () => {
    mount(true);
    expect(modal?.buttons()).toHaveLength(0);
  });

  function mount(readonly: boolean) {
    modal = new AntTaskModal(undefined, false, jasmine.createSpy(),
                             new PluginInfos(), readonly);
  }

});
