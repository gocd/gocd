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

describe MaterialHistoryAPIModel do
  include APIModelMother

  describe "should initialize correctly" do
    it "should populate correct data" do
      @pagination_view_model = create_pagination_model
      @modification_view_model = create_modification_view_model
      material_history_api_model = MaterialHistoryAPIModel.new(@pagination_view_model, [@modification_view_model])

      material_history_api_model.pagination.page_size.should == 10
      material_history_api_model.pagination.offset.should == 1
      material_history_api_model.pagination.total.should == 100

      modification_api_model = material_history_api_model.modifications[0]
      modification_api_model.id.should == 321
      modification_api_model.revision.should == 'revision'
      modification_api_model.modified_time.should == 12345678
      modification_api_model.user_name.should == 'user name'
      modification_api_model.comment.should == 'comment'
      modification_api_model.email_address.should == 'test@test.com'
    end

    it "should handle empty data" do
      @pagination_view_model = create_empty_pagination_model
      @modification_view_model = create_empty_modification_view_model
      material_history_api_model = MaterialHistoryAPIModel.new(@pagination_view_model, [@modification_view_model])

      material_history_api_model.pagination.page_size.should == nil
      material_history_api_model.pagination.offset.should == nil
      material_history_api_model.pagination.total.should == nil

      modification_api_model = material_history_api_model.modifications[0]
      modification_api_model.id.should == nil
      modification_api_model.revision.should == nil
      modification_api_model.modified_time.should == nil
      modification_api_model.user_name.should == nil
      modification_api_model.comment.should == nil
      modification_api_model.email_address.should == nil
    end
  end
end
