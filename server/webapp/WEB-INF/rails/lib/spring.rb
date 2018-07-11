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

class Spring

  MUTEX = Mutex.new

  def self.bean(bean_name)
    context.get_bean(bean_name)
  rescue => e
    puts "Error loading bean #{bean_name} : #{e}"
    beans = context.bean_definition_names.collect { |bean| bean }
    puts "Defined beans are: #{beans.sort.join(', ')}"
    raise e
  end

  def self.context
    unless @context
      MUTEX.synchronize do
        @context ||= load_context
      end
    end
    @context
  end

  def self.load_context
    org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext($servlet_context)
  end
end
