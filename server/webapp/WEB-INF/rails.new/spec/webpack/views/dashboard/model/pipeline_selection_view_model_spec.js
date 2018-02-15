/*
 * Copyright 2018 ThoughtWorks, Inc.
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

describe("Dashboard Pipeline Selection View Model", () => {
  const PipelineSelectionVM = require("views/dashboard/models/pipeline_selection_view_model");

  let vm;
  beforeEach(() => {
    vm = new PipelineSelectionVM();
    vm.initialize(json);
  });

  it('should initialize vm with pipeline group states', () => {
    expect(vm.isGroupExpanded('group-1')).toBe(true);
    expect(vm.isGroupExpanded('group-2')).toBe(false);
    expect(vm.isGroupExpanded('group-3')).toBe(false);
  });

  it('should toggle group selection', () => {
    const groupName = 'group-1';

    expect(vm.isGroupExpanded(groupName)).toBe(true);
    vm.toggleGroupSelection(groupName);
    expect(vm.isGroupExpanded(groupName)).toBe(false);
  });

  it('should set group selection', () => {
    const groupName = 'group-1';

    expect(vm.isGroupExpanded(groupName)).toBe(true);
    vm.setGroupSelection(groupName, false);
    expect(vm.isGroupExpanded(groupName)).toBe(false);
    vm.setGroupSelection(groupName, true);
    expect(vm.isGroupExpanded(groupName)).toBe(true);
  });

  const json = {
    "group-1": [],
    "group-2": [],
    "group-3": []
  };
});
