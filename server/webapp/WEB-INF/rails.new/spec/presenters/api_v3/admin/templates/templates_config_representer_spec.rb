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

describe ApiV3::Admin::Templates::TemplatesConfigRepresenter do

  it 'should render links' do
    templates = TemplateToPipelines.new(CaseInsensitiveString.new("template-name"), true, true)
    templates.add(PipelineWithAuthorization.new(CaseInsensitiveString.new("pipeline1"), true))
    templates.add(PipelineWithAuthorization.new(CaseInsensitiveString.new("pipeline2"), false))

    actual_json = ApiV3::Admin::Templates::TemplatesConfigRepresenter.new([templates]).to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:self, :doc, :find)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/templates')
    expect(actual_json).to have_link(:doc).with_url('https://api.gocd.org/#template-config')
    expect(actual_json).to have_link(:find).with_url('http://test.host/api/admin/templates/:template_name')
    actual_json.delete(:_links)

    actual_json.fetch(:_embedded).should == {templates: [ApiV3::Admin::Templates::TemplateSummaryRepresenter.new(templates).to_hash(url_builder: UrlBuilder.new)]}
  end
end
