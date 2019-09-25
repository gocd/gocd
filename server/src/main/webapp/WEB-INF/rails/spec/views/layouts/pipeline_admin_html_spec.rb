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

describe "/layouts/pipeline_admin" do
  it_should_behave_like :layout

  before do
    @layout_name = "layouts/pipeline_admin"
    @admin_url = "/admin/pipelines"
    @user = Username::ANONYMOUS
    assign(:user, @user)
    assign(:error_count,0)
    assign(:warning_count,0)

    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)

    assign(:pipeline, PipelineConfigMother.pipelineConfig("mingle"))
    assign(:pause_info, PipelinePauseInfo.paused("need to have fun", "loser"))
  end

  it "should wire-up ajax_refresher for pause_info" do
    render :inline => 'body', :layout => @layout_name
    expect(response.body).to have_selector("script",:text=>/AjaxRefreshers.addRefresher\(new AjaxRefresher\("\/admin\/pipelines\/mingle\/pause_info.json"\), true\)/, :visible => false)
  end

  it "should display pause-info section" do
    render :inline => 'body', :layout => @layout_name
    Capybara.string(response.body).find('#pause_info_and_controls').tap do |block|
      block.find("form[action='/go/api/pipelines/mingle/unpause']").tap do |form|
        form.find("button[title='Unpause'][value='Unpause']").tap do |button|
          expect(button).to have_selector("span[title='Unpause']")
        end
      end
      expect(block).to have_selector(".pause_description.paused_by", :text=>"Paused by loser")
      expect(block).to have_selector(".pause_description.pause_message", :text=>"(need to have fun)")
    end
  end
end

