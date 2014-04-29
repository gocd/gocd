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

  module Util
    extend self

    def java_platform?
      !!(RUBY_PLATFORM =~ /java/)
    end

    # In order to determine if we are running on a windows OS,
    # prefer this function instead of using Gem.win_platform?.
    #
    # Gem.win_platform? only checks these RUBY_PLATFORM global,
    # that in some cases like when running on JRuby is not
    # sufficient for our purpose:
    #
    # For JRuby, the value for RUBY_PLATFORM will always be 'java'
    # That's why this function checks on Config::CONFIG['host_os']
    def win_os?
      !!(RbConfig::CONFIG['host_os'] =~ /windows|cygwin|bccwin|cygwin|djgpp|mingw|mswin|mswin32|wince/i)
    end

    # Runs Ruby with these command line arguments.  The last argument may be a hash,
    # supporting the following keys:
    #   :command  -- Runs the specified script (e.g., :command=>'gem')
    #   :sudo     -- Run as sudo on operating systems that require it.
    #   :verbose  -- Override Rake's verbose flag.
    def ruby(*args)
      options = Hash === args.last ? args.pop : {}
      cmd = []
      ruby_bin = normalize_path(RbConfig::CONFIG['ruby_install_name'], RbConfig::CONFIG['bindir'])
      if options.delete(:sudo) && !(win_os? || Process.uid == File.stat(ruby_bin).uid)
        cmd << 'sudo' << '-u' << "##{File.stat(ruby_bin).uid}"
      end
      cmd << ruby_bin
      cmd << '-S' << options.delete(:command) if options[:command]
      cmd.concat args.flatten
      cmd.push options
      sh *cmd do |ok, status|
        ok or fail "Command ruby failed with status (#{status ? status.exitstatus : 'unknown'}): [#{cmd.join(" ")}]"
      end
    end

    # Just like File.expand_path, but for windows systems it
    # capitalizes the drive name and ensures backslashes are used
    def normalize_path(path, *dirs)
      path = File.expand_path(path, *dirs)
      if win_os?
        path.gsub!('/', '\\').gsub!(/^[a-zA-Z]+:/) { |s| s.upcase }
      else
        path
      end
    end

    # Return the timestamp of file, without having to create a file task
    def timestamp(file)
      if File.exist?(file)
        File.mtime(file)
      else
        Rake::EARLY
      end
    end

    def uuid
      return SecureRandom.uuid if SecureRandom.respond_to?(:uuid)
      ary = SecureRandom.random_bytes(16).unpack("NnnnnN")
      ary[2] = (ary[2] & 0x0fff) | 0x4000
      ary[3] = (ary[3] & 0x3fff) | 0x8000
      "%08x-%04x-%04x-%04x-%04x%08x" % ary
    end

    # Return the path to the first argument, starting from the path provided by the
    # second argument.
    #
    # For example:
    #   relative_path('foo/bar', 'foo')
    #   => 'bar'
    #   relative_path('foo/bar', 'baz')
    #   => '../foo/bar'
    #   relative_path('foo/bar')
    #   => 'foo/bar'
    #   relative_path('/foo/bar', 'baz')
    #   => '/foo/bar'
    def relative_path(to, from = '.')
      to = Pathname.new(to).cleanpath
      return to.to_s if from.nil?
      to_path = Pathname.new(File.expand_path(to.to_s, "/"))
      from_path = Pathname.new(File.expand_path(from.to_s, "/"))
      to_path.relative_path_from(from_path).to_s
    end

    # Generally speaking, it's not a good idea to operate on dot files (files starting with dot).
    # These are considered invisible files (.svn, .hg, .irbrc, etc).  Dir.glob/FileList ignore them
    # on purpose.  There are few cases where we do have to work with them (filter, zip), a better
    # solution is welcome, maybe being more explicit with include.  For now, this will do.
    def recursive_with_dot_files(*dirs)
      FileList[dirs.map { |dir| File.join(dir, '/**/{*,.*}') }].reject { |file| File.basename(file) =~ /^[.]{1,2}$/ }
    end

    # :call-seq:
    #   replace_extension(filename) => filename_with_updated_extension
    #
    # Replace the file extension, e.g.,
    #   replace_extension("foo.zip", "txt") => "foo.txt"
    def replace_extension(filename, new_ext)
      ext = File.extname(filename)
      if filename =~ /\.$/
        filename + new_ext
      elsif ext == ""
        filename + "." + new_ext
      else
        filename[0..-ext.length] + new_ext
      end
    end

  end # Util
