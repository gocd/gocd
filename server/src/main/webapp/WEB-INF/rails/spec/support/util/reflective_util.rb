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

module ReflectiveUtil
  def set(obj, field, value)
    ReflectionUtil.setField(obj, field, value)
  end

#  def get(obj, field)
#    my_caller = caller[0]
#    File.open('/tmp/get_caller_log', 'a+') do |h|
#      h.write(my_caller + "\n")
#    end
#    get_val obj, field
#  end

  def get_val obj, field
    ReflectionUtil.getField(obj, field)
  end
end