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
import {AgentJobRunHistoryAPI, AgentJobRunHistoryAPIJSON} from "models/agent_job_run_history";
import {Agent} from "models/agents/agents";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AgentJobRunHistoryWidget} from "views/pages/agent-job-run-history/agent_job_run_history_widget";
import {Page, PageState} from "views/pages/page";

interface State {
  jobHistory: Stream<AgentJobRunHistoryAPIJSON>;
  agent: Stream<Agent>;
}

export class AgentJobRunHistoryPage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (this.pageState === PageState.FAILED) {
      return <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>;
    }

    return <div>
      <AgentJobRunHistoryWidget jobHistory={vnode.state.jobHistory}
                                onPageChange={(pageNumber: number) => {
                                  this.fetchJobHistoryForPage(vnode, pageNumber);
                                  return false;
                                }}/>
    </div>;
  }

  pageName() {
    return "Agent Job Run History";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    vnode.state.jobHistory = Stream();
    vnode.state.agent      = Stream();
    return this.fetchJobHistoryForPage(vnode, 1);
  }

  private fetchJobHistoryForPage(vnode: m.Vnode<null, State>, pageNumber: number) {
    const pageSize = vnode.state.jobHistory() ? vnode.state.jobHistory().pagination.page_size : 0;
    const offset   = (pageNumber - 1) * pageSize;

    this.pageState = PageState.LOADING;
    return Promise.all([AgentJobRunHistoryAPI.all(this.getMeta().uuid, offset)])
                  .then((results) => {
                    results[0].do((successResponse) => {
                      this.pageState = PageState.OK;
                      vnode.state.jobHistory(successResponse.body);
                    }, (errorResponse) => {
                      this.setErrorState();
                      this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      throw errorResponse;
                    });
                  });
  }
}
