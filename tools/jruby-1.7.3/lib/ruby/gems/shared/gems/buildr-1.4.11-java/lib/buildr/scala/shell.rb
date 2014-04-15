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
  module Scala
    class ScalaShell < Buildr::Shell::Base
      include Buildr::JRebel

      specify :name => :scala, :languages => [:scala]

      def launch(task)
        jline = [File.expand_path("lib/jline.jar", Scalac.scala_home)].find_all { |f| File.exist? f }
        jline = ['jline:jline:jar:0.9.94'] if jline.empty?

        cp = project.compile.dependencies +
             Scalac.dependencies +
             project.test.dependencies +
             task.classpath

        java_args = jrebel_args + task.java_args

        props = jrebel_props(project).merge(task.properties)

        Java::Commands.java 'scala.tools.nsc.MainGenericRunner',
                            '-cp', cp.join(File::PATH_SEPARATOR),
        {
          :properties => props,
          :classpath => cp + jline,
          :java_args => java_args
        }
      end
    end
  end
end

Buildr::Shell.providers << Buildr::Scala::ScalaShell
