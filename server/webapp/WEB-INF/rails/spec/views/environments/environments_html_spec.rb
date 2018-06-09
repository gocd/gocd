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

describe 'environments/_environments.html.erb' do
  describe "with environments" do
    before do
      allow(view).to receive(:is_user_an_admin?).and_return(true)

      @uat = 'uat'
      @prod = 'prod'
      assign(:environments, [@uat, @prod])

      view.extend StagesHelper
      view.extend EnvironmentsHelper
    end

    it "should render environments using environment partial" do
      stub_template "_environment.html.erb" => "Content for: <%= scope[:environment] %>"

      render :partial => 'environments/environments.html.erb', :locals => {:scope => {:show_edit_environments => true}}

      expect(response).to have_selector("div#environment_uat_panel", :text => "Content for: uat")
      expect(response).to have_selector("div#environment_prod_panel", :text => "Content for: prod")
    end
  end
end


