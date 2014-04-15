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
  # Provides ANTLR grammar generation tasks. Require explicitly using <code>require "buildr/antlr"</code>.
  module ANTLR
    REQUIRES = [ "org.antlr:antlr:jar:3.0", "antlr:antlr:jar:2.7.7", "org.antlr:stringtemplate:jar:3.0" ]

    Java.classpath << REQUIRES

    class << self
      def antlr(*args)
        options = Hash === args.last ? args.pop : {}
        rake_check_options options, :output, :token

        args = args.flatten.map(&:to_s).collect { |f| File.directory?(f) ? FileList[f + "/**/*.g"] : f }.flatten
        args = ["-o",  options[:output]] + args if options[:output]
        if options[:token]
          # antlr expects the token directory to exist when it starts
          mkdir_p options[:token] 
          args = ["-lib",  options[:token]] + args 
        end
        Java.load
        #Java.org.antlr.Tool.new_with_sig("[Ljava.lang.String;", args).process
        Java.org.antlr.Tool.new(args.to_java(Java.java.lang.String)).process
      end
    end

    def antlr(*args)
      if Hash === args.last
        options = args.pop 
        in_package = options[:in_package].split(".")
        token = options[:token].split(".") if options[:token]
      else
        in_package = []; token = nil
      end
      file(path_to(:target, :generated, :antlr)=>args.flatten) do |task|
        args = {:output=>File.join(task.name, in_package)}
        args.merge!({:token=>File.join(task.name, token)}) if token
        ANTLR.antlr task.prerequisites, args
      end         
    end

  end
  
  class Project
    include ANTLR
  end
end
