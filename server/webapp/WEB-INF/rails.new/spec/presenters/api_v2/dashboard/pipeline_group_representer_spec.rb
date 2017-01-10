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
    permissions = Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE)
    pipeline1 = dashboard_pipeline('pipeline1')
    pipeline2 = dashboard_pipeline('pipeline2')

    pipeline_group = GoDashboardPipelineGroup.new('group1', permissions)
    pipeline_group.addPipeline(pipeline1)
    pipeline_group.addPipeline(pipeline2)

    presenter = ApiV2::Dashboard::PipelineGroupRepresenter.new({pipeline_group: pipeline_group, user: Username.new(CaseInsensitiveString.new(SecureRandom.hex))})

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/config/pipeline_groups')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/current/#pipeline-groups')
    actual_json.delete(:_links)
    expect(actual_json).to eq({name: 'group1', pipelines: ['pipeline1', 'pipeline2'], can_administer: true})
  end

  it 'renders pipeline group authorization information' do
    no_admin_permissions = Permissions.new(Everyone.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE)
    pipeline1 = dashboard_pipeline('pipeline1')
    pipeline2 = dashboard_pipeline('pipeline2')

    pipeline_group = GoDashboardPipelineGroup.new('group1', no_admin_permissions)
    pipeline_group.addPipeline(pipeline1)
    pipeline_group.addPipeline(pipeline2)

    presenter = ApiV2::Dashboard::PipelineGroupRepresenter.new({pipeline_group: pipeline_group, user: Username.new(CaseInsensitiveString.new(SecureRandom.hex))})

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/config/pipeline_groups')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/current/#pipeline-groups')
    actual_json.delete(:_links)
    expect(actual_json).to eq({name: 'group1', pipelines: ['pipeline1', 'pipeline2'], can_administer: false})
  end

  private

  def expected_embedded_pipeline(pipeline_model)
    ApiV2::Dashboard::PipelineRepresenter.new(pipeline_model).to_hash(url_builder: UrlBuilder.new)
  end
end