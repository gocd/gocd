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

import {Agents} from "../../../../../models/agents/agents";
import {AgentsTestData} from "../../../../../models/agents/spec/agents_test_data";
import {JobsViewModel, SortableColumn} from "../../models/jobs_view_model";
import {TestData} from "../test_data";

describe('Jobs View Model', () => {

  let jobsVM: JobsViewModel;
  let agents: Agents;

  beforeEach(() => {
    agents = Agents.fromJSON(AgentsTestData.list());
    jobsVM = new JobsViewModel(TestData.jobsJSON(), agents);
  });

  it('should by default sort by job state in descending order', () => {
    expect(jobsVM.isSortedBy(SortableColumn.STATE)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();
  });

  it('should provide sorted list of jobs by job state in descending order', () => {
    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['another-failed-job', 'failed-job', 'cancelled-job', 'passing-job', 'running-job', 'waiting-job'];

    expect(sortedJobNames).toEqual(expected);
  });

  it('should sort in different order when clicked on the same column', () => {
    expect(jobsVM.isSortedAscending()).toBeFalse();
    jobsVM.updateSort(SortableColumn.DURATION);
    expect(jobsVM.isSortedAscending()).toBeTrue();

    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['passing-job', 'another-failed-job', 'cancelled-job', 'failed-job', 'waiting-job', 'running-job'];

    expect(sortedJobNames).toEqual(expected);
  });

  it('should sort by name in ascending order when clicked', () => {
    expect(jobsVM.isSortedBy(SortableColumn.STATE)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    jobsVM.updateSort(SortableColumn.NAME);

    expect(jobsVM.isSortedBy(SortableColumn.NAME)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeTrue();

    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['another-failed-job', 'cancelled-job', 'failed-job', 'passing-job', 'running-job', 'waiting-job'];

    expect(sortedJobNames).toEqual(expected);
  });

  it('should sort by name in descending order when clicked twice', () => {
    expect(jobsVM.isSortedBy(SortableColumn.STATE)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    jobsVM.updateSort(SortableColumn.NAME);

    expect(jobsVM.isSortedBy(SortableColumn.NAME)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeTrue();

    jobsVM.updateSort(SortableColumn.NAME);

    expect(jobsVM.isSortedBy(SortableColumn.NAME)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['waiting-job', 'running-job', 'passing-job', 'failed-job', 'cancelled-job', 'another-failed-job'];

    expect(sortedJobNames).toEqual(expected);
  });

  it('should sort by duration in ascending order when clicked', () => {
    expect(jobsVM.isSortedBy(SortableColumn.STATE)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    jobsVM.updateSort(SortableColumn.DURATION);

    expect(jobsVM.isSortedBy(SortableColumn.DURATION)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeTrue();

    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['passing-job', 'another-failed-job', 'cancelled-job', 'failed-job', 'waiting-job', 'running-job'];

    expect(sortedJobNames).toEqual(expected);
  });

  it('should sort by duration in descending order when clicked twice', () => {
    expect(jobsVM.isSortedBy(SortableColumn.STATE)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    jobsVM.updateSort(SortableColumn.DURATION);

    expect(jobsVM.isSortedBy(SortableColumn.DURATION)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeTrue();

    jobsVM.updateSort(SortableColumn.DURATION);

    expect(jobsVM.isSortedBy(SortableColumn.DURATION)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['running-job', 'waiting-job', 'failed-job', 'cancelled-job', 'another-failed-job', 'passing-job'];

    expect(sortedJobNames).toEqual(expected);
  });

  it('should sort by agent in ascending order when clicked', () => {
    expect(jobsVM.isSortedBy(SortableColumn.STATE)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    jobsVM.updateSort(SortableColumn.AGENT);

    expect(jobsVM.isSortedBy(SortableColumn.AGENT)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeTrue();

    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['waiting-job', 'another-failed-job', 'passing-job', 'running-job', 'failed-job', 'cancelled-job'];

    expect(sortedJobNames).toEqual(expected);
  });

  it('should sort by agent in descending order when clicked twice', () => {
    expect(jobsVM.isSortedBy(SortableColumn.STATE)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    jobsVM.updateSort(SortableColumn.AGENT);

    expect(jobsVM.isSortedBy(SortableColumn.AGENT)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeTrue();

    jobsVM.updateSort(SortableColumn.AGENT);

    expect(jobsVM.isSortedBy(SortableColumn.AGENT)).toBeTrue();
    expect(jobsVM.isSortedAscending()).toBeFalse();

    const sortedJobNames: string[] = jobsVM.getJobs().map((j) => j.name);
    const expected: string[] = ['cancelled-job', 'failed-job', 'running-job', 'passing-job', 'another-failed-job', 'waiting-job'];

    expect(sortedJobNames).toEqual(expected);
  });

});
