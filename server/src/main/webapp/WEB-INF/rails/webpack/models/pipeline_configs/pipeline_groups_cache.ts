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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import {AbstractObjCache, ObjectCache, rejectAsString} from "models/base/cache";
import {Option} from "views/components/forms/input_fields";

interface PipelineGroup {
  name: string;
}

export interface PipelineGroupCache<G> extends ObjectCache<PipelineGroup[]> {
  pipelineGroups: () => G[];
}

export class PipelineGroupsCache<G> extends AbstractObjCache<PipelineGroup[]> {
  private toPipelineGroup: (groupName: string) => G;

  constructor(toPipelineGroup: (groupName: string) => G) {
    super();
    this.toPipelineGroup = toPipelineGroup;
  }

  doFetch(resolve: (data: PipelineGroup[]) => void, reject: (reason: string) => void) {
    PipelineGroupCRUD.all().then((res) => {
      res.do((s) => {
        resolve(s.body);
      }, (e) => {
        reject(e.message);
      });
    }).catch(rejectAsString(reject));
  }

  pipelineGroups(): G[] {
    return this.ready() ? _.map(this.contents(), (entry) => this.toPipelineGroup(entry.name)) : [];
  }
}

export class DefaultCache extends PipelineGroupsCache<Option> {
  constructor() {
    super((groupName: string) => ({id: groupName, text: groupName} as Option));
  }

  pipelineGroups() {
    if (this.ready() && !super.pipelineGroups().length) {
      return [{id: "defaultGroup", text: "defaultGroup"}]; // when there are no groups available, return "defaultGroup"
    }

    return super.pipelineGroups();
  }
}

export class PipelineGroupCRUD {
  static create(group: PipelineGroup): Promise<ApiResult<PipelineGroup>> {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineGroupsPath(), ApiVersion.latest, {
      payload: group
    }).then((result) => {
      return result.map((str) => {
        return JSON.parse(str) as PipelineGroup;
      });
    });
  }

  static all(): Promise<ApiResult<PipelineGroupJSON[]>> {
    return ApiRequestBuilder.GET(SparkRoutes.pipelineGroupsPath(), ApiVersion.latest)
                            .then((result: ApiResult<string>) => {
                              return result.map((str) => {
                                const data = JSON.parse(str);
                                return data._embedded.groups as PipelineGroupJSON[];
                              });
                            });
  }
}

interface PipelineGroupJSON {
  name: string;
  authorization?: any;
}
