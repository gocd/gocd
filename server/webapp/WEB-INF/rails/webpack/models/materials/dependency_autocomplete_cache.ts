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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import SparkRoutes from "helpers/spark_routes";
import * as _ from "lodash";

interface PipelineSuggestion {
  name: string;
  stages: string[];
}

export interface PipelineNameCache<P, S> {
  ready: () => boolean;
  prime: (onComplete: () => void) => void;
  pipelines: () => P[];
  stages: (pipeline: string) => S[];
  failureReason: () => string | undefined;
  failed: () => boolean;
}

export class DependencyMaterialAutocomplete<P, S> implements PipelineNameCache<P, S> {
  private syncing: boolean = false;
  private data?: PipelineSuggestion[];
  private error?: string;
  private toPipeline: (pipelineName: string) => P;
  private toStage: (stageName: string) => S;

  constructor(toPipeline: (pipelineName: string) => P, toStage: (stage: string) => S) {
    this.toPipeline = toPipeline;
    this.toStage = toStage;
  }

  prime(onComplete: () => void, onError?: () => void) {
    if (this.busy()) {
      return;
    }

    this.lock();

    ApiRequestBuilder.GET(SparkRoutes.internalDependencyMaterialSuggestionsPath(), ApiVersion.v1).then((res) => {
      res.do((s) => {
        delete this.error;
        this.data = JSON.parse(s.body) as PipelineSuggestion[];
        onComplete();
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

  pipelines(): P[] {
    return this.ready() ? _.map(this.data!, (entry) => this.toPipeline(entry.name)) : [];
  }

  stages(pipeline: string): S[] {
    if (!this.ready()) {
      return [];
    }
    const current = _.find(this.data!, (entry) => entry.name === pipeline);
    return current ? _.map(current!.stages, this.toStage) : [];
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
