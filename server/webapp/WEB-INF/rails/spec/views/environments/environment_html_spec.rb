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

describe "/environments/_environment.html.erb" do
  include PipelineModelMother

  before do
    allow(view).to receive(:is_user_an_admin?).and_return(true)

    @environment = 'UAT'
    view.extend StagesHelper
    view.extend EnvironmentsHelper
  end

  def render_show
    render :partial => 'environments/environment.html.erb', locals: {scope: {environment: @environment, show_edit_environments: true}}
  end

  describe "environment title" do

      it "should display the environment name as link if show_add_environment is true" do
        render :partial => 'environments/environment.html.erb', locals: {scope: {environment: @environment, show_edit_environments: true}}
        expect(response).to have_selector("div.environment h2.entity_title a[href='/environments/UAT/show']")
        expect(response).to have_selector("div.environment h2.entity_title", :text => /UAT/)
      end

      it "should display the environment name as plain text if show_add_environment is false" do
        render :partial => 'environments/environment.html.erb', locals: {scope: {environment: @environment, show_edit_environments: false}}
        expect(response).to have_selector("div.environment h2.entity_title", :text => /UAT/)
        expect(response).to_not have_selector("div.environment h2.entity_title a[href='/environments/UAT/show']")
      end

  end

end
