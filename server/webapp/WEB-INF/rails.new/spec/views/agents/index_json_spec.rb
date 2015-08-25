##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

require 'spec_helper'


describe "/agents/index.json.erb" do
  before do
    view.stub(:can_view_admin_page?).and_return(true)
    view.stub(:has_operate_permission_for_agents?).and_return(true)
  end

  it "should return json of partials" do
    assign(:agents, :agents_collection)
    allow(view).to receive(:render_json).with(:partial=>'agents_header.html.erb', :locals => {:scope => {}}).and_return("\"header\"")
    allow(view).to receive(:render_json).with(:partial=>'agents_table.html.erb', :locals => {:scope => {}}).and_return("\"table\"")
    allow(view).to receive(:render_json).with(:partial=>'agents/hidden_selectors.html', :locals => {:scope => {:agents => :agents_collection}}).and_return("\"hidden_checkboxes\"")

    render
    json = JSON.parse(response.body)

    expect(json["ajax_agents_header"]["html"]).to eq "header"
    expect(json["ajax_agents_table"]["html"]).to eq "table"
    expect(json["actual_agent_selectors"]["html"]).to eq "hidden_checkboxes"
  end
end
