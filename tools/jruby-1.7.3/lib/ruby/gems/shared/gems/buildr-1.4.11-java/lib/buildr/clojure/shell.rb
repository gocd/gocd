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

module Buildr::Shell

  class Clojure < Base
    include JRebel

    specify :name => :clj, :lang => :clojure

    # don't build if it's *only* Clojure sources
    def build?
      !has_source?(:clojure) or has_source?(:java) or has_source?(:scala) or has_source?(:groovy)
    end

    def launch(task)
      cp = project.compile.dependencies +
           ::Buildr::Clojure.dependencies +
           [ build? ? project.path_to(:target, :classes) : project.path_to(:src, :main, :clojure) ]

      Java::Commands.java 'jline.ConsoleRunner', 'clojure.lang.Repl', {
        :properties => jrebel_props(project).merge(task.properties),
        :classpath => cp,
        :java_args => jrebel_args + task.java_args
      }
    end

  private
    def clojure_home
      @home ||= ENV['CLOJURE_HOME']
    end

    def has_source?(lang)
      File.exists? project.path_to(:src, :main, lang)
    end
  end
end

Buildr::Shell.providers << Buildr::Shell::Clojure

