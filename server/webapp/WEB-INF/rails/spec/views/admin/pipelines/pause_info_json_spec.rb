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

describe 'admin/pipelines/pause_info.json.erb' do

  it "should create json with 'pause_info_and_control' partial" do
    pause_info = double('some pause info')
    assign(:pause_info, pause_info)
    pipeline = double('pipeline')
    expect(pipeline).to receive(:name).and_return('mingle')
    assign(:pipeline, pipeline)

    expect(view).to receive(:render_json).with(:partial => "shared/pause_info_and_control.html", :locals => {:scope => {:pause_info => pause_info, :pipeline_name => 'mingle'}}).and_return("\"pause_fragment\"")

    render

    expect(response.body).to eq <<EOF.strip
{
    "pause_info_and_controls": {"html" : "pause_fragment"}
}
EOF
  end
end


