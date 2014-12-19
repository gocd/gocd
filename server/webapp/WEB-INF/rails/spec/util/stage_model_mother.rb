##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

module StageModelMother
  def stage(counter)
    StageMother.createPassedStage("cruise", 1, "dev", counter, "rspec", org.joda.time.DateTime.new().plus_minutes(10).toDate())
  end

  def failing_stage(name)
    stage = stage(1)
    stage.fail()
    stage_model_for(stage)
  end

  def stage_model_for(stage)
    StageSummaryModel.new(stage, Stages.new([stage]), JobDurationStrategy::ALWAYS_ZERO, nil)
  end

  def stage_with_three_runs
    stage1 = stage(1)
    stage2 = stage(2)
    stage3 = stage(3)
    stage3.fail();

    StageSummaryModel.new(stage2, Stages.new([stage1, stage2, stage3]), JobDurationStrategy::ALWAYS_ZERO, nil)
  end

  def historical_stages(count)
    history = []
    count.times do |i|
      stage = StageMother.createPassedStage("cruise", i+1, "dev", 1, "rspec", org.joda.time.DateTime.new().plus_minutes(10).toDate())
      stage.setConfigVersion("md5-test")
      history << StageHistoryEntry.new(stage, i, nil)
    end
    history.reverse
  end

  def stage_with_all_jobs_passed
    stage1 = stage(1)

    StageSummaryModel.new(stage1, Stages.new([stage1]), JobDurationStrategy::ALWAYS_ZERO, nil)
  end

  def stage_with_5_jobs
    first = JobInstanceMother.completed("first", JobResult::Failed)
    second = JobInstanceMother.completed("second", JobResult::Passed)
    third = JobInstanceMother.completed("third", JobResult::Passed)
    fourth = JobInstanceMother.building("fourth" )
    fifth = JobInstanceMother.completed("fifth", JobResult::Cancelled)
    stage3=StageMother.custom("pipeline", "stage", JobInstances.new([first,second,third,fourth,fifth]))

    StageSummaryModel.new(stage3, Stages.new([stage3]), JobDurationStrategy::ALWAYS_ZERO, nil)
  end

  def stage_history_page(offset)
    StageHistoryPage.new(historical_stages(10), Pagination.pageStartingAt(offset,100,10), historical_stages(1)[0])
  end

  def last_stage_history_page(offset)
    StageHistoryPage.new(historical_stages(1), Pagination.pageStartingAt(offset,100,10), nil)
  end

  def stage_model(name, counter, job_state = JobState::Completed, job_result = JobResult::Passed)
    job_1 = JobHistory.new
    job_1.addJob("job-1", job_state, job_result, Time.now)
    StageInstanceModel.new(name, counter.to_s, job_1, StageIdentifier.new("cruise", 10, name, counter.to_s))
  end

  def stage_model_with_result(name, counter, stage_result = StageResult::Passed, job_state = JobState::Completed, job_result = JobResult::Passed)
    the_model = StageInstanceModel.new(name, counter.to_s, stage_result, StageIdentifier.new("cruise", 10, name, counter.to_s))
    job_history = the_model.getBuildHistory
    job_history.addJob("job-1", job_state, job_result, Time.now)
    the_model
  end

  def stage_model_with_unknown_state(name)
    NullStageHistoryItem.new(name, false)
  end

  def failing_tests(failures)
    test_runs = StageTestRuns.new(5)
    #int pipelineCounter, String pipelineLabel, String suiteName, String testName, TestStatus testStatus, JobIdentifier jobIdentifier
    failures.each do |failure|
      test_runs.add(failure[:counter], "label-#{failure[:counter]}", failure[:suite], failure[:test], failure[:status], failure[:job_id])
    end
    test_runs
  end

  def stage_history_for *stage_names
    from_sims = StageInstanceModels.new
    stage_names.each do |name|
      from_sims.add(stage_model(name, 35))
    end
    from_sims
  end
end

