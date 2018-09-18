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

module FormUI
  def config_errors(*tuples)
    config_error = ConfigErrors.new()
    tuples.each do |field_name, message|
      config_error.add(field_name, message)
    end
    config_error
  end

  def config_error(key, message)
    config_errors([key, message])
  end

  def set(obj, field, value)
    ReflectionUtil.setField(obj, field, value)
  end
end