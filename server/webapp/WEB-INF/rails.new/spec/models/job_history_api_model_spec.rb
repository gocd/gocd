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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')

describe JobHistoryAPIModel do
  include APIModelMother

  before(:each) do
    @pagination_view_model = create_pagination_model
    @job_history_view_model = [create_job_history_model]
  end

  describe "should initialize correctly" do
    it "should populate correct data" do
      job_history_api_model = JobHistoryAPIModel.new(@pagination_view_model, @job_history_view_model)

      job_history_api_model.pagination.page_size.should == 10
      job_history_api_model.pagination.offset.should == 1
      job_history_api_model.pagination.total.should == 100

      job_instance_api_model = job_history_api_model.jobs[0]
      job_instance_api_model.id.should == 5
      job_instance_api_model.name.should == 'job name'
      job_instance_api_model.state.should == 'state'
      job_instance_api_model.result.should == 'result'
      job_instance_api_model.scheduled_date.should == 'scheduled time'
      job_instance_api_model.rerun.should == false
      job_instance_api_model.original_job_id.should == 0
      job_instance_api_model.agent_uuid.should == 'uuid'
      job_instance_api_model.pipeline_name.should == 'pipeline'
      job_instance_api_model.pipeline_counter.should == 1
      job_instance_api_model.stage_name.should == 'stage'
      job_instance_api_model.stage_counter.should == '1'
    end
  end
end