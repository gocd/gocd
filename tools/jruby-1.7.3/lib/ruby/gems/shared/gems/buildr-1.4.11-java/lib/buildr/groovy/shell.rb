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
  module Groovy
    class GroovySH < Buildr::Shell::Base
      include JRebel

      SUFFIX = if Util.win_os? then '.bat' else '' end

      specify :name => :groovy, :languages => [:groovy]

      def launch(task)
        cp = Groovy.dependencies +
             project.compile.dependencies +
             [ project.path_to(:target, :classes) ] +
             task.classpath
        props = jrebel_props(project).merge(task.properties)
        java_args = jrebel_args + task.java_args

        groovy_home = nil
        if groovy_home
          cmd_args = " -classpath '#{cp.join(File::SEPARATOR)}'"
          trace "groovysh #{cmd_args}"
          system(File.expand_path("bin#{File::SEPARATOR}groovysh#{SUFFIX}", groovy_home) + cmd_args)
        else
          Java::Commands.java 'org.codehaus.groovy.tools.shell.Main', {
            :properties => props,
            :classpath => cp,
            :java_args => java_args
          }
        end
      end

    private
      def groovy_home
        @home ||= ENV['GROOVY_HOME']
      end
    end
  end
end

Buildr::Shell.providers << Buildr::Groovy::GroovySH
