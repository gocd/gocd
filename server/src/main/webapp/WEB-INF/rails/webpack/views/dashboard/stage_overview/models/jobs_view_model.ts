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

import Stream from 'mithril/stream';
import {ApiRequestBuilder, ApiVersion} from "../../../../helpers/api_request_builder";
import {SparkRoutes} from "../../../../helpers/spark_routes";
import {Agents} from "../../../../models/agents/agents";
import {JobDurationStrategyHelper} from "./job_duration_stratergy_helper";
import {JobJSON, Result} from "./types";

export enum SortOrder {
  ASC,
  DESC,
  UNSORTED
}

export enum SortableColumn {
  NAME,
  STATE,
  DURATION,
  AGENT
}

export class JobsViewModel {
  public readonly checkedState: Map<string, Stream<boolean>> = new Map();
  private jobs: JobJSON[];
  private agents: Agents;
  private readonly sortableColumn: Stream<SortableColumn>;
  private readonly sortOrder: Stream<SortOrder>;

  constructor(jobs: JobJSON[], agents: Agents) {
    this.sortableColumn = Stream<SortableColumn>(SortableColumn.STATE);
    this.sortOrder = Stream<SortOrder>(SortOrder.DESC);
    this.jobs = jobs;
    this.agents = agents;
    jobs.forEach(job => this.checkedState.set(job.name, Stream<boolean>(false)));
  }

  getJobs(): JobJSON[] {
    let sorted: JobJSON[];

    switch (this.sortableColumn()) {
      case SortableColumn.NAME:
        sorted = this.sortByName();
        break;
      case SortableColumn.STATE:
        sorted = this.sortByState();
        break;
      case SortableColumn.DURATION:
        sorted = this.sortByDuration();
        break;
      case SortableColumn.AGENT:
        sorted = this.sortByAgent();
        break;
      default:
        sorted = this.jobs;
    }

    return this.isSortedAscending() ? sorted : sorted.reverse();
  }

  buildingJobNames(): JobJSON[] {
    return this.getJobNamesByResult(Result.Unknown);
  }

  failedJobNames(): JobJSON[] {
    return this.getJobNamesByResult(Result.Failed).concat(this.getJobNamesByResult(Result.Cancelled));
  }

  passedJobNames(): JobJSON[] {
    return this.getJobNamesByResult(Result.Passed);
  }

  getCheckedJobNames(): string[] {
    const checkedJobNames: string[] = [];
    this.checkedState.forEach((value: Stream<boolean>, key: string) => {
      if (value() === true) {
        checkedJobNames.push(key);
      }
    });

    return checkedJobNames;
  }

  rerunFailedJobs(pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number) {
    return ApiRequestBuilder.POST(SparkRoutes.rerunFailedJobs(pipelineName, pipelineCounter, stageName, stageCounter), ApiVersion.latest);
  }

  rerunSelectedJobs(pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number) {
    const jobs = this.getCheckedJobNames();
    return ApiRequestBuilder.POST(SparkRoutes.rerunSelectedJobs(pipelineName, pipelineCounter, stageName, stageCounter),
      ApiVersion.latest, {payload: {jobs}});
  }

  // responsible to update the view model
  update(jobs: JobJSON[], agents: Agents) {
    this.jobs = jobs;
    this.agents = agents;
  }

  updateSort(column: SortableColumn) {
    if (this.isSortedBy(column)) {
      this.sortOrder() === SortOrder.ASC ? this.sortOrder(SortOrder.DESC) : this.sortOrder(SortOrder.ASC);
      return;
    }

    this.sortableColumn(column);
    this.sortOrder(SortOrder.ASC);
  }

  isSortedBy(column: SortableColumn) {
    return this.sortableColumn() === column;
  }

  getSortType(column: SortableColumn): string {
    if (this.isSortedBy(column)) {
      return SortOrder[this.sortOrder()].toLowerCase();
    }

    return SortOrder[SortOrder.UNSORTED].toLowerCase();
  }

  isSortedAscending() {
    return this.sortOrder() === SortOrder.ASC;
  }

  private getJobNamesByResult(result: Result) {
    return this.jobs.filter((j: JobJSON) => j.result === Result[result]);
  }

  private sortByName() {
    return this.jobs.sort((first: JobJSON, next: JobJSON) => {
      if (first.name < next.name) {
        return -1;
      }

      if (first.name > next.name) {
        return 1;
      }

      return 0;
    });
  }

  private sortByState() {
    return this.jobs.sort((first: JobJSON, next: JobJSON) => {
      const priority = [
        Result[Result.Failed],
        Result[Result.Cancelled],
        Result[Result.Unknown],
        Result[Result.Passed]
      ];

      const firstPriority = priority.indexOf(first.result as string);
      const nextPriority = priority.indexOf(next.result as string);

      if (firstPriority < nextPriority) {
        return 1;
      }

      if (firstPriority > nextPriority) {
        return -1;
      }

      return 0;
    });
  }

  private sortByDuration() {
    return this.jobs.sort((first: JobJSON, next: JobJSON) => {
      const firstDuration = JobDurationStrategyHelper.getJobDuration(first) || 0;
      const nextDuration = JobDurationStrategyHelper.getJobDuration(next) || 0;

      if (firstDuration < nextDuration) {
        return 1;
      }

      if (firstDuration > nextDuration) {
        return -1;
      }

      return 0;
    });
  }

  private sortByAgent() {
    return this.jobs.sort((first: JobJSON, next: JobJSON) => {
      const firstAgent = this.agents.hasAgent(first.agent_uuid!) ? this.agents.getAgent(first.agent_uuid!)!.hostname : (first.agent_uuid || 'zz');
      const nextAgent = this.agents.hasAgent(next.agent_uuid!) ? this.agents.getAgent(next.agent_uuid!)!.hostname : (next.agent_uuid || 'zz');

      if (firstAgent < nextAgent) {
        return 1;
      }

      if (firstAgent > nextAgent) {
        return -1;
      }

      return 0;
    });
  }
}