end


class Object #:nodoc:
  unless defined? instance_exec # 1.9
    module InstanceExecMethods #:nodoc:
    end
    include InstanceExecMethods

    # Evaluate the block with the given arguments within the context of
    # this object, so self is set to the method receiver.
    #
    # From Mauricio's http://eigenclass.org/hiki/bounded+space+instance_exec
    def instance_exec(*args, &block)
      begin
        old_critical, Thread.critical = Thread.critical, true
        n = 0
        n += 1 while respond_to?(method_name = "__instance_exec#{n}")
        InstanceExecMethods.module_eval { define_method(method_name, &block) }
      ensure
        Thread.critical = old_critical
      end

      begin
        send(method_name, *args)
      ensure
        InstanceExecMethods.module_eval { remove_method(method_name) } rescue nil
      end
    end
  end
end

module Kernel #:nodoc:
  unless defined? tap # 1.9
    def tap
      yield self if block_given?
      self
    end
  end
end

class Symbol #:nodoc:
  unless defined? to_proc # 1.9
    # Borrowed from Ruby 1.9.
    def to_proc
      Proc.new{|*args| args.shift.__send__(self, *args)}
    end
  end
end

unless defined? BasicObject # 1.9
  class BasicObject #:nodoc:
    (instance_methods - ['__send__', '__id__', '==', 'send', 'send!', 'respond_to?', 'equal?', 'object_id']).
      each do |method|
        undef_method method
      end

    def self.ancestors
      [Kernel]
    end
  end
end


class OpenObject < Hash

  def initialize(source=nil, &block)
    super &block
    update source if source
  end

  def method_missing(symbol, *args)
    if symbol.to_s =~ /=$/
      self[symbol.to_s[0..-2].to_sym] = args.first
    else
      self[symbol]
    end
  end
end


class Hash

  class << self

    # :call-seq:
    #   Hash.from_java_properties(string)
    #
    # Returns a hash from a string in the Java properties file format. For example:
    #   str = 'foo=bar\nbaz=fab'
    #   Hash.from_properties(str)
    #   => { 'foo'=>'bar', 'baz'=>'fab' }.to_properties
    def from_java_properties(string)
      hash = {}
      input_stream = Java.java.io.StringBufferInputStream.new(string)
      java_properties = Java.java.util.Properties.new
      java_properties.load input_stream
      keys = java_properties.keySet.iterator
      while keys.hasNext
        # Calling key.next in JRuby returns a java.lang.String, behaving as a Ruby string and life is good.
        # MRI, unfortunately, treats next() like the interface says returning an object that's not a String,
        # and the Hash doesn't work the way we need it to.  Unfortunately, we can call toString on MRI's object,
        # but not on the JRuby one; calling to_s on the JRuby object returns what we need, but ... you guessed it.
        #  So this seems like the one hack to unite them both.
        #key = Java.java.lang.String.valueOf(keys.next.to_s)
        key = keys.next
        key = key.toString unless String === key
        hash[key] = java_properties.getProperty(key)
      end
      hash
    end

  end

  # :call-seq:
  #   only(keys*) => hash
  #
  # Returns a new hash with only the specified keys.
  #
  # For example:
  #   { :a=>1, :b=>2, :c=>3, :d=>4 }.only(:a, :c)
  #   => { :a=>1, :c=>3 }
  def only(*keys)
    keys.inject({}) { |hash, key| has_key?(key) ? hash.merge(key=>self[key]) : hash }
  end


  # :call-seq:
  #   except(keys*) => hash
  #
  # Returns a new hash without the specified keys.
  #
  # For example:
  #   { :a=>1, :b=>2, :c=>3, :d=>4 }.except(:a, :c)
  #   => { :b=>2, :d=>4 }
  def except(*keys)
    (self.keys - keys).inject({}) { |hash, key| hash.merge(key=>self[key]) }
  end

  # :call-seq:
  #   to_java_properties => string
  #
  # Convert hash to string format used for Java properties file. For example:
  #   { 'foo'=>'bar', 'baz'=>'fab' }.to_properties
  #   => foo=bar
  #      baz=fab
  def to_java_properties
    keys.sort.map { |key|
      value = self[key].gsub(/[\t\r\n\f\\]/) { |escape| "\\" + {"\t"=>"t", "\r"=>"r", "\n"=>"n", "\f"=>"f", "\\"=>"\\"}[escape] }
      "#{key}=#{value}"
    }.join("\n")
  end

