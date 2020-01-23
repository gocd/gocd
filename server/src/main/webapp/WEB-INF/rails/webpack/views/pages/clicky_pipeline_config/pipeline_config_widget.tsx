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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {PipelineConfig} from "models/new_pipeline_configs/pipeline_config";
import styles from "./index.scss";
import {NavigationWidget} from "./navigation_widget";

interface Attrs {
  pipelineConfig: PipelineConfig;
}

export class PipelineConfigWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div class={styles.mainContainer}>
      <div class={styles.navigation}>
        <NavigationWidget pipelineConfig={vnode.attrs.pipelineConfig}/>
      </div>
      <div class={styles.entityConfigContainer}>Right</div>
    </div>;
  }
}
