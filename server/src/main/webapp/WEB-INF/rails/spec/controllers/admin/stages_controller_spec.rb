#
# Copyright 2020 ThoughtWorks, Inc.
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

describe Admin::StagesController do
  before do
    allow(controller).to receive(:go_config_service).with(no_args).and_return(@go_config_service = double('@go_config_service'))
    allow(@go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
  end

  describe "action" do
    describe "config_change" do
      describe "security" do
        it 'should allow anyone, with security disabled' do
          disable_security
          expect(controller).to allow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should disallow anonymous users, with security enabled' do
          enable_security
          login_as_anonymous
          expect(controller).to disallow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should disallow normal users, with security enabled' do
          login_as_user
          expect(controller).to disallow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should allow admin users, with security enabled' do
          login_as_admin
          expect(controller).to allow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end

        it 'should disallow pipeline group admin users, with security enabled' do
          login_as_group_admin
          expect(controller).to disallow_action(:get, :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        end
      end

      it "should route to action" do
        login_as_admin

        expect(:get => "admin/config_change/between/md5_value_2/and/md5_value_1").to route_to({:controller => "admin/stages", :action => "config_change", :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
        expect(controller.send(:admin_config_change_path, :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1")).to eq("/admin/config_change/between/md5_value_2/and/md5_value_1")
      end

      it "should generate the correct route" do
        login_as_admin

        expect(controller.send(:admin_config_change_path, :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1")).to eq("/admin/config_change/between/md5_value_2/and/md5_value_1")
      end

      it "should assign config changes for given md5" do
        login_as_admin

        result = HttpLocalizedOperationResult.new
        expect(@go_config_service).to receive(:configChangesFor).with("md5_value_2", "md5_value_1", result).and_return("changes_string")
        get :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"}
        expect(assigns(:changes)).to eq "changes_string"
      end

      it "should assign message if config changes is nil because given md5 is the first revision in repo" do
        login_as_admin

        result = HttpLocalizedOperationResult.new
        allow(@go_config_service).to receive(:configChangesFor).with("md5_value_2", "md5_value_1", result).and_return(nil)
        get :config_change, params: {:later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"}
        expect(assigns(:config_change_error_message)).to eq "This is the first entry in the config versioning. Please refer config tab to view complete configuration during this run."
      end
    end
  end
end
