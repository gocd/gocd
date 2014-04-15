# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr
  module JRebel
    def jrebel_home
      unless @jrebel_home
        @jrebel_home = ENV['REBEL_HOME'] || ENV['JREBEL'] || ENV['JREBEL_HOME']
      end

      (@jrebel_home && File.exists?(@jrebel_home)) ? @jrebel_home : nil
    end

    def rebel_jar
      if jrebel_home
        # jrebel_home may point to jrebel.jar directly
        File.directory?(jrebel_home) ? File.join(jrebel_home, 'jrebel.jar') : jrebel_home
      end
    end

    def jrebel_args
      rebel_jar ? [ '-noverify', "-javaagent:#{rebel_jar}" ] : []
    end

    def jrebel_props(project)
      {}
    end
  end
end

