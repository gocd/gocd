#
# Copyright 2021 ThoughtWorks, Inc.
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

describe "/layouts/application" do
  it_should_behave_like :layout

  before do
    @layout_name = "layouts/application"
    @admin_url = "/admin/pipelines"
    @user = Username::ANONYMOUS
    assign(:user, @user)
    assign(:error_count, 0)
    assign(:warning_count, 0)
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
  end

  it "should show content" do
    render :inline => '<div>content</div>', :layout => @layout_name

    expect(response).to have_selector('html body div div', 'content')
  end

  it "should render reload option when the config file MD5 has changed under the message" do
    assign(:config_file_conflict, true)

    render :inline => '<div>content</div>', :layout => @layout_name

    expect(response).to have_selector("#messaging_wrapper #config_save_actions button.reload_config#reload_config", :text => "Reload")
    expect(response).to have_selector("#messaging_wrapper #config_save_actions label", :text => "This will refresh the page and you will lose your changes on this page.")
  end

  it "should not render reload option when the config file has not conflicted" do
    render :inline => '<div>content</div>', :layout => @layout_name

    expect(response).to_not have_selector("#messaging_wrapper #config_save_actions")
  end
end
