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


describe SortableTableHelper do
  describe :table_sort_params do
    it "should generate sort link params to sort ASC the first time" do
      expect(table_sort_params('hostname')).to eq({ :column => 'hostname', :order => 'ASC', :filter => nil})
      expect(table_sort_params('ip_address')).to eq({ :column => 'ip_address', :order => 'ASC', :filter => nil})
    end

    it "should generate sort link params to switch to DESC when accending" do
      params[:column] = 'hostname'
      params[:order] = 'ASC'
      expect(table_sort_params('hostname')).to eq({ :column => 'hostname', :order => 'DESC', :filter => nil})
    end

    it "should generate sort link params to switch to ASC when decending" do
      params[:column] = 'hostname'
      params[:order] = 'DESC'
      expect(table_sort_params('hostname')).to eq({ :column => 'hostname', :order => 'ASC', :filter => nil})
    end
  end

  describe :column_header do
    it "should generate column_header as link when sortable" do
      expect(self).to receive(:link_to).with("<span>ColumnName</span>", {:column => "SomeResource", :order => "ASC", :filter => nil}, {}).and_return("OUTPUT")
      expect(column_header("ColumnName", "SomeResource")).to eq("OUTPUT")
    end

    it "should generate column_header as span-covered text when not sortable" do
      expect(column_header("ColumnName", "SomeResource", false)).to eq("<span>ColumnName</span>")
    end
  end

  describe :sortable_column_status do
    it "should return no options for unsorted column" do
      expect(sortable_column_status('hostname')).to eq({ })
    end

    it "should add css class 'sorted_asc' for column sorted asc" do
      params[:column] = 'hostname'
      params[:order] = 'ASC'
      expect(sortable_column_status('hostname')).to eq({ :class => 'sorted_asc' })
    end

    it "should add css class 'sorted_desc' for column sorted desc" do
      params[:column] = 'hostname'
      params[:order] = 'DESC'
      expect(sortable_column_status('hostname')).to eq({ :class => 'sorted_desc' })
    end

    it "should return no options for unsorted column even with another one being sorted" do
      params[:column] = 'hostname'
      params[:order] = 'DESC'
      expect(sortable_column_status('location')).to eq({ })
    end
  end
end
