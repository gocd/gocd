#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe JobInstanceAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should clear correct data" do
      job_instance_api_model = JobInstanceAPIModel.new(create_job_model)

      expect(job_instance_api_model.pipeline_name).to eq('pipeline')
      expect(job_instance_api_model.pipeline_counter).to eq(123)
      expect(job_instance_api_model.stage_name).to eq('stage')
      expect(job_instance_api_model.stage_counter).to eq('1')

      job_instance_api_model.clear_pipeline_and_stage_details

      expect(job_instance_api_model.pipeline_name).to eq(nil)
      expect(job_instance_api_model.pipeline_counter).to eq(nil)
      expect(job_instance_api_model.stage_name).to eq(nil)
      expect(job_instance_api_model.stage_counter).to eq(nil)
    end
  end
end
