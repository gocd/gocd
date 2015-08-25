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

describe ConfigView::TemplatesController do

  describe :routes do
    it "should resolve & generate route for viewing templates" do
      expect(:get => "/config_view/templates/template.name").to route_to(:controller => "config_view/templates", :action => "show", :name => "template.name")
      expect(config_view_templates_show_path(:name => "template.name")).to eq("/config_view/templates/template.name")
    end
  end

  describe :show do
    before :each do
      @template_config_service = double('template config service')
      controller.stub(:template_config_service).and_return(@template_config_service)
    end

    it "should return a template object of the given name" do
      template_config = 'template config'
      @template_config_service.should_receive(:loadForView).with('template.name', an_instance_of(HttpLocalizedOperationResult)).and_return(template_config)
      get :show, {:name => 'template.name'}
      expect(assigns[:template_config]).to eq(template_config)
    end

    it "should return nil for template config when template name does not exist" do
      @template_config_service.should_receive(:loadForView).with('template.name', an_instance_of(HttpLocalizedOperationResult)).and_return(nil)
      get :show, {:name => 'template.name'}
      expect(assigns[:template_config]).to eq(nil)
    end

    it "should render error when template config service returns bad operation result" do
      result = HttpLocalizedOperationResult.new()
      result.should_receive(:isSuccessful).and_return(false)
      result.should_receive(:httpCode).and_return(404)
      result.should_receive(:message).with(anything).and_return("Template Not found")
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      template_name = 'template.name'
      @template_config_service.should_receive(:loadForView).with(template_name, result).and_return(nil)
      get :show, {:name => template_name}
      expect(assigns[:template_config]).to eq(nil)
      expect(response).to render_template("shared/config_error")
      expect(response.status).to eq(404)
    end
  end
end
