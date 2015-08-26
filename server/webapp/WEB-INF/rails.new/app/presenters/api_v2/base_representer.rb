##########################GO-LICENSE-START################################
# Copyright 2015 ThoughtWorks, Inc.
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

require 'roar/decorator'
require 'roar/json'
require 'roar/json/hal'

module ApiV2
  class BaseRepresenter < Roar::Decorator
    include Roar::JSON::HAL

    class <<self
      def property(name, options={})
        if (options[:skip_nil])
          super
        else
          super(name, options.merge!(render_nil: true))
        end
      end
    end

    def to_hash(*options)
      super.deep_symbolize_keys
    end

  end
end
