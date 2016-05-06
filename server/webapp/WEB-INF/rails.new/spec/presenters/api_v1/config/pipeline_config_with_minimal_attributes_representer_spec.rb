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

describe ApiV1::Config::PipelineConfigWithMinimalAttributesRepresenter do
  describe :serialize do
    it 'should render pipeline with hal representation' do
      pipeline_config = PipelineConfigMother.createPipelineConfigWithStages('regression', 'fetch', 'run')

      actual_json = ApiV1::Config::PipelineConfigWithMinimalAttributesRepresenter.new(pipeline_config).to_hash(url_builder: UrlBuilder.new)

      expect(actual_json).to have_link(:self).with_url('http://test.host/api/admin/pipelines/regression')
      actual_json.delete(:_links)
      expect(actual_json).to eq({name: 'regression', stages: [{:name=>'fetch', :jobs=>['dev']}, {:name=>'run', :jobs=>['dev']}]})
    end
  end
end