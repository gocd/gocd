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

module SortableTableHelper
  def table_sort_params column
    options = { :column => column, :order => 'ASC', :filter => params[:filter]}
    options.merge!(:order => 'DESC') if (params[:column] == column) && (params[:order] == 'ASC')
    options
  end

  def sortable_column_status column
    return { } unless column == params[:column]
    {:class => "sorted_#{params[:order].downcase}"}
  end

  def column_header(name, param_name, sortable = true)
    sortable ? link_to(surround_with_span(name), table_sort_params(param_name), sortable_column_status(param_name)) : surround_with_span(name)
  end

  private
  def surround_with_span span_text
    "<span>#{span_text}</span>".html_safe
  end
end