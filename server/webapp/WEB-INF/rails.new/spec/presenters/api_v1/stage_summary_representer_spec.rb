##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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

describe ApiV1::StageSummaryRepresenter do

  it 'renders an stage with hal representation' do

    presenter   = ApiV1::StageSummaryRepresenter.new(stage)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/stages/pipeline/1/stage/2')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#get-stage-instance')

    actual_json.delete(:_links)
    expect(actual_json).to eq(stage_hash)
  end

  def stage_hash
    {
      name:    'stage',
      counter: 2
    }
  end

  def stage
    OpenStruct.new({
                     pipeline_name:    'pipeline',
                     pipeline_counter: 1,
                     stage_name:       'stage',
                     stage_counter:    2
                   })
  end
end
