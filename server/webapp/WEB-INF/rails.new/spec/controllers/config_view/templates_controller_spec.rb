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

describe ConfigView::TemplatesController do

  describe 'security' do

    it 'should allow anyone, with security disabled' do
      disable_security

      expect(controller).to allow_action(:get, :show, params: { name: 'template' })
    end

    it 'should disallow anonymous users, with security enabled' do
      enable_security
      login_as_anonymous

      expect(controller).to disallow_action(:get, :show, params: { name: 'template' })
    end

    it 'should disallow normal users, with security enabled' do
      enable_security
      login_as_user

      expect(controller).to disallow_action(:get, :show, params: { name: 'template' })
    end

    it 'should allow admin, with security enabled' do
      enable_security
      login_as_admin

      expect(controller).to allow_action(:get, :show)
    end

    it 'should allow template admin, with security enabled' do
      login_as_template_admin

      expect(controller).to allow_action(:get, :show)
    end

    it 'should allow template view users, with security enabled' do
      enable_security
      allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(anything, anything).and_return(true)

      expect(controller).to allow_action(:get, :show)
    end
  end

  describe "show" do
    before :each do
      login_as_admin
      @template_config_service = double('template config service')
      allow(controller).to receive(:template_config_service).and_return(@template_config_service)
    end

    it "should return a template object of the given name" do
      template_config = 'template config'
      expect(@template_config_service).to receive(:loadForView).with('template.name', an_instance_of(HttpLocalizedOperationResult)).and_return(template_config)
      get :show, params: { :name => 'template.name' }
      expect(assigns[:template_config]).to eq(template_config)
    end

    it "should return nil for template config when template name does not exist" do
      expect(@template_config_service).to receive(:loadForView).with('template.name', an_instance_of(HttpLocalizedOperationResult)).and_return(nil)
      get :show, params: { :name => 'template.name' }
      expect(assigns[:template_config]).to eq(nil)
    end

    it "should render error when template config service returns bad operation result" do
      result = double(HttpLocalizedOperationResult)
      expect(result).to receive(:isSuccessful).and_return(false)
      expect(result).to receive(:httpCode).and_return(404)
      expect(result).to receive(:message).with(anything).and_return("Template Not found")
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
      template_name = 'template.name'
      expect(@template_config_service).to receive(:loadForView).with(template_name, result).and_return(nil)
      get :show, params: { :name => template_name }
      expect(assigns[:template_config]).to eq(nil)
      expect(response).to render_template("shared/config_error")
      expect(response.status).to eq(404)
    end
  end
end
