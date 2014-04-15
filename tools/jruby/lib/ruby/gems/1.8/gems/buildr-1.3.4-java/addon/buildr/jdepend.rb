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


require 'buildr/java'


module Buildr

  # Addes the <code>jdepend:swing</code>, <code>jdepend:text</code> and <code>jdepend:xml</code> tasks.
  # Require explicitly using <code>require "buildr/jdepend"</code>.
  module Jdepend

    REQUIRES = ["jdepend:jdepend:jar:2.9.1"]

    class << self

      def requires()
        @requires ||= Buildr.artifacts(REQUIRES).each(&:invoke).map(&:to_s)
      end

      def paths()
        Project.projects.map(&:compile).each(&:invoke).map(&:target).
          map(&:to_s).select { |path| File.exist?(path) }.map { |path| File.expand_path(path) }
      end

    end

    namespace "jdepend" do

      desc "Runs JDepend on all your projects (Swing UI)"
      task "swing" do
        Java::Commands.java "jdepend.swingui.JDepend", paths, :classpath=>requires, :name=>"JDepend"
      end

      desc "Runs JDepend on all your projects (Text UI)"
      task "text" do
        Java::Commands.java "jdepend.textui.JDepend", paths, :classpath=>requires, :name=>"JDepend"
      end

      desc "Runs JDepend on all your projects (XML output to jdepend.xml)"
      task "xml" do
        Java::Commands.java "jdepend.xmlui.JDepend", "-file", "jdepend.xml", paths, :classpath=>requires, :name=>"JDepend"
        puts "Created jdepend.xml"
      end
    end
  end
end
