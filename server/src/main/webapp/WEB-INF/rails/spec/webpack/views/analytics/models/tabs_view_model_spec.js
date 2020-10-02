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
import {VM as TabsVM} from "views/analytics/models/tabs_view_model";

describe("Analytics Tabs View Model", () => {

  let vm;
  beforeEach(() => {
    vm = new TabsVM();
  });

  it('should select global tab by default', () => {
    expect(vm.isGlobalTabSelected()).toBe(true);
  });

  it('should set pipelines tab selection', () => {
    expect(vm.isPipelineTabSelected()).toBe(false);
    vm.setPipelineTabSelection();
    expect(vm.isPipelineTabSelected()).toBe(true);
  });

  it('should set global tab selection', () => {
    vm.setPipelineTabSelection();
    expect(vm.isGlobalTabSelected()).toBe(false);
    vm.setGlobalTabSelection();
    expect(vm.isGlobalTabSelected()).toBe(true);
  });

});
