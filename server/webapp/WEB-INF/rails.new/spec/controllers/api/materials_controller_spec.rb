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

describe Api::MaterialsController do
  include APIModelMother

  describe :routes do
    it "should generate the route" do
      expect(material_notify_path(:post_commit_hook_material_type => 'svn')).to eq("/api/material/notify/svn")
    end

    it "should resolve" do
      expect(:post => "/api/material/notify/svn").to route_to(:controller => "api/materials", :action => "notify", :no_layout=>true, :post_commit_hook_material_type => "svn")
    end
  end

  describe :notify do
    before :each do
      @material_update_service = double('Material Update Service')
      @user = Username.new(CaseInsensitiveString.new('loser'))
      controller.stub(:current_user).and_return(@user)
      controller.stub(:material_update_service).and_return(@material_update_service)
      @params = {:post_commit_hook_material_type => 'svn', :no_layout => true, :payload => {}}
    end

    it "should return 401 when user is not an admin" do
      @material_update_service.should_receive(:notifyMaterialsForUpdate).with(@user, an_instance_of(ActionController::Parameters), an_instance_of(HttpLocalizedOperationResult)) do |user, params, result|
        result.unauthorized(LocalizedMessage.string('API_ACCESS_UNAUTHORIZED'), HealthStateType.unauthorised())
      end
      post :notify, @params
      expect(response.status).to eq(401)
      expect(response.body).to eq("Unauthorized to access this API.\n")
    end

    it "should return 200 when notify is successful" do
      @material_update_service.should_receive(:notifyMaterialsForUpdate).with(@user, an_instance_of(ActionController::Parameters), an_instance_of(HttpLocalizedOperationResult)).and_return(nil)
      post :notify, @params
      expect(response.status).to eq(200)
    end

    it "should return 400 with params is empty" do
      @material_update_service.should_receive(:notifyMaterialsForUpdate).with(@user, an_instance_of(ActionController::Parameters), an_instance_of(HttpLocalizedOperationResult)) do |user, params, result|
        result.badRequest(LocalizedMessage.string('API_BAD_REQUEST'))
      end
      post :notify, @params
      expect(response.status).to eq(400)
      expect(response.body).to eq("The request could not be understood by Go Server due to malformed syntax. The client SHOULD NOT repeat the request without modifications.\n")
    end
  end

  describe :list_materials_config do
    before :each do
      controller.stub(:material_config_service).and_return(@material_config_service = double('material_config_service'))
    end

    it "should resolve" do
      expect(:get => "/api/config/materials").to route_to(:controller => "api/materials", :action => "list_configs", :no_layout=>true)
    end

    it "should render material list json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @material_config_service.should_receive(:getMaterialConfigs).with("loser").and_return([create_material_config_model])

      get :list_configs, :no_layout => true

      expect(response.body).to eq([MaterialConfigAPIModel.new(create_material_config_model)].to_json)
    end
  end

  describe :list_material_modifications do
    before :each do
      controller.stub(:material_config_service).and_return(@material_config_service = double('material_config_service'))
      controller.stub(:material_service).and_return(@material_service = double('material_service'))
    end

    it "should resolve" do
      expect(:get => "/api/materials/fingerprint/modifications").to route_to(:controller => "api/materials", :action => "modifications", :fingerprint => "fingerprint", :offset => "0", :no_layout => true)
      expect(:get => "/api/materials/fingerprint/modifications/1").to route_to(:controller => "api/materials", :action => "modifications", :fingerprint => "fingerprint", :offset => "1", :no_layout => true)
    end

    it "should render material modification list json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      material_config = create_material_view_model
      @material_config_service.should_receive(:getMaterialConfig).with("loser", "fingerprint", anything).and_return(material_config)
      @material_service.should_receive(:getTotalModificationsFor).with(material_config).and_return(10)
      @material_service.should_receive(:getModificationsFor).with(material_config, anything).and_return([create_modification_view_model])

      get :modifications, :fingerprint => "fingerprint", :offset => "5", :no_layout => true

      expect(response.body).to eq(MaterialHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), [create_modification_view_model]).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      controller.should_receive(:current_user).and_return(loser)
      @material_config_service.should_receive(:getMaterialConfig).with("loser", "fingerprint", anything) do |username, fingerprint, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :modifications, :fingerprint => "fingerprint", :no_layout => true

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end
end
