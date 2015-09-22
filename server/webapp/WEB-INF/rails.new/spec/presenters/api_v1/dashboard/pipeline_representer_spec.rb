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

describe ApiV1::Dashboard::PipelineRepresenter do
  include PipelineModelMother

  it 'renders all pipeline groups with hal representation' do
    pipeline    = pipeline_model('pipeline_name', 'pipeline_label')
    presenter   = ApiV1::Dashboard::PipelineRepresenter.new(pipeline)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc, :settings_path, :trigger, :trigger_with_options, :unpause)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/pipelines/pipeline_name/history')
    expect(actual_json).to have_link(:doc).with_url('http://api.go.cd/current/#pipelines')
    expect(actual_json).to have_link(:settings_path).with_url('http://test.host/admin/pipelines/pipeline_name/general')
    expect(actual_json).to have_link(:trigger).with_url('http://test.host/api/pipelines/pipeline_name/schedule')
    expect(actual_json).to have_link(:trigger_with_options).with_url('http://test.host/api/pipelines/pipeline_name/pause')
    expect(actual_json).to have_link(:unpause).with_url('http://test.host/api/pipelines/pipeline_name/unpause')
    actual_json.delete(:_links)
    actual_json.delete(:_embedded).should == {:instances => [expected_embedded_pipeline(presenter.instances.first)]}
    expect(actual_json).to eq(pipelines_hash)
  end

  private

  def expected_embedded_pipeline(pipeline_model)
    ApiV1::Dashboard::PipelineInstanceRepresenter.new(pipeline_model).to_hash(url_builder: UrlBuilder.new)
  end

  def pipelines_hash
    {
      name:         'pipeline_name',
      locked:       false,
      paused_by:    nil,
      pause_reason: nil,
    }
  end

end
