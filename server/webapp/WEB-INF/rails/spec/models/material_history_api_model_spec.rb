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

describe MaterialHistoryAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pagination_view_model = create_pagination_model
      @modification_view_model = create_modification_view_model
      material_history_api_model = MaterialHistoryAPIModel.new(@pagination_view_model, [@modification_view_model])

      expect(material_history_api_model.pagination.page_size).to eq(10)
      expect(material_history_api_model.pagination.offset).to eq(1)
      expect(material_history_api_model.pagination.total).to eq(100)

      modification_api_model = material_history_api_model.modifications[0]
      expect(modification_api_model.id).to eq(321)
      expect(modification_api_model.revision).to eq('revision')
      expect(modification_api_model.modified_time).to eq(12345678)
      expect(modification_api_model.user_name).to eq('user name')
      expect(modification_api_model.comment).to eq('comment')
      expect(modification_api_model.email_address).to eq('test@test.com')
    end

    it "should handle empty data" do
      @pagination_view_model = create_empty_pagination_model
      @modification_view_model = create_empty_modification_view_model
      material_history_api_model = MaterialHistoryAPIModel.new(@pagination_view_model, [@modification_view_model])

      expect(material_history_api_model.pagination.page_size).to eq(nil)
      expect(material_history_api_model.pagination.offset).to eq(nil)
      expect(material_history_api_model.pagination.total).to eq(nil)

      modification_api_model = material_history_api_model.modifications[0]
      expect(modification_api_model.id).to eq(nil)
      expect(modification_api_model.revision).to eq(nil)
      expect(modification_api_model.modified_time).to eq(nil)
      expect(modification_api_model.user_name).to eq(nil)
      expect(modification_api_model.comment).to eq(nil)
      expect(modification_api_model.email_address).to eq(nil)
    end
  end
end
