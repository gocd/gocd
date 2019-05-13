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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import SparkRoutes from "helpers/spark_routes";
import * as _ from "lodash";
import {Option} from "views/components/forms/input_fields";

interface PipelineGroup {
  name: string;
}

export interface PipelineGroupCache<G> {
  ready: () => boolean;
  prime: (onSuccess: () => void, onError?: () => void) => void;
  pipelineGroups: () => G[];
  failureReason: () => string | undefined;
  failed: () => boolean;
}

export class PipelineGroupsCache<G> implements PipelineGroupCache<G> {
  private syncing: boolean = false;
  private data?: PipelineGroup[];
  private error?: string;
  private toPipelineGroup: (groupName: string) => G;

  constructor(toPipelineGroup: (groupName: string) => G) {
    this.toPipelineGroup = toPipelineGroup;
  }

  prime(onSuccess: () => void, onError?: () => void) {
    if (this.busy()) {
      return;
    }

    this.lock();

    delete this.error;

    ApiRequestBuilder.GET(SparkRoutes.pipelineGroupsListPath(), ApiVersion.v1).then((res) => {
      res.do((s) => {
        this.data = JSON.parse(s.body)._embedded.groups as PipelineGroup[];
        onSuccess();
      }, (e) => {
        this.error = e.message;
        if (onError) {
          onError();
        }
      });
    }).finally(() => {
      this.release();
    });
  }

  pipelineGroups(): G[] {
    return this.ready() ? _.map(this.data!, (entry) => this.toPipelineGroup(entry.name)) : [];
  }

  failed(): boolean {
    return !!this.error;
  }

  failureReason(): string | undefined {
    return this.error;
  }

  ready(): boolean {
    return !!this.data;
  }

  private busy(): boolean {
    return this.syncing;
  }

  private lock(): void {
    this.syncing = true;
  }

  private release(): void {
    this.syncing = false;
  }
}

export class DefaultCache extends PipelineGroupsCache<Option> {
  constructor() {
    super((groupName: string) => ({id: groupName, text: groupName} as Option));
  }
}

export class PipelineGroupCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static all(): Promise<ApiResult<PipelineGroup[]>> {
    return ApiRequestBuilder.GET(SparkRoutes.pipelineGroupsListPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((str) => {
                                const data = JSON.parse(str);
                                return data._embedded.groups as PipelineGroup[];
                              });
                            });
  }
}
