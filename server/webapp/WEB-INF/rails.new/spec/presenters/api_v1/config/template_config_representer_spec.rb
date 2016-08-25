##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe ApiV1::Config::TemplateConfigRepresenter do
  before :each do
    @template = PipelineTemplateConfig.new(CaseInsensitiveString.new('some-template'), StageConfig.new(CaseInsensitiveString.new('stage'), JobConfigs.new(JobConfig.new(CaseInsensitiveString.new('job')))))
  end

  it 'should render a template with hal representation' do
    actual_json = ApiV1::Config::TemplateConfigRepresenter.new(@template).to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_link(:self).with_url(UrlBuilder.new.apiv1_admin_template_url(template_name: @template.name.to_s))
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#template-config')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/templates/:template_name')
    actual_json.delete(:_links)

    expect(actual_json).to eq(template_hash)
  end

  it 'should deserialize given json to PipelineTemplateConfig object' do
    deserialized_scm = PipelineTemplateConfig.new
    ApiV1::Config::TemplateConfigRepresenter.new(deserialized_scm).from_hash(template_hash)
    expect(deserialized_scm).to eq(@template)
  end

  private
  def template_hash
    {
        name: 'some-template',
        stages: [
            {
                name: "stage",
                fetch_materials: true,
                clean_working_directory: false,
                never_cleanup_artifacts: false,
                approval: {
                    type: "success",
                    authorization: {
                        roles: [],
                        users: []
                    }
                },
                environment_variables: [],
                :jobs => [
                    {
                        name: "job",
                        run_instance_count: nil,
                        timeout: nil,
                        environment_variables: [],
                        resources: [],
                        tasks: [],
                        tabs: [],
                        artifacts: [],
                        properties: nil
                    }
                ]
            }
        ]
    }
  end
end