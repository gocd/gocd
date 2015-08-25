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

load File.expand_path(File.dirname(__FILE__) + '/../auto_refresh_examples.rb')

describe 'environments/index.html.erb' do
  before do
    @foo = Environment.new("foo", [])
    @bar = Environment.new("bar", [])
  end

  it "should render partial 'environment' for each environment" do
    assign(:environments, [@foo, @bar])
    assign(:show_add_environments, true)
    allow(view).to receive(:environments_allowed?).and_return(true)

    stub_template "_environment.html.erb" => "Content for: <%= scope[:environment].name %>"

    render

    expect(response).to have_selector("div.environments div#environment_foo_panel", :text => "Content for: foo")
    expect(response).to have_selector("div.environments div#environment_bar_panel", :text => "Content for: bar")
  end

  it "should display 'no environments configured' message with link to configuration when there are no environments and using enterprise license" do
    assign(:environments, [])
    allow(view).to receive(:environments_allowed?).and_return(true)

    render

    expect(response).to have_selector("div.unused_feature a[href='http://www.go.cd/documentation/user/current/configuration/managing_environments.html']", :text => "More Information")
  end

  describe :auto_refresh do
    before do
      @partial = 'environments/index.html.erb'
      @ajax_refresher = /DashboardAjaxRefresher/
      assign(:environments, [@foo, @bar])
      assign(:show_add_environments, true)
      allow(view).to receive(:environments_allowed?).and_return(true)

      stub_template "_environment.html.erb" => "Content for: <%= scope[:environment].name %>"
    end

    it_should_behave_like :auto_refresh
  end
end
