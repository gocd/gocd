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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import {AbstractObjCache, ObjectCache, rejectAsString} from "models/base/cache";

interface PipelineSuggestion {
  name: string;
  stages: string[];
}

export interface PipelineNameCache<P, S> extends ObjectCache<PipelineSuggestion[]> {
  pipelines: () => P[];
  stages: (pipeline: string) => S[];
}

export class DependencyMaterialAutocomplete<P, S> extends AbstractObjCache<PipelineSuggestion[]> implements PipelineNameCache<P, S> {
  private toPipeline: (pipelineName: string) => P;
  private toStage: (stageName: string) => S;

  constructor(toPipeline: (pipelineName: string) => P, toStage: (stage: string) => S) {
    super();
    this.toPipeline = toPipeline;
    this.toStage = toStage;
  }

  doFetch(resolve: (data: PipelineSuggestion[]) => void, reject: (reason: string) => void) {
    ApiRequestBuilder.GET(SparkRoutes.internalDependencyMaterialSuggestionsPath(), ApiVersion.v1).then((res) => {
      res.do((s) => {
        resolve(JSON.parse(s.body));
      }, (e) => {
        reject(JSON.parse(e.body!).message);
      });
    }).catch(rejectAsString(reject));
  }

  pipelines(): P[] {
    return this.ready() ? _.map(this.contents(), (entry) => this.toPipeline(entry.name)) : [];
  }

  stages(pipeline: string): S[] {
    if (!this.ready()) {
      return [];
    }
    const current = _.find(this.contents(), (entry) => entry.name === pipeline);
    return current ? _.map(current!.stages, this.toStage) : [];
  }
}
