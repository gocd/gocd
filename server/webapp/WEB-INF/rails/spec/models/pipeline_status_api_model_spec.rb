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

describe PipelineStatusAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pipeline_status_view_model = create_pipeline_status_model
      pipeline_status_api_model = PipelineStatusAPIModel.new(@pipeline_status_view_model)

      expect(pipeline_status_api_model.paused).to eq(true)
      expect(pipeline_status_api_model.pausedCause).to eq('Pausing it for some reason')
      expect(pipeline_status_api_model.pausedBy).to eq('admin')
      expect(pipeline_status_api_model.locked).to eq(true)
      expect(pipeline_status_api_model.schedulable).to eq(true)
    end
  end
end
