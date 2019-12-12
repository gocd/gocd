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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {AgentJobRunHistoryAPIJSON, JobRunHistoryJSON} from "models/agent_job_run_history";
import {Link} from "views/components/link";
import {PaginationWidget} from "views/components/pagination";
import {Pagination} from "views/components/pagination/models/pagination";
import {Spinner} from "views/components/spinner";
import {Table, TableSortHandler} from "views/components/table";
import {AgentJobStateTransitionModal} from "views/pages/agent-job-run-history/agent_job_state_transitions_modal";
import styles from "./index.scss";

const stateTransitionIcon = require("./icon-state-transition.svg");

interface Attrs {
  jobHistory: Stream<AgentJobRunHistoryAPIJSON>;
  onPageChange: (pageNumber: number) => void;
}

interface State {
  sortHandler: JobRunHistoryTableSortHandler;
}

export class AgentJobRunHistoryWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.sortHandler = new JobRunHistoryTableSortHandler(vnode.attrs.jobHistory().jobs);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (!vnode.attrs.jobHistory || !vnode.attrs.jobHistory()) {
      return <Spinner/>;
    }

    const tableData = vnode.attrs.jobHistory().jobs.map((jobHistoryItem) => {
      const pipelineName = jobHistoryItem.pipeline_name;
      const stageName    = jobHistoryItem.stage_name;
      const jobName      = jobHistoryItem.job_name;

      const jobNameLink = <Link
        href={`/go/tab/build/detail/${pipelineName}/${jobHistoryItem.pipeline_counter}/${stageName}/${jobHistoryItem.stage_counter}/${jobName}`}
        target="_blank">
        {jobName}
      </Link>;

      const jobStateTransitionIcon = <img class={styles.jobStateTransitionIconContainer}
                                          onclick={() => new AgentJobStateTransitionModal(jobHistoryItem).render()}
                                          src={stateTransitionIcon}/>;

      return [
        pipelineName, stageName, jobNameLink, jobHistoryItem.result, jobStateTransitionIcon
      ];
    });

    const paginationWidget = <PaginationWidget pagination={Pagination.fromJSON(vnode.attrs.jobHistory().pagination)}
                                               onPageChange={vnode.attrs.onPageChange}/>;

    return (
      <div>
        {paginationWidget}
        <Table data={tableData}
               headers={["Pipeline", "Stage", "Job", "Result", "Job State Transitions"]}
               sortHandler={vnode.state.sortHandler}/>
        {paginationWidget}
      </div>
    );
  }
}

export enum SortOrder {
  ASC, DESC
}

type Foo = "pipeline_name" | "stage_name" | "job_name" | "result";

class JobRunHistoryTableSortHandler implements TableSortHandler {
  private currentSortedColumnIndex: number = 0;
  private jobs: JobRunHistoryJSON[];
  private sortOrder: SortOrder             = SortOrder.ASC;

  constructor(jobs: JobRunHistoryJSON[]) {
    this.jobs = jobs;
  }

  onColumnClick(columnIndex: number): void {
    if (this.currentSortedColumnIndex === columnIndex) {
      this.sortOrder = this.sortOrder === SortOrder.ASC ? SortOrder.DESC : SortOrder.ASC;
    }

    this.currentSortedColumnIndex = columnIndex;
    this.jobs.sort((element1: JobRunHistoryJSON, element2: JobRunHistoryJSON) => {
      return this.compare(element1, element2, columnIndex);
    });
  }

  getSortableColumns(): number[] {
    return [0, 1, 2, 3];
  }

  getCurrentSortOrder(): SortOrder {
    return this.sortOrder;
  }

  private static getColumns(): Foo[] {
    return ["pipeline_name", "stage_name", "job_name", "result"];
  }

  private compare(element1: JobRunHistoryJSON, element2: JobRunHistoryJSON, index: number) {
    const key = JobRunHistoryTableSortHandler.getColumns()[index];

    const comparison = element1[key].toLocaleLowerCase().localeCompare(element2[key].toLocaleLowerCase());

    if (this.sortOrder === SortOrder.ASC) {
      return comparison;
    } else {
      return -comparison;
    }
  }
}
