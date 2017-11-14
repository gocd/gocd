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

module ParamEncoder
  module ClassMethods
    def decode_params *param_names
      options = param_names.extract_options!
      prepend_before_action(options) do |controller|
        params = controller.params
        param_names.each do |param_name|
          params[param_name] = controller.dec(params[param_name])
        end
      end
    end
  end

  def self.included(base)
    base.extend(ClassMethods)
  end

  def enc str
    CGI.escape(Base64.encode64(str))
  end

  def dec str
    Base64.decode64(CGI.unescape(str))
  end
end