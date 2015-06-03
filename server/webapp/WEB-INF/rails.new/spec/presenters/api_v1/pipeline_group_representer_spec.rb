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

describe ApiV1::PipelineGroupRepresenter do
  include PipelineModelMother

  before (:each) do
    pipeline_group = PipelineGroupModel.new('MyPipelines')
    pipeline_model = pipeline_model('pipeline-name', 'pipeline-label')
    pipeline_group.add(pipeline_model)
    presenter   = ApiV1::PipelineGroupRepresenter.new(pipeline_group)
    actual_json = JSON.parse(presenter.to_json(url_builder: UrlBuilder.new))

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url(expected_self_links)
    expect(actual_json).to have_link(:doc).with_url('http://www.go.cd/documentation/user/current/api/v1/pipeline_group_api.html')
    actual_json.delete(:_embedded)[:pipelines].should == [expected_embedded_pipeline(pipeline_model)]
    expect(actual_json).to eq({name: 'PipelineGroupName1'})
  end

  private

  def expected_self_links
    [
      {content_type: ApiV1::BaseRepresenter::CONTENT_TYPE_API_V1, href: 'http://test.host/api/config/pipeline_groups'},
    ]
  end

  def expected_embedded_pipeline(pipeline_model)
    ApiV1::PipelineRepresenter.new(pipeline_model).to_hash(url_builder: UrlBuilder.new)
  end

end
