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

describe ConfigRevisionAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @config_revision_view_model = create_config_revision_model
      config_revision_api_model = ConfigRevisionAPIModel.new(@config_revision_view_model)

      expect(config_revision_api_model.md5).to eq('md5')
      expect(config_revision_api_model.username).to eq('user name')
      expect(config_revision_api_model.goVersion).to eq('version')
      expect(config_revision_api_model.time).to eq(12345678)
      expect(config_revision_api_model.schemaVersion).to eq('schema')
      expect(config_revision_api_model.commitSHA).to eq('commit')
    end

    it "should should handle empty data" do
      @config_revision_view_model = create_empty_config_revision_model
      config_revision_api_model = ConfigRevisionAPIModel.new(@config_revision_view_model)

      expect(config_revision_api_model.md5).to eq(nil)
      expect(config_revision_api_model.username).to eq(nil)
      expect(config_revision_api_model.goVersion).to eq(nil)
      expect(config_revision_api_model.goEdition).to eq(nil)
      expect(config_revision_api_model.time).to eq(nil)
      expect(config_revision_api_model.schemaVersion).to eq(nil)
      expect(config_revision_api_model.commitSHA).to eq(nil)
    end
  end
end
