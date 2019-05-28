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

describe AboutHelper do
  include AboutHelper

  def pipeline_config_service
    @pipeline_config_service
  end

  it "should get the pipelines count" do
    @pipeline_config_service = double('pipeline_config_service')
    expect(@pipeline_config_service).to receive(:totalPipelinesCount).and_return(10)

    expect(total_pipelines_count).to eq(10)
  end
end
