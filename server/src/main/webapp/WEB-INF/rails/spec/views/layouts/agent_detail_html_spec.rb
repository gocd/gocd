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
require_relative 'layout_html_examples'

describe "/layouts/agent_detail" do
  include AgentMother
  include GoUtil

  it_should_behave_like :layout

  before do
    @layout_name = 'layouts/agent_detail'
    @user = Username::ANONYMOUS
    assign(:user, @user)

    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)

    stub_context_path(view)
    assign(:agent, idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1"))
  end

  describe "header" do

    it "should set the page title" do
      render :inline => '<div>content</div>', :layout => @layout_name

      page = Capybara::Node::Simple.new(response.body)
      expect(page.title).to include("Agents")
    end

    it "should show agent name" do
      render :inline => '<div>content</div>', :layout => @layout_name

      expect(response.body).to have_selector(".page_header ul.entity_title li h1",:text=>"Agent01")
    end
  end
end
