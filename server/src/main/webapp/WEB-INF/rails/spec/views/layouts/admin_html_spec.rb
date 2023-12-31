#
# Copyright 2024 Thoughtworks, Inc.
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

describe "layouts/admin" do
  it_should_behave_like :layout

  before do
    @layout_name = 'layouts/admin'
    @admin_url = "/admin/pipelines"
    @user = Username::ANONYMOUS
    assign(:user, @user)
    assign(:error_count, 0)
    assign(:warning_count, 0)
    assign(:tab_name, "dummy-tab")
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_a_group_admin?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:is_user_authorized_to_view_templates?).and_return(false)
    view.extend(SparkUrlAware)
  end

  it "should display warning message when config not valid" do
    assign(:config_valid, false)
    render :inline => '<div>content</div>', :layout => @layout_name
    expect(response.body).to have_selector(".flash p.warning", :text => "Invalid config on disk. Displaying the last known valid config (Editing config through Go will overwrite the invalid copy. Edit it on disk to fix this problem).")
  end

  it "should show content for tab" do
    render :inline => '<div>content</div>', :layout => @layout_name
    expect(response.body).to have_selector('#tab-content-of-dummy-tab')
    end
end
