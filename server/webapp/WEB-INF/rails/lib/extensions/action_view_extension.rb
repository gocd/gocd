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

ActionView::Helpers::InstanceTag.class_eval do

  unless @go_enhancement_loaded
    def add_default_name_and_id_with_id_omission(options)
      add_default_name_and_id_without_id_omission(options)
      options.delete("omit_id_generation") && options.delete("id")
    end

    alias_method_chain :add_default_name_and_id, :id_omission

    def to_check_box_tag_with_optional_hidden_field(options = {}, *args)
      drop_hidden_field = options.delete(:drop_hidden_field)
      checkbox = to_check_box_tag_without_optional_hidden_field(options, *args)
      if drop_hidden_field
        checkbox.gsub!(/^<input[^>]+?\/>/, '')
      end
      checkbox
    end

    alias_method_chain :to_check_box_tag, :optional_hidden_field

    @go_enhancement_loaded = true
  end
end