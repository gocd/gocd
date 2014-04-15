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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "layouts/agents" do
before do
stub_server_health_messages
end
  include AgentMother
  include GoUtil

  before do
    @layout_name = 'agent_detail'
    assigns[:user] = @user = Object.new
    @user.stub(:anonymous?).and_return(true)
    template.stub!(:can_view_admin_page?).and_return(true)
    template.stub!(:is_user_an_admin?).and_return(true)

    class << template
      def url_for_with_stub *args
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end

    stub_context_path(template)
  end

  describe :header do

    it "should set the page title" do
      assigns[:agent] = idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1")

      render :inline => '<div>content</div>', :layout => @layout_name

      assigns[:view_title].should == "Agents"
    end

    it "should show agent name" do
      assigns[:agent] = idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1")

      render :inline => '<div>content</div>', :layout => @layout_name

      response.body.should have_tag(".page_header ul.entity_title li h1", "Agent01")
    end
  end
end
