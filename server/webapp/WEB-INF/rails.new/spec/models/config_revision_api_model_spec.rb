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

describe ConfigRevisionAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @config_revision_view_model = create_config_revision_model
      config_revision_api_model = ConfigRevisionAPIModel.new(@config_revision_view_model)

      config_revision_api_model.md5.should == 'md5'
      config_revision_api_model.username.should == 'user name'
      config_revision_api_model.goVersion.should == 'version'
      config_revision_api_model.time.should == 12345678
      config_revision_api_model.schemaVersion.should == 'schema'
      config_revision_api_model.commitSHA.should == 'commit'
    end

    it "should should handle empty data" do
      @config_revision_view_model = create_empty_config_revision_model
      config_revision_api_model = ConfigRevisionAPIModel.new(@config_revision_view_model)

      config_revision_api_model.md5.should == nil
      config_revision_api_model.username.should == nil
      config_revision_api_model.goVersion.should == nil
      config_revision_api_model.goEdition.should == nil
      config_revision_api_model.time.should == nil
      config_revision_api_model.schemaVersion.should == nil
      config_revision_api_model.commitSHA.should == nil
    end
  end
end
