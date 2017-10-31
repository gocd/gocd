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

module JobMother

  def assigned_job
    JobInstanceModel.new(JobInstanceMother.assigned("job"), com.thoughtworks.go.server.domain.JobDurationStrategy::ALWAYS_ZERO)
  end

  def jobs_model
    jobs =  []
    [JobInstanceMother.completed("first", JobResult::Failed),
     JobInstanceMother.completed("second", JobResult::Passed),
     JobInstanceMother.scheduled("third"),
     JobInstanceMother.building("fourth" ),
     JobInstanceMother.completed("fifth", JobResult::Cancelled)
    ].each_with_index do |job, j|
      job.setIdentifier(JobIdentifier.new("blah-pipeline", 1, "blah-label", "blah-stage", "2", job.getName()))
      i = j+1
      agent = job.isAssignedToAgent()?
              AgentInstanceMother.updateLocation(
              AgentInstanceMother.updateIpAddress(
              AgentInstanceMother.updateHostname(
                      AgentInstanceMother.updateUuid(AgentInstanceMother.building(), "agent#{i}"), "host#{i}"), "#{i}.#{i}.#{i}.#{i}"), "location-#{i}") : nil
      jobs << JobInstanceModel.new(job, JobDurationStrategy::ConstantJobDuration.new(100), agent)
    end
    return jobs
  end

   def jobs_with_long_and_short_name
    job1 = JobInstanceMother.completed("iamverysmartandiamproudofit", JobResult::Passed)
    job2 = JobInstanceMother.completed("foo", JobResult::Passed)
    jobs = [JobInstanceModel.new(job1, JobDurationStrategy::ConstantJobDuration.new(100), AgentInstanceMother.idle),
            JobInstanceModel.new(job2, JobDurationStrategy::ConstantJobDuration.new(100), AgentInstanceMother.idle)]
    return jobs
   end

  def job_id job_name ="job_name", pipeline_counter = 3, stage_counter = 2, stage_name = "stage_name", pipeline_name = "pipeline_name"
    JobIdentifier.new(pipeline_name, pipeline_counter, "label-#{pipeline_counter}", stage_name, stage_counter.to_s, job_name)
  end

  def job_instance name
    JobInstanceMother.completed(name, JobResult::Passed)
  end
end