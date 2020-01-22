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
import {PipelineConfig} from "models/pipeline_config/pipeline_config";
import {PipelineConfigWidget} from "views/pages/pipeline_config/pipeline_config_widget";
import {Page} from "views/pages/page";

interface State {
  dummy?: PipelineConfig;
}

export class PipelineConfigPage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <PipelineConfigWidget/>;
  }

  pageName(): string {
    return "SPA Name goes here!";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    // to be implemented
    return Promise.resolve();
  }
}
