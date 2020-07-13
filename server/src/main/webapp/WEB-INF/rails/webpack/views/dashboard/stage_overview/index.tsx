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
import Stream from "mithril/stream";
import * as styles from "./index.scss";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {StageHeaderWidget} from "./stage_overview_header";
import {StageInstance} from "./models/stage_instance";
import {Spinner} from "../../components/spinner";
import {JobsListWidget} from "./jobs_list_widget";

export interface Attrs {
  pipelineName: string;
  pipelineCounter: string | number;
  stageName: string;
  stageCounter: string | number;
}

export interface State {
  stageInstance: Stream<StageInstance | undefined>;
}

export class StageOverview extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.stageInstance = Stream(undefined);

    StageInstance.get(vnode.attrs.pipelineName, vnode.attrs.pipelineCounter, vnode.attrs.stageName, vnode.attrs.stageCounter)
      .then((result) => result.do((successResponse) => {
        vnode.state.stageInstance(successResponse.body);
      }));
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    if (!vnode.state.stageInstance()) {
      return <div data-test-id="stage-overview-container" className={styles.stageOverviewContainer}>
        <Spinner/>
      </div>
    }

    return <div data-test-id="stage-overview-container" class={styles.stageOverviewContainer}>
      <StageHeaderWidget stageName={vnode.attrs.stageName}
                         stageCounter={vnode.attrs.stageCounter}
                         pipelineName={vnode.attrs.pipelineName}
                         stageInstance={vnode.state.stageInstance}/>
    <JobsListWidget stageInstance={vnode.state.stageInstance}/>
    </div>
  }
}