end

if Buildr::Util.java_platform?
  require 'ffi'

  # Workaround for BUILDR-535: when requiring 'ffi', JRuby defines an :error
  # method with arity 0.
  class Module
    remove_method :error if method_defined?(:error)
  end

  module Buildr
    class ProcessStatus
      attr_reader :pid, :termsig, :stopsig, :exitstatus

      def initialize(pid, success, exitstatus)
        @pid = pid
        @success = success
        @exitstatus = exitstatus

        @termsig = nil
        @stopsig = nil
      end

      def &(num)
        pid & num
      end

      def ==(other)
        pid == other.pid
      end

      def >>(num)
        pid >> num
      end

      def coredump?
        false
      end

      def exited?
        true
      end

      def stopped?
        false
      end

      def success?
        @success
      end

      def to_i
        pid
      end

      def to_int
        pid
      end

      def to_s
        pid.to_s
      end
    end
  end

  module FileUtils
    extend FFI::Library

    ffi_lib FFI::Platform::LIBC

    alias_method :__jruby_system__, :system
    attach_function :system, [:string], :int
    alias_method :__native_system__, :system
    alias_method :system, :__jruby_system__

    # code "borrowed" directly from Rake
    def sh(*cmd, &block)
      options = (Hash === cmd.last) ? cmd.pop : {}
      unless block_given?
        show_command = cmd.join(" ")
        show_command = show_command[0,42] + "..."

        block = lambda { |ok, status|
          ok or fail "Command failed with status (#{status.exitstatus}): [#{show_command}]"
        }
      end
      if RakeFileUtils.verbose_flag == Rake::FileUtilsExt::DEFAULT
        options[:verbose] = false
      else
        options[:verbose] ||= RakeFileUtils.verbose_flag
      end
      options[:noop]    ||= RakeFileUtils.nowrite_flag
      rake_check_options options, :noop, :verbose
      rake_output_message cmd.join(" ") if options[:verbose]
      unless options[:noop]
        if Buildr::Util.win_os?
          # Ruby uses forward slashes regardless of platform,
          # unfortunately cd c:/some/path fails on Windows
          pwd = Dir.pwd.gsub(%r{/}, '\\')
          cd = "cd /d \"#{pwd}\" && "
        else
          cd = "cd '#{Dir.pwd}' && "
        end
        args = if cmd.size > 1 then cmd[1..cmd.size] else [] end

        res = if Buildr::Util.win_os? && cmd.size == 1
          __native_system__("#{cd} call #{cmd.first}")
        else
          arg_str = args.map { |a| "'#{a}'" }
          __native_system__(cd + cmd.first + ' ' + arg_str.join(' '))
        end
        status = Buildr::ProcessStatus.new(0, res == 0, res)    # KLUDGE
        block.call(res == 0, status)
      end
    end

  end
else
  module FileUtils
    # code "borrowed" directly from Rake
    def sh(*cmd, &block)
      options = (Hash === cmd.last) ? cmd.pop : {}
      unless block_given?
        show_command = cmd.join(" ")
        show_command = show_command[0,42] + "..."

        block = lambda { |ok, status|
          ok or fail "Command failed with status (#{status.exitstatus}): [#{show_command}]"
        }
      end
      if RakeFileUtils.verbose_flag == Rake::FileUtilsExt::DEFAULT
        options[:verbose] = false
      else
        options[:verbose] ||= RakeFileUtils.verbose_flag
      end
      options[:noop]    ||= RakeFileUtils.nowrite_flag
      rake_check_options options, :noop, :verbose
      rake_output_message cmd.join(" ") if options[:verbose]
      unless options[:noop]
        if Buildr::Util.win_os?
          # Ruby uses forward slashes regardless of platform,
          # unfortunately cd c:/some/path fails on Windows
          pwd = Dir.pwd.gsub(%r{/}, '\\')
          cd = "cd /d \"#{pwd}\" && "
        else
          cd = "cd '#{Dir.pwd}' && "
        end

        args = if cmd.size > 1 then cmd[1..cmd.size] else [] end

        res = if Buildr::Util.win_os? && cmd.size == 1
          system("#{cd} call #{cmd.first}")
        else
          arg_str = args.map { |a| "'#{a}'" }
          system(cd + cmd.first + ' ' + arg_str.join(' '))
        end

        block.call(res, $?)
      end
    end
  end
end
