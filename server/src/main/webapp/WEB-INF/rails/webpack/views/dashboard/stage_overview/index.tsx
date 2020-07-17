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
import {MithrilComponent} from "../../../jsx/mithril-component";
import {Spinner} from "../../components/spinner";
import * as styles from "./index.scss";
import {JobsListWidget} from "./jobs_list_widget";
import {StageHeaderWidget} from "./stage_overview_header";
import {StageOverviewViewModel} from "./models/stage_overview_view_model";
import {StageState} from "./models/types";

export interface Attrs {
  pipelineName: string;
  pipelineCounter: string | number;
  stageName: string;
  stageCounter: string | number;
  stageStatus: StageState;
}

export interface State {
  stageOverviewVM: Stream<StageOverviewViewModel | undefined>;
}

export class StageOverview extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.stageOverviewVM = Stream(undefined);

    StageOverviewViewModel.initialize(vnode.attrs.pipelineName, vnode.attrs.pipelineCounter, vnode.attrs.stageName, vnode.attrs.stageCounter, vnode.attrs.stageStatus)
      .then((result) => vnode.state.stageOverviewVM(result as StageOverviewViewModel));
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    if (!vnode.state.stageOverviewVM()) {
      return <div data-test-id="stage-overview-container" className={styles.stageOverviewContainer}>
        <Spinner/>
      </div>;
    }

    return <div data-test-id="stage-overview-container" class={styles.stageOverviewContainer}>
      <StageHeaderWidget stageName={vnode.attrs.stageName}
                         stageCounter={vnode.attrs.stageCounter}
                         pipelineName={vnode.attrs.pipelineName}
                         stageInstance={vnode.state.stageOverviewVM().stageInstance}/>
      <JobsListWidget jobsVM={vnode.state.stageOverviewVM().jobsVM}
                      lastPassedStageInstance={vnode.state.stageOverviewVM().lastPassedStageInstance}/>
    </div>;
  }
}
