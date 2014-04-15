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


require 'rbconfig'
require 'pathname'
autoload :Tempfile, 'tempfile'
autoload :YAML, 'yaml'
autoload :REXML, 'rexml/document'
gem 'xml-simple' ; autoload :XmlSimple, 'xmlsimple'
gem 'builder' ; autoload :Builder, 'builder' # A different kind of buildr, one we use to create XML.
require 'highline/import'


module Buildr
  
  module Util
    extend self

    def java_platform?
      RUBY_PLATFORM =~ /java/
    end

    # In order to determine if we are running on a windows OS,
    # prefer this function instead of using Gem.win_platform?.
    #
    # Gem.win_platform? only checks these RUBY_PLATFORM global,
    # that in some cases like when running on JRuby is not 
    # succifient for our purpose:
    #
    # For JRuby, the value for RUBY_PLATFORM will always be 'java'
    # That's why this function checks on Config::CONFIG['host_os']
    def win_os?
      Config::CONFIG['host_os'] =~ /windows|cygwin|bccwin|cygwin|djgpp|mingw|mswin|wince/i
    end

    # Runs Ruby with these command line arguments.  The last argument may be a hash,
    # supporting the following keys:
    #   :command  -- Runs the specified script (e.g., :command=>'gem')
    #   :sudo     -- Run as sudo on operating systems that require it.
    #   :verbose  -- Override Rake's verbose flag.
    def ruby(*args)
      options = Hash === args.last ? args.pop : {}
      cmd = []
      ruby_bin = File.expand_path(Config::CONFIG['ruby_install_name'], Config::CONFIG['bindir'])
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

    # Utility methods for running gem commands
    module Gems #:nodoc:
      extend self

      # Install gems specified by each Gem::Dependency if they are missing. This method prompts the user
      # for permission before installing anything.
      # 
      # Returns the installed Gem::Dependency objects or fails if permission not granted or when buildr
      # is not running interactively (on a tty)
      def install(*dependencies)
        raise ArgumentError, "Expected at least one argument" if dependencies.empty?
        remote = dependencies.map { |dep| Gem::SourceInfoCache.search(dep).last || dep }
        not_found_deps, to_install = remote.partition { |gem| gem.is_a?(Gem::Dependency) }
        fail Gem::LoadError, "Build requires the gems #{not_found_deps.join(', ')}, which cannot be found in local or remote repository." unless not_found_deps.empty?
        uses = "This build requires the gems #{to_install.map(&:full_name).join(', ')}:"
        fail Gem::LoadError, "#{uses} to install, run Buildr interactively." unless $stdout.isatty
        unless agree("#{uses} do you want me to install them? [Y/n]", true)
          fail Gem::LoadError, 'Cannot build without these gems.'
        end
        to_install.each do |spec|
          say "Installing #{spec.full_name} ... " if verbose
          command 'install', spec.name, '-v', spec.version.to_s, :verbose => false
          Gem.source_index.load_gems_in Gem::SourceIndex.installed_spec_directories
        end
        to_install
      end

      # Execute a GemRunner command
      def command(cmd, *args)
        options = Hash === args.last ? args.pop : {}
        gem_home = ENV['GEM_HOME'] || Gem.path.find { |f| File.writable?(f) }
        options[:sudo] = :root unless Util.win_os? || gem_home
        options[:command] = 'gem'
        args << options
        args.unshift '-i', gem_home if cmd == 'install' && gem_home && !args.any?{ |a| a[/-i|--install-dir/] }
        Util.ruby cmd, *args
      end

    end # Gems

  end # Util
end


if RUBY_VERSION < '1.9.0'
  module Kernel #:nodoc:
    # Borrowed from Ruby 1.9.
    def tap
      yield self if block_given?
      self
    end unless method_defined?('tap')
  end


  class Symbol #:nodoc:
    # Borrowed from Ruby 1.9.
    def to_proc
      Proc.new{|*args| args.shift.__send__(self, *args)}
    end unless method_defined?('to_proc')
  end

  # Also borrowed from Ruby 1.9.
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
        key = Java.java.lang.String.valueOf(keys.next)
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
