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

describe ApiV1::Dashboard::PipelineGroupsRepresenter do

  it 'renders pipeline dashboard  with hal representation' do
    model       = PipelineGroupModel.new('bla')
    presenter   = ApiV1::Dashboard::PipelineGroupsRepresenter.new([model])
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:self, :doc)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/dashboard')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#dashboard')
    actual_json.fetch(:_embedded)[:pipeline_groups].should == [expected_embedded_pipeline_groups(model)]
  end

  private

  def expected_embedded_pipeline_groups(model)
    ApiV1::Dashboard::PipelineGroupRepresenter.new(model).to_hash(url_builder: UrlBuilder.new)
  end

end
