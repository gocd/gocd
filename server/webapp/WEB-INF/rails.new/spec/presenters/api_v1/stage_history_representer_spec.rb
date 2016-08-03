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

describe ApiV1::StageHistoryRepresenter do

  it 'renders an stages with hal representation' do
    stage_model     =StageMother.createPassedStage('pipeline', 1, 'stage', 2, 'job', java.util.Date.new())
    stage_identifier=com.thoughtworks.go.domain.StageIdentifier.new("pipeline", 1, "1", "stage", "2")
    stage_model.setIdentifier(stage_identifier)
    stage_model.setRerunOfCounter(1)

    presenter   = ApiV1::StageHistoryRepresenter.new([stage_model], {pipeline_name: 'stage', stage_name: 'pipeline'})
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/stages/stage/pipeline')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#get-stage-history')

    actual_json.delete(:_links)
    actual_json.fetch(:_embedded).should == {:stages => [ApiV1::StageRepresenter.new(stage_model).to_hash(url_builder: UrlBuilder.new)]}
    actual_json.delete(:_embedded)
  end
end
