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

describe ApiV1::Dashboard::PipelineGroupRepresenter do
  include PipelineModelMother

  it 'renders pipeline dashboard  with hal representation' do
    pipeline_group = PipelineGroupModel.new('MyPipelines')
    pipeline_model = pipeline_model('pipeline-name', 'pipeline-label')
    pipeline_group.add(pipeline_model)
    presenter   = ApiV1::Dashboard::PipelineGroupRepresenter.new(pipeline_group)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/config/pipeline_groups')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#pipeline-groups')
    actual_json.delete(:_links)
    actual_json.delete(:_embedded)[:pipelines].should == [expected_embedded_pipeline(pipeline_model)]
    expect(actual_json).to eq({name: 'MyPipelines'})
  end

  private

  def expected_embedded_pipeline(pipeline_model)
    ApiV1::Dashboard::PipelineRepresenter.new(pipeline_model).to_hash(url_builder: UrlBuilder.new)
  end

end
