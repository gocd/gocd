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

describe ApiV1::Dashboard::PipelineInstanceRepresenter do
  include PipelineModelMother

  it 'renders all pipeline instances with hal representation' do
    presenter = ApiV1::Dashboard::PipelineInstanceRepresenter.new(pipeline_instance_model_empty('p1', 's1'))

    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)
    expect(actual_json).to have_links(:self, :doc, :build_cause_url, :compare_url, :history_url, :vsm_url)
    expect(actual_json).to have_link(:self).with_url('http://test.host/api/pipelines/p1/instance/0')
    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#get-pipeline-instance')
    expect(actual_json).to have_link(:build_cause_url).with_url('http://test.host/pipelines/p1/0/build_cause')
    expect(actual_json).to have_link(:compare_url).with_url('http://test.host/compare/p1/-1/with/0')
    expect(actual_json).to have_link(:history_url).with_url('http://test.host/api/pipelines/p1/history')
    expect(actual_json).to have_link(:vsm_url).with_url('http://test.host/pipelines/value_stream_map/p1/0')

    actual_json.fetch(:_embedded)[:stages].collect { |s| s[:name] }.should == ['s1']
  end
end
