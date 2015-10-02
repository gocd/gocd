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

describe "/layouts/application" do
  include EngineUrlHelper

  before do
    stub_server_health_messages
  end

  before do
    @layout_name = "layouts/application"
    @admin_url = "/admin/pipelines"
    assign(:user, @user = Object.new)
    assign(:error_count, 0)
    assign(:warning_count, 0)
    @user.stub(:anonymous?).and_return(true)
    view.stub(:can_view_admin_page?).and_return(true)
    view.stub(:is_user_an_admin?).and_return(true)
    class << view
      def url_for_with_stub *args
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end

    main_app = double('main_app')
    stub_routes_for_main_app main_app
    allow(view).to receive(:main_app).and_return(main_app)
  end

  it_should_behave_like :layout

  it "should show content" do
    render :inline => '<div>content</div>', :layout => @layout_name

    expect(response).to have_selector('html body div div', 'content')
  end

  it "should display Add New environment link" do
    assign(:show_add_environments, true)
    assign(:page_header, "Environments")

    render :inline => '<div>content</div>', :layout => @layout_name

    expect(response).to have_selector(".add_new_environment a.link_as_header_button", :text => "Add a new environment")
  end

  it "should not display Add New environment link when there is not show_add_environments" do
    assign(:page_header, "Environments")

    render :inline => '<div>content</div>', :layout => @layout_name

    expect(response).to_not have_selector(".add_new_environment a.link_as_button")
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
