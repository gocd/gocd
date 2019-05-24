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
import {Option} from "views/components/forms/input_fields";

interface Template {
  name: string;
}

export interface TemplateCache<G> {
  ready: () => boolean;
  prime: (onComplete: () => void) => void;
  templates: () => G[];
  failureReason: () => string | undefined;
  failed: () => boolean;
}

export class TemplatesCache<G> implements TemplateCache<G> {
  private syncing: boolean = false;
  private data?: Template[];
  private error?: string;
  private toTemplate: (templateName: string) => G;

  constructor(toTemplate: (templateName: string) => G) {
    this.toTemplate = toTemplate;
  }

  prime(onComplete: () => void, onError?: () => void) {
    if (this.busy()) {
      return;
    }

    this.lock();

    ApiRequestBuilder.GET(SparkRoutes.templatesPath(), ApiVersion.v4).then((res) => {
      res.do((s) => {
        delete this.error;
        this.data = JSON.parse(s.body)._embedded.templates as Template[];
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

  templates(): G[] {
    return this.ready() ? _.map(this.data!, (entry) => this.toTemplate(entry.name)) : [];
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

export class DefaultTemplatesCache extends TemplatesCache<Option> {
  constructor() {
    super((templateName: string) => ({id: templateName, text: templateName} as Option));
  }
}
