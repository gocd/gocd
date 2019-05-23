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

import * as m from "mithril";
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityCRUD} from "models/pipeline_activity/pipeline_activity_crud";
import {PipelineActivityWidget} from "views/pages/pipeline_activity/pipeline_activity_widget";
import {Page, PageState} from "views/pages/page";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";

interface State {
  pipelineActivity: Stream<PipelineActivity>;
}

export class PipelineActivityPage extends Page<null, State> {

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.pipelineActivity = stream();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <PipelineActivityWidget pipelineActivity={vnode.state.pipelineActivity}/>;
  }

  pageName(): string {
    return "Pipeline Activity";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.resolve(PipelineActivityCRUD.get("up42")).then((result) => {
      result.do((successResponse) => {
        this.pageState = PageState.OK;
        vnode.state.pipelineActivity(successResponse.body);
      }, this.setErrorState);
    });
  }
}
