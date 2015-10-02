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

describe ApiV1::Dashboard::StageRepresenter do
  it 'renders stages and previous stage with hal representation' do
    previous_stage_instance = StageInstanceModel.new('stage1', '1', StageResult::Cancelled, StageIdentifier.new('pipeline-name', 23, 'stage', '1'))
    stage_instance          = StageInstanceModel.new('stage2', '2', StageResult::Cancelled, StageIdentifier.new('pipeline-name', 23, 'stage', '2'))
    stage_instance.setPreviousStage(previous_stage_instance)

    stage_presenter_args = [stage_instance, {
                                            pipeline_name:    'pipeline-name',
                                            pipeline_counter: 2,
                                            render_previous:  true
                                          }]

    presenter   = ApiV1::Dashboard::StageRepresenter.new(stage_presenter_args)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/stages/pipeline-name/2/stage2/2')
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/current/#get-stage-instance')

    actual_json.delete(:_links)
    expect(actual_json.delete(:previous_stage)).to eq(ApiV1::Dashboard::StageRepresenter.new(presenter.previous_stage).to_hash(url_builder: UrlBuilder.new))
    expect(actual_json).to eq({name: 'stage2', status: StageState::Unknown})
  end

  it 'renders stages (without previous stage) with hal representation' do
    stage_instance = StageInstanceModel.new('stage2', '2', StageResult::Cancelled, StageIdentifier.new('pipeline-name', 23, 'stage', '2'))

    stage_value =[stage_instance, {
                                  pipeline_name:    'pipeline-name',
                                  pipeline_counter: 2,
                                  render_previous:  true}
    ]
    presenter   = ApiV1::Dashboard::StageRepresenter.new(stage_value)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc,)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/stages/pipeline-name/2/stage2/2')
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/current/#get-stage-instance')

    actual_json.delete(:_links)
    expect(actual_json.delete(:previous_stage)).to eq(nil)
    expect(actual_json).to eq({name: 'stage2', status: StageState::Unknown})
  end

end