##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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

describe ApiV2::Dashboard::PipelineGroupRepresenter do
  include GoDashboardPipelineMother

  it 'renders pipeline group with hal representation' do
    pipeline1 = dashboard_pipeline("pipeline1", "group1")
    pipeline2 = dashboard_pipeline("pipeline2", "group1")

    model       = {:name => 'group1', :pipelines => [pipeline1, pipeline2]}
    presenter   = ApiV2::Dashboard::PipelineGroupRepresenter.new(model)

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/config/pipeline_groups')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/current/#pipeline-groups')
    actual_json.delete(:_links)
    actual_json.delete(:_embedded)[:pipelines].should == [expected_embedded_pipeline(pipeline1.model()),
                                                          expected_embedded_pipeline(pipeline2.model())]
    expect(actual_json).to eq({name: 'group1'})
  end

  private

  def expected_embedded_pipeline(pipeline_model)
    ApiV2::Dashboard::PipelineRepresenter.new(pipeline_model).to_hash(url_builder: UrlBuilder.new)
  end
end