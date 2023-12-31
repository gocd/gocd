#
# Copyright 2024 Thoughtworks, Inc.
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

module GoUtil
  def in_params map
    map.each do |key, value|
      if controller.respond_to?(:extra_params)  # for view specs
        controller.extra_params = controller.extra_params.merge(key => value)
      end
      controller.params[key] = value
      @request.path_parameters[key] = value
    end
  end
end
