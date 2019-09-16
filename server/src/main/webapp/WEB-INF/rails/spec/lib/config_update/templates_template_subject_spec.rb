#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe ConfigUpdate::TemplatesTemplateSubject do
  include ::ConfigUpdate::TemplatesTemplateSubject

  it "should return template from template collection" do
    allow(self).to receive(:template_name).and_return('template1')
    templates = TemplatesConfig.new()
    expectedTemplate = PipelineTemplateConfig.new(CaseInsensitiveString.new("template1"), [com.thoughtworks.go.helper.StageConfigMother.manual_stage("some_stage")].to_java(StageConfig))
    templates.add(expectedTemplate)
    templates.add(PipelineTemplateConfig.new(CaseInsensitiveString.new("template2"), [com.thoughtworks.go.helper.StageConfigMother.manual_stage("some_stage")].to_java(StageConfig)))

    actualTemplate = subject(templates)

    expect(actualTemplate).to eq(expectedTemplate)
  end
end
