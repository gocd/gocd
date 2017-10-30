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

require 'rails_helper'

describe ApiV1::StageRepresenter do

  it 'renders an stage with hal representation' do
    stage_model     =StageMother.createPassedStage('pipeline', 1, 'stage', 2, 'job', java.util.Date.new())
    stage_identifier=com.thoughtworks.go.domain.StageIdentifier.new("pipeline", 1, "1", "stage", "2")
    stage_model.setIdentifier(stage_identifier)
    stage_model.setRerunOfCounter(1)

    presenter   = ApiV1::StageRepresenter.new(stage_model)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)

    expect(actual_json).to have_link(:self).with_url('http://test.host/api/stages/pipeline/1/stage/2')
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#get-stage-instance')

    actual_json.delete(:_links)
    expect(actual_json.fetch(:_embedded)).to eq({:jobs => stage_model.getJobInstances().collect { |j| ApiV1::JobSummaryRepresenter.new(j).to_hash(url_builder: UrlBuilder.new) }})
    actual_json.delete(:_embedded)
    expect(actual_json).to eq(stage_hash(stage_model))
  end

  def stage_hash(stage_model)
    {
      name:         'stage',
      result:       StageResult::Passed,
      counter:      2,
      stage_type:   'success',
      rerun_of:     ApiV1::StageSummaryRepresenter.new(stage_summary).to_hash(url_builder: UrlBuilder.new),
      triggered_by: 'changes',
      pipeline:     ApiV1::PipelineSummaryRepresenter.new(stage_model.getIdentifier).to_hash(url_builder: UrlBuilder.new)
    }
  end

  def stage_summary
    OpenStruct.new({
                     pipeline_name:    'pipeline',
                     pipeline_counter: 1,
                     stage_name:       'stage',
                     stage_counter:    1
                   })
  end
end
