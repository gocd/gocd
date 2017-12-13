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

require 'rails_helper'

describe ApiV2::Dashboard::PipelineGroupsRepresenter do
  include GoDashboardPipelineMother

  it 'renders pipeline dashboard with hal representation' do
    user = Username.new(CaseInsensitiveString.new(SecureRandom.hex))
    permissions = Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE)

    pipeline_group1 = GoDashboardPipelineGroup.new('group1', permissions)
    pipeline_group2 = GoDashboardPipelineGroup.new('group2', permissions)

    pipeline1_in_group1 = dashboard_pipeline('pipeline1')
    pipeline2_in_group1 = dashboard_pipeline('pipeline2')
    pipeline3_in_group2 = dashboard_pipeline('pipeline2')

    pipeline_group1.addPipeline(pipeline1_in_group1)
    pipeline_group1.addPipeline(pipeline2_in_group1)
    pipeline_group2.addPipeline(pipeline3_in_group2)

    presenter   = ApiV2::Dashboard::PipelineGroupsRepresenter.new({pipeline_groups: [pipeline_group1, pipeline_group2], user: user})

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/dashboard')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/current/#dashboard')
    actual_json.delete(:_links)
    expect(actual_json.fetch(:_embedded)[:pipeline_groups]).to eq(
                                                                 [expected_embedded_pipeline_groups({pipeline_group: pipeline_group1, user: user}),
                                                                  expected_embedded_pipeline_groups({pipeline_group: pipeline_group2, user: user})]
                                                               )
    expect(actual_json.delete(:_embedded)[:pipelines]).to eq([expected_embedded_pipeline({pipeline: pipeline1_in_group1, user: user}),
                                                              expected_embedded_pipeline({pipeline: pipeline2_in_group1, user: user}),
                                                              expected_embedded_pipeline({pipeline: pipeline3_in_group2, user: user})])
  end

  private

  def expected_embedded_pipeline_groups(model)
    ApiV2::Dashboard::PipelineGroupRepresenter.new(model).to_hash(url_builder: UrlBuilder.new)
  end

  def expected_embedded_pipeline(model)
    ApiV2::Dashboard::PipelineRepresenter.new(model).to_hash(url_builder: UrlBuilder.new)
  end
end