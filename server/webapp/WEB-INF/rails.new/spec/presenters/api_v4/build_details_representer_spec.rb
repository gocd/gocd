##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################

require 'spec_helper'

describe ApiV4::BuildDetailsRepresenter do

  it 'renders build related information in hal representation' do
    actual_json = ApiV4::BuildDetailsRepresenter.new(AgentBuildingInfo.new('buildInfo', 'pipeline1/1/stage1/1/job')).to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:job, :stage, :pipeline)
    expect(actual_json).to have_link(:job).with_url('http://test.host/tab/build/detail/pipeline1/1/stage1/1/job')
    expect(actual_json).to have_link(:stage).with_url('http://test.host/pipelines/pipeline1/1/stage1/1')
    expect(actual_json).to have_link(:pipeline).with_url('http://test.host/tab/pipeline/history/pipeline1')

    actual_json.delete(:_links)
    expect(actual_json).to eq({
                                  pipeline_name: 'pipeline1',
                                  stage_name: 'stage1',
                                  job_name: 'job'
                              })
  end
end