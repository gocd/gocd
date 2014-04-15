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

  module Shell

    class BeanShell < Base
      include Buildr::JRebel

      VERSION = '2.0b4'

      specify :name => :bsh, :languages => [:java]

      class << self
        def version
          Buildr.settings.build['bsh'] || VERSION
        end

        def artifact
          "org.beanshell:bsh:jar:#{version}"
        end
      end

      def launch(task)
        cp = ( project.compile.dependencies +
               [project.path_to(:target, :classes), Buildr.artifact(BeanShell.artifact)] +
               task.classpath )
        Java::Commands.java 'bsh.Console', {
          :properties => jrebel_props(project).merge(task.properties),
          :classpath => cp,
          :java_args => jrebel_args + task.java_args
        }
      end

    end # BeanShell


    class JIRB < Base
      include JRebel

      JRUBY_VERSION = '1.6.2'

      def launch(task)
        if jruby_home     # if JRuby is installed, use it
          cp = project.compile.dependencies +
            [project.path_to(:target, :classes)] +
            Dir.glob("#{jruby_home}#{File::SEPARATOR}lib#{File::SEPARATOR}*.jar") +
            task.classpath

          props = {
            'jruby.home' => jruby_home,
            'jruby.lib' => "#{jruby_home}#{File::SEPARATOR}lib"
          }
          props.merge! jrebel_props(project)
          props.merge! task.properties

          if not Util.win_os?
            uname = `uname -m`
            cpu = if uname =~ /i[34567]86/
              'i386'
            elsif uname == 'i86pc'
              'x86'
            elsif uname =~ /amd64|x86_64/
              'amd64'
            end

            os = `uname -s | tr '[A-Z]' '[a-z]'`
            path = if os == 'darwin'
              'darwin'
            else
              "#{os}-#{cpu}"
            end

            props['jna.boot.library.path'] = "#{jruby_home}/lib/native/#{path}"
          end

          props['jruby.script'] = if Util.win_os? then 'jruby.bat' else 'jruby' end
          props['jruby.shell'] = if Util.win_os? then 'cmd.exe' else '/bin/sh' end

          args = [
            "-Xbootclasspath/a:#{Dir.glob("#{jruby_home}#{File::SEPARATOR}lib#{File::SEPARATOR}jruby*.jar").join File::PATH_SEPARATOR}"
          ] + jrebel_args + task.java_args

          Java::Commands.java 'org.jruby.Main', "#{jruby_home}#{File::SEPARATOR}bin#{File::SEPARATOR}jirb", {
            :properties => props,
            :classpath => cp,
            :java_args => args
          }
        else
          cp = project.compile.dependencies + [ jruby_artifact, project.path_to(:target, :classes) ] +
               task.classpath
          props = jrebel_props(project).merge(task.properties)
          args = jrebel_args + task.java_args

          Java::Commands.java 'org.jruby.Main', '--command', 'irb', {
            :properties => props,
            :classpath => cp,
            :java_args => args
          }
        end
      end

    private
      def jruby_home
        @jruby_home ||= RUBY_PLATFORM =~ /java/ ? RbConfig::CONFIG['prefix'] : ENV['JRUBY_HOME']
      end

      def jruby_artifact
        version = Buildr.settings.build['jruby'] || JRUBY_VERSION
        "org.jruby:jruby-complete:jar:#{version}"
      end

    end
  end
end

Buildr::Shell.providers << Buildr::Shell::BeanShell
Buildr::Shell.providers << Buildr::Shell::JIRB

