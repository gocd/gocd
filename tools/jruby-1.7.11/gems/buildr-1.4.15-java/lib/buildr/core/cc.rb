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

module Buildr #:nodoc:
  class CCTask < Rake::Task
    attr_accessor :delay
    attr_reader :project

    def initialize(*args)
      super
      @delay = 0.2
      enhance do
        monitor_and_compile
      end
    end

  private

    # run block on sub-projects depth-first, then on this project
    def each_project(&block)
      depth_first = lambda do |p|
        p.projects.each { |c| depth_first.call(c, &block) }
        block.call(p)
      end
      depth_first.call(@project)
    end

    def associate_with(project)
      @project = project
    end

    def monitor_and_compile
      # we don't want to actually fail if our dependencies don't succeed
      begin
        each_project { |p| p.test.compile.invoke }
        build_completed(project)
      rescue Exception => ex
        $stderr.puts Buildr::Console.color(ex.message, :red)
        $stderr.puts

        build_failed(project, ex)
      end

      srcs = []
      each_project do |p|
        srcs += p.compile.sources.map(&:to_s)
        srcs += p.test.compile.sources.map(&:to_s)
        srcs += p.resources.sources.map(&:to_s)
      end
      if srcs.length == 1
        info "Monitoring directory: #{srcs.first}"
      else
        info "Monitoring directories: [#{srcs.join ', '}]"
      end

      timestamps = lambda do
        times = {}
        srcs.each do |a|
          if File.directory? a
            Dir.glob("#{a}/**/*").map { |f| times[f] = File.mtime f }
          elsif File.exist? a
            times[a] = File.mtime a
          end
        end
        times
      end

      old_times = timestamps.call()

      while true
        sleep delay

        new_times = timestamps.call()
        changed = changed(new_times, old_times)
        old_times = new_times

        unless changed.empty?
          info ''    # better spacing

          changed.each do |file|
            info "Detected changes in #{file}"
          end

          each_project do |p|
            # transitively reenable prerequisites
            reenable = lambda do |t|
              t = task(t)
              t.reenable
              t.prerequisites.each { |c| reenable.call(c) }
            end
            reenable.call(p.test.compile)
          end

          successful = true
          begin
            each_project { |p| p.test.compile.invoke }
            build_completed(project)
          rescue Exception => ex
            $stderr.puts Buildr::Console.color(ex.message, :red)
            build_failed(project, ex)
            successful = false
          end

          puts Buildr::Console.color("Build complete", :green) if successful
        end
      end
    end

    def build_completed(project)
      Buildr.application.build_completed('Compilation successful', project.path_to)
    end

    def build_failed(project, ex = nil)
      Buildr.application.build_failed('Compilation failed', project.path_to, ex)
    end

    def changed(new_times, old_times)
      changed = []
      new_times.each do |(fname,newtime)|
        if old_times[fname].nil? || old_times[fname] < newtime
          changed << fname
        end
      end

      # detect deletion (slower than it could be)
      old_times.each_key do |fname|
        changed << fname unless new_times.has_key? fname
      end

      changed
    end
  end

  module CC
    include Extension

    first_time do
      desc 'Execute continuous compilation, listening to changes'
      Project.local_task('cc') { |name|  "Executing continuous compilation for #{name}" }
    end

    before_define do |project|
      cc = CCTask.define_task :cc
      cc.send :associate_with, project
    end

    def cc
      task :cc
    end
  end

  class Project
    include CC
  end
end
