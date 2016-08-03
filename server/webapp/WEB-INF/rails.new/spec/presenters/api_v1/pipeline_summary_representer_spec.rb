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

describe ApiV1::PipelineSummaryRepresenter do

  it 'renders an pipeline with hal representation' do
    pipeline_instance=OpenStruct.new({
                                  pipeline_name:    'pipeline',
                                  pipeline_counter: 1,
                                  pipeline_label:   'stage'
                                })
    presenter   = ApiV1::PipelineSummaryRepresenter.new(pipeline_instance)
    actual_json = presenter.to_hash(url_builder: UrlBuilder.new)

    expect(actual_json).to have_links(:doc)

    expect(actual_json).to have_link(:doc).with_url('https://api.go.cd/#pipelines')

    actual_json.delete(:_links)
    expect(actual_json).to eq(pipeline_hash(pipeline_instance))
  end

  def pipeline_hash(pipeline_instacne)
    {
      name:    pipeline_instacne.pipeline_name,
      counter: pipeline_instacne.pipeline_counter,
      label:   pipeline_instacne.pipeline_label
    }
  end

end
