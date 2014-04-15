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
load File.join(File.dirname(__FILE__), 'layout_html_examples.rb')

describe "/layouts/pipeline_admin" do
before do
stub_server_health_messages
end
  before do
    @layout_name = "pipeline_admin"
    @admin_url = "/admin/pipelines"
    assigns[:user] = @user = Object.new
    assigns[:error_count] = 0
    assigns[:warning_count] = 0
    @user.stub(:anonymous?).and_return(true)

    template.stub!(:can_view_admin_page?).and_return(true)
    template.stub!(:is_user_an_admin?).and_return(true)
    class << template
      def url_for_with_stub *args
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end

    assigns[:pipeline] = PipelineConfigMother.pipelineConfig("mingle")
    assigns[:pause_info] = PipelinePauseInfo.paused("need to have fun", "loser")
  end

  it_should_behave_like "layout"

  it "should wire-up ajax_refresher for pause_info" do
    render :inline => 'body', :layout => @layout_name
    response.should have_tag('script', /AjaxRefreshers.addRefresher\(new AjaxRefresher\("\/admin\/pipelines\/mingle\/pause_info.json"\), true\)/)
  end

  it "should display pause-info section" do
    render :inline => 'body', :layout => @layout_name
    response.should have_tag("#pause_info_and_controls") do
      with_tag("form[action=?]", "/api/pipelines/mingle/unpause") do
        with_tag("button[title='Unpause'][value='Unpause']") do
          with_tag("span[title='Unpause']")
        end
      end
      with_tag(".pause_description.paused_by", "Paused by loser")
      with_tag(".pause_description.pause_message", "(need to have fun)")
    end
  end
end

