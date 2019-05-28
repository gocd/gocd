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

describe Api::MaterialsController do
  include APIModelMother

  describe "notify" do
    before :each do
      @material_update_service = double('Material Update Service')
      @user = Username.new(CaseInsensitiveString.new('loser'))
      allow(controller).to receive(:current_user).and_return(@user)
      allow(controller).to receive(:material_update_service).and_return(@material_update_service)
      @params = {:post_commit_hook_material_type => 'svn', :no_layout => true, :payload => {}}
    end

    it "should return 403 when user is not an admin" do
      expect(@material_update_service).to receive(:notifyMaterialsForUpdate).with(@user, an_instance_of(HashWithIndifferentAccess), an_instance_of(HttpLocalizedOperationResult)) do |user, params, result|
        result.forbidden("Unauthorized to access this API.", HealthStateType.forbidden())
      end
      post :notify, params: @params
      expect(response.status).to eq(403)
      expect(response.body).to eq("Unauthorized to access this API.\n")
    end

    it "should return 200 when notify is successful" do
      expect(@material_update_service).to receive(:notifyMaterialsForUpdate).with(@user, an_instance_of(HashWithIndifferentAccess), an_instance_of(HttpLocalizedOperationResult)).and_return(nil)
      post :notify, params: @params
      expect(response.status).to eq(200)
    end

    it "should return 400 with params is empty" do
      expect(@material_update_service).to receive(:notifyMaterialsForUpdate).with(@user, an_instance_of(HashWithIndifferentAccess), an_instance_of(HttpLocalizedOperationResult)) do |user, params, result|
        result.badRequest("The request could not be understood by Go Server due to malformed syntax. The client SHOULD NOT repeat the request without modifications.")
      end
      post :notify, params: @params
      expect(response.status).to eq(400)
      expect(response.body).to eq("The request could not be understood by Go Server due to malformed syntax. The client SHOULD NOT repeat the request without modifications.\n")
    end

    it "should generate the route" do
      expect(material_notify_path(:post_commit_hook_material_type => 'svn')).to eq("/api/material/notify/svn")
    end

    it "should resolve" do
      expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      expect(:post => "/api/material/notify/svn").to route_to(:controller => "api/materials", :action => "notify", :no_layout=>true, :post_commit_hook_material_type => "svn")
    end
  end

  describe "list_materials_config" do
    before :each do
      allow(controller).to receive(:material_config_service).and_return(@material_config_service = double('material_config_service'))
    end

    it "should resolve" do
      expect(:get => "/api/config/materials").to route_to(:controller => "api/materials", :action => "list_configs", :no_layout=>true)
    end

    it "should render material list json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@material_config_service).to receive(:getMaterialConfigs).with("loser").and_return([create_material_config_model])

      get :list_configs, params:{:no_layout => true}

      expect(response.body).to eq([MaterialConfigAPIModel.new(create_material_config_model)].to_json)
    end
  end

  describe "list_material_modifications" do
    before :each do
      allow(controller).to receive(:material_config_service).and_return(@material_config_service = double('material_config_service'))
      allow(controller).to receive(:material_service).and_return(@material_service = double('material_service'))
    end

    it "should resolve" do
      expect(:get => "/api/materials/fingerprint/modifications").to route_to(:controller => "api/materials", :action => "modifications", :fingerprint => "fingerprint", :offset => "0", :no_layout => true)
      expect(:get => "/api/materials/fingerprint/modifications/1").to route_to(:controller => "api/materials", :action => "modifications", :fingerprint => "fingerprint", :offset => "1", :no_layout => true)
    end

    it "should render material modification list json" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      material_config = create_material_view_model
      expect(@material_config_service).to receive(:getMaterialConfig).with("loser", "fingerprint", anything).and_return(material_config)
      expect(@material_service).to receive(:getTotalModificationsFor).with(material_config).and_return(10)
      expect(@material_service).to receive(:getModificationsFor).with(material_config, anything).and_return([create_modification_view_model])

      get :modifications, params:{:fingerprint => "fingerprint", :offset => "5", :no_layout => true}

      expect(response.body).to eq(MaterialHistoryAPIModel.new(Pagination.pageStartingAt(5, 10, 10), [create_modification_view_model]).to_json)
    end

    it "should render error correctly" do
      loser = Username.new(CaseInsensitiveString.new("loser"))
      expect(controller).to receive(:current_user).and_return(loser)
      expect(@material_config_service).to receive(:getMaterialConfig).with("loser", "fingerprint", anything) do |username, fingerprint, result|
        result.notAcceptable("Not Acceptable", HealthStateType.general(HealthStateScope::GLOBAL))
      end

      get :modifications, params:{:fingerprint => "fingerprint", :no_layout => true}

      expect(response.status).to eq(406)
      expect(response.body).to eq("Not Acceptable\n")
    end
  end
end
