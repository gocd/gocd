#!/usr/bin/env ruby
#
# Start script for JRuby interpreter.
#
# Uses a non-Java Ruby to start up JRuby.
#

class Classpath

  def initialize
    @classpath = []
  end

  def <<(jar)
    @classpath << jar
  end

  def to_s
    @classpath.join(File::PATH_SEPARATOR)
  end
end


class JavaLaunch

  attr_writer :classpath

  def initialize(jvm, main_class)
    @jvm, @main_class = jvm, main_class
    @options = {}
    @server_vm = false
    @classpath = nil
  end

  def server_vm=(server_vm)
    @server_vm = server_vm
  end

  def []=(name, value)
    @options[name] = value
  end

  def to_s
    result = "#{@jvm} "
    if @server_vm
      result << "-server "
    end
    if @classpath
      result << "-classpath #{@classpath.to_s} "
    end
    @options.each_pair {|name, value|
      result << "-D#{name}=#{value} "
    }
    result << @main_class
    return result
  end
end


java_home = ENV['JAVA_HOME']
if java_home.nil?
  puts("You must set JAVA_HOME to point at your Java " +
       "Development Kit installation")
end

bin_dir = File.dirname($0)
jruby_home = File.dirname(bin_dir)

jruby_opts = ENV['JRUBY_OPTS']
if jruby_opts.nil?
  jruby_opts = ""
end

launch = JavaLaunch.new("#{java_home}/bin/java", "org.jruby.Main")

classpath = Classpath.new

lib_path = "#{jruby_home}/lib"
Dir.new(lib_path).grep(/\.jar$/).each {|jar|
  classpath << (lib_path + '/' + jar)
}
launch.classpath = classpath

arguments = ARGV.entries
if arguments.include?('-server')
  launch.server_vm = true
  arguments.delete('-server')
end

# FIXME: for cygwin, convert paths

launch['jruby.home'] = jruby_home
launch['jruby.lib'] = jruby_home + '/lib'
launch['jruby.script'] = 'jruby.rb'
launch['jruby.shell'] = '/bin/sh'

exec("#{launch} #{jruby_opts} #{arguments.join(' ')}")
