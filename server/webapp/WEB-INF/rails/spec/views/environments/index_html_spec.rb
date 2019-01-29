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

require 'rails_helper'

describe 'environments/index.html.erb' do

  before do
    @foo = EnvironmentViewModel.new(BasicEnvironmentConfig.new(CaseInsensitiveString.new('foo')))
  end

  it "should render partial 'show_env' for each environment" do
    assign(:environments, [@foo])
    assign(:show_add_environments, true)

    stub_template "_show_env.html.erb" => "Content for: <%= scope[:environment_view_model].getEnvironmentConfig().name().to_s %>"

    render

    expect(response).to have_selector("div.environments div#environment_foo_panel", :text => "Content for: foo")
  end

  it "should display 'no environments configured' message with link to configuration when there are no environments and using enterprise license" do
    assign(:environments, [])

    render

    expect(response).to have_selector("div.unused_feature a[href='#{docs_url '/configuration/managing_environments.html'}']", :text => "More Information")
  end
end
