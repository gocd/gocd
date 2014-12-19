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

module ComparisonHelper

  def pipeline_compare_href pipeline_name, from_counter, to_counter
    if (from_counter > to_counter)
      return pipeline_compare_href pipeline_name, to_counter, from_counter
    end
    compare_pipelines_path(:pipeline_name => pipeline_name, :from_counter => from_counter, :to_counter => to_counter)
  end

  def any_match?(pattern, *values)
    regex = /#{pattern}/i
    values.compact.any? { |s| s =~ regex }
  end

  def compare_pipeline_pagination_handler page, suffix
    dom_id = "pim_pages_#{page.getLabel()}"
    url = compare_pipelines_timeline_path(:page => page.getNumber(),:other_pipeline_counter => params[:other_pipeline_counter], :suffix => suffix)
    <<END
    <a href="#" id="#{dom_id}">#{page.getLabel()}</a>
    <script type="text/javascript">
        Util.click_load({target: '##{dom_id}', url: '#{url}', update: '#modal_timeline_container', spinnerContainer: 'pagination_bar'});
    </script>
END
  end

  def show_bisect?
    params[:show_bisect] == true.to_s
  end
end