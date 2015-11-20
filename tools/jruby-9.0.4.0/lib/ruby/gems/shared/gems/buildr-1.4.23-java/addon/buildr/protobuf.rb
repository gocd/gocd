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

  # Provides Protocol buffer code generation tasks.
  #
  # Require explicitly using <code>require "buildr/protobuf"</code>.
  #
  # Usage in your project:
  #
  #   protoc _("path/to/proto/files")
  #
  # and also supports two options,
  #
  #  :output => "target/generated/protoc"  # this is the default
  #  :lang => "java"                       # defaults to compile.language
  #
  module Protobuf
    class << self
      def protoc(*args)
        options = Hash === args.last ? args.pop : {}
        rake_check_options options, :output, :lang, :include

        options[:lang] ||= :java
        options[:output] ||= File.expand_path "target/generated/protoc"
        options[:include] ||= []

        command_line = []

        command_line << "--#{options[:lang]}_out=#{options[:output]}" if options[:output]

        (paths_from_sources(*args) + options[:include]).each { |i| command_line << "-I#{i}" }

        command_line += files_from_sources(*args)

        mkdir_p( options[:output] )

        system protoc_path, *command_line
      end

      def protoc_path
        ENV['PROTOC'] || "protoc"
      end

      def files_from_sources(*args)
        args.flatten.map(&:to_s).collect { |f| File.directory?(f) ? FileList[f + "/**/*.proto"] : f }.flatten
      end

      def paths_from_sources(*args)
        args.flatten.map(&:to_s).collect { |f| File.directory?(f) ? f : File.dirname(f) }
      end
    end

    def protoc(*args)
      if Hash === args.last
        options = args.pop
      else
        options = {}
      end

      options[:output] ||= path_to(:target, :generated, :protoc)
      options[:lang] ||= compile.language if compile.language

      file(options[:output]=>Protobuf.files_from_sources(*args)) do |task|
        Protobuf.protoc task.prerequisites, options
      end
    end

  end

  class Project
    include Protobuf
  end
end
