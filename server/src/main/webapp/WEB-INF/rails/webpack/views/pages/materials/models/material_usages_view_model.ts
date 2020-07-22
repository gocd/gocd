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
import _ from "lodash";
import {DefinedGroup, NamedTree} from "models/config_repos/defined_structures";
import {MaterialUsageJSON} from "./material_view_model";

export class MaterialUsagesVM implements NamedTree {
  children: NamedTree[];

  constructor(groups: DefinedGroup[]) {
    this.children = groups;
  }

  static fromJSON(data: MaterialUsageJSON[]): MaterialUsagesVM {
    const mapPipeline = (pipe: string) => {
      return {name: pipe};
    };
    const results     = _.map(data, (usage) => new DefinedGroup(usage.group, _.map(usage.pipelines, mapPipeline)));
    return new MaterialUsagesVM(results);
  }

  name(): string {
    return "Usages";
  }
}
