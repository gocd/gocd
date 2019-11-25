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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineActivity} from "models/pipeline_activity/pipeline_activity";
import {PipelineActivityService} from "models/pipeline_activity/pipeline_activity_crud";
import {MessageType} from "views/components/flash_message";
import {Page} from "views/pages/page";
import {ResultAwarePage} from "views/pages/page_operations";
import {PipelineActivityWidget} from "views/pages/pipeline_activity/pipeline_activity_widget";

interface State {
  pipelineActivity: Stream<PipelineActivity>;
  showBuildCaseFor: Stream<string>;
}

export class PipelineActivityPage extends Page<null, State> implements ResultAwarePage<PipelineActivity>, State {
  pipelineActivity                         = Stream<PipelineActivity>();
  showBuildCaseFor                         = Stream<string>();
  private service: PipelineActivityService = new PipelineActivityService();

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <PipelineActivityWidget pipelineActivity={vnode.state.pipelineActivity}
                                   showBuildCaseFor={vnode.state.showBuildCaseFor}/>;
  }

  pageName(): string {
    return "Pipeline Activity";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.service.activities("Foo", this);
    return Promise.resolve();
  }

  onFailure(message: string) {
    this.flashMessage.setMessage(MessageType.alert, message);
    this.setErrorState();
  }

  onSuccess(data: PipelineActivity) {
    this.pipelineActivity(data);
  }
}
