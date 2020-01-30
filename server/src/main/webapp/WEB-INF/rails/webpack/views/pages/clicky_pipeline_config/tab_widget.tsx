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

import m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";

export abstract class TabWidget {
  //render only selected tab
  public content(entity: PipelineConfig, templateConfig: TemplateConfig, isSelectedTab: boolean): m.Children {
    if (isSelectedTab) {
      return this.renderer(entity, templateConfig);
    }
    return <div> Not a selected tab</div>;
  }

  public abstract name(): string;

  protected abstract renderer(entity: PipelineConfig, templateConfig: TemplateConfig): m.Children;
}
