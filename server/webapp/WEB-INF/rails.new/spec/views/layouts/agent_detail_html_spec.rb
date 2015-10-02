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
load File.join(File.dirname(__FILE__), 'layout_html_examples.rb')

describe "/layouts/agent_detail" do
before do
stub_server_health_messages
end
  include AgentMother
  include GoUtil

  before do
    @layout_name = 'layouts/agent_detail'
    assign(:user, @user = Object.new)
    @user.stub(:anonymous?).and_return(true)

    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    class << view
      def url_for_with_stub *args
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end

    stub_context_path(view)
  end

  describe :header do

    it "should set the page title" do
      assign(:agent, idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1"))

      render :inline => '<div>content</div>', :layout => @layout_name

      page = Capybara::Node::Simple.new(response.body)
      expect(page.title).to include("Agents")
    end

    it "should show agent name" do
      assign(:agent, idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1"))

      render :inline => '<div>content</div>', :layout => @layout_name

      expect(response.body).to have_selector(".page_header ul.entity_title li h1",:text=>"Agent01")
    end
  end
end
