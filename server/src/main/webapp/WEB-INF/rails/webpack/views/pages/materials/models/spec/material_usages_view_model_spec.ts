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

import {DefinedPipeline} from "models/config_repos/defined_structures";
import {usagesJSON} from "views/pages/materials/spec/material_usage_widget_spec";
import {MaterialUsagesVM} from "../material_usages_view_model";

describe('MaterialUsagesVMSpec', () => {
  it('should map materials json to vm', () => {
    const vm = MaterialUsagesVM.fromJSON(usagesJSON());

    expect(vm.children.length).toBe(2);
    expect(vm.children[0].name()).toBe('group1');
    expect(vm.children[1].name()).toBe('group2');

    expect(vm.children[0].children).toBeInstanceOf(Array);

    const children = vm.children[0].children as DefinedPipeline[];

    expect(children.length).toBe(2);
    expect(children[0].name()).toBe('pipeline1');
    expect(children[0].children.length).toBe(0);
    expect(children[1].name()).toBe('pipeline2');
  });
});
