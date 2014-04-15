# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr
  module JaxbXjc
    class << self

      def jaxb_version
        "2.2.1"
      end

      # The specs for requirements
      def dependencies
        [
          "javax.xml.bind:jaxb-api:jar:#{jaxb_version}",
          "com.sun.xml.bind:jaxb-impl:jar:#{jaxb_version}",
          "com.sun.xml.bind:jaxb-xjc:jar:#{jaxb_version}"
        ]
      end

      # Repositories containing the requirements
      def remote_repository
        "http://download.java.net/maven/2"
      end

      def xjc(*args)
        cp = Buildr.artifacts(self.dependencies).each(&:invoke).map(&:to_s)
        Java::Commands.java 'com.sun.tools.xjc.XJCFacade', *(args + [{ :classpath => cp }])
      end
    end

    def compile_jaxb(files, *args)
      options = Hash === args.last ? args.pop.dup : {}
      rake_check_options options, :directory, :keep_content, :package, :id
      args = args.dup
      files = Array === files ? files.flatten : [files]

      target_dir = options[:directory] || path_to(:target, :generated, :jaxb)
      timestamp_file = File.expand_path("#{target_dir}/jaxb-#{options[:id] || 1}.cache")

      file(target_dir => timestamp_file)

      file(timestamp_file => files.flatten) do |task|
        rm_rf target_dir unless options[:keep_content]
        mkdir_p target_dir
        args << "-d" << target_dir
        args << "-p" << options[:package] if options[:package]
        args += files.collect{|f| f.to_s}
        JaxbXjc.xjc args
        touch timestamp_file
      end

      target_dir
    end
  end
end

class Buildr::Project
  include Buildr::JaxbXjc
end