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

class TopLevelFormNameProvider
  include com.thoughtworks.go.plugins.presentation.FormNameProvider

  attr_reader :form_name_prefix

  def initialize(form_name_prefix)
    @form_name_prefix = form_name_prefix
  end

  def name(actual_name)
    "#{@form_name_prefix}[#{actual_name}]"
  end

  def collection(actual_name)
    TopLevelFormNameProvider.new(name(actual_name) + "[]")
  end

  def obj(actual_name)
    TopLevelFormNameProvider.new(name(actual_name))
  end

  def css_id_for(element_name)
    "#{@form_name_prefix}_#{element_name}".gsub(/[\[\]_]+/, '_').sub(/_$/, '')
  end
end