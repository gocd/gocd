require 'bundler/dependency'
require 'bundler/ruby_dsl'

module Bundler
  class Dsl
    include RubyDsl

    def self.evaluate(gemfile, lockfile, unlock)
      builder = new
      builder.eval_gemfile(gemfile)
      builder.to_definition(lockfile, unlock)
    end

    VALID_PLATFORMS = Bundler::Dependency::PLATFORM_MAP.keys.freeze

    attr_accessor :dependencies

    def initialize
      @source               = nil
      @sources              = SourceList.new
      @git_sources          = {}
      @dependencies         = []
      @groups               = []
      @install_conditionals = []
      @optional_groups      = []
      @platforms            = []
      @env                  = nil
      @ruby_version         = nil
      add_git_sources
    end

    def eval_gemfile(gemfile, contents = nil)
      contents ||= Bundler.read_file(gemfile.to_s)
      instance_eval(contents, gemfile.to_s, 1)
    rescue Exception => e
      message = "There was an error parsing `#{File.basename gemfile.to_s}`: #{e.message}"
      raise DSLError.new(message, gemfile, e.backtrace, contents)
    end

    def gemspec(opts = nil)
      path              = opts && opts[:path] || '.'
      glob              = opts && opts[:glob]
      name              = opts && opts[:name] || '{,*}'
      development_group = opts && opts[:development_group] || :development
      expanded_path     = File.expand_path(path, Bundler.default_gemfile.dirname)

      gemspecs = Dir[File.join(expanded_path, "#{name}.gemspec")]

      case gemspecs.size
      when 1
        spec = Bundler.load_gemspec(gemspecs.first)

        unless spec
          raise InvalidOption, "There was an error loading the gemspec at " \
            "#{file}. Make sure you can build the gem, then try again."
        end

        gem spec.name, :path => path, :glob => glob

        group(development_group) do
          spec.development_dependencies.each do |dep|
            gem dep.name, *(dep.requirement.as_list + [:type => :development])
          end
        end
      when 0
        raise InvalidOption, "There are no gemspecs at #{expanded_path}."
      else
        raise InvalidOption, "There are multiple gemspecs at #{expanded_path}. " \
          "Please use the :name option to specify which one should be used."
      end
    end

    def gem(name, *args)
      options = args.last.is_a?(Hash) ? args.pop.dup : {}
      version = args || [">= 0"]

      normalize_options(name, version, options)

      dep = Dependency.new(name, version, options)

      # if there's already a dependency with this name we try to prefer one
      if current = @dependencies.find { |d| d.name == dep.name }
        if current.requirement != dep.requirement
          if current.type == :development
            @dependencies.delete current
          elsif dep.type == :development
            return
          else
            raise GemfileError, "You cannot specify the same gem twice with different version requirements.\n" \
                            "You specified: #{current.name} (#{current.requirement}) and #{dep.name} (#{dep.requirement})"
          end

        else
          Bundler.ui.warn "Your Gemfile lists the gem #{current.name} (#{current.requirement}) more than once.\n" \
                          "You should probably keep only one of them.\n" \
                          "While it's not a problem now, it could cause errors if you change the version of just one of them later."
        end

        if current.source != dep.source
          if current.type == :development
            @dependencies.delete current
          elsif dep.type == :development
            return
          else
            raise GemfileError, "You cannot specify the same gem twice coming from different sources.\n" \
                            "You specified that #{dep.name} (#{dep.requirement}) should come from " \
                            "#{current.source || 'an unspecified source'} and #{dep.source}\n"
          end
        end
      end

      @dependencies << dep
    end

    def source(source, &blk)
      source = normalize_source(source)
      if block_given?
        with_source(@sources.add_rubygems_source("remotes" => source), &blk)
      else
        check_primary_source_safety(@sources)
        @sources.add_rubygems_remote(source)
      end
    end

    def git_source(name, &block)
      unless block_given?
        raise InvalidOption, "You need to pass a block to #git_source"
      end

      if valid_keys.include?(name.to_s)
        raise InvalidOption, "You cannot use #{name} as a git source. It " \
          "is a reserved key. Reserved keys are: #{valid_keys.join(", ")}"
      end

      @git_sources[name.to_s] = block
    end

    def path(path, options = {}, &blk)
      with_source(@sources.add_path_source(normalize_hash(options).merge("path" => Pathname.new(path))), &blk)
    end

    def git(uri, options = {}, &blk)
      unless block_given?
        msg = "You can no longer specify a git source by itself. Instead, \n" \
              "either use the :git option on a gem, or specify the gems that \n" \
              "bundler should find in the git source by passing a block to \n" \
              "the git method, like: \n\n" \
              "  git 'git://github.com/rails/rails.git' do\n" \
              "    gem 'rails'\n" \
              "  end"
        raise DeprecatedError, msg
      end

      with_source(@sources.add_git_source(normalize_hash(options).merge("uri" => uri)), &blk)
    end

    def github(repo, options = {})
      raise ArgumentError, "Github sources require a block" unless block_given?
      github_uri  = @git_sources["github"].call(repo)
      git_options = normalize_hash(options).merge("uri" => github_uri)
      git_source  = @sources.add_git_source(git_options)
      with_source(git_source) { yield }
    end

    def to_definition(lockfile, unlock)
      Definition.new(lockfile, @dependencies, @sources, unlock, @ruby_version, @optional_groups)
    end

    def group(*args, &blk)
      opts = Hash === args.last ? args.pop.dup : {}
      normalize_group_options(opts, args)

      @groups.concat args

      if opts["optional"]
        optional_groups = args - @optional_groups
        @optional_groups.concat optional_groups
      end

      yield
    ensure
      args.each { @groups.pop }
    end

    def install_if(*args, &blk)
      @install_conditionals.concat args
      blk.call
    ensure
      args.each { @install_conditionals.pop }
    end

    def platforms(*platforms)
      @platforms.concat platforms
      yield
    ensure
      platforms.each { @platforms.pop }
    end
    alias_method :platform, :platforms

    def env(name)
      @env, old = name, @env
      yield
    ensure
      @env = old
    end

    def method_missing(name, *args)
      raise GemfileError, "Undefined local variable or method `#{name}' for Gemfile"
    end

  private

    def add_git_sources
      git_source(:github) do |repo_name|
        repo_name = "#{repo_name}/#{repo_name}" unless repo_name.include?("/")
        "git://github.com/#{repo_name}.git"
      end

      git_source(:gist){ |repo_name| "https://gist.github.com/#{repo_name}.git" }

      git_source(:bitbucket) do |repo_name|
        user_name, repo_name = repo_name.split '/'
        repo_name ||= user_name
        "https://#{user_name}@bitbucket.org/#{user_name}/#{repo_name}.git"
      end
    end

    def with_source(source)
      if block_given?
        @source = source
        yield
      end
      source
    ensure
      @source = nil
    end

    def normalize_hash(opts)
      opts.keys.each do |k|
        opts[k.to_s] = opts.delete(k) unless k.is_a?(String)
      end
      opts
    end

    def valid_keys
      @valid_keys ||= %w(group groups git path glob name branch ref tag require submodules platform platforms type source install_if)
    end

    def normalize_options(name, version, opts)
      if name.is_a?(Symbol)
        raise GemfileError, %{You need to specify gem names as Strings. Use 'gem "#{name}"' instead.}
      end
      if name =~ /\s/
        raise GemfileError, %{'#{name}' is not a valid gem name because it contains whitespace.}
      end

      normalize_hash(opts)

      git_names = @git_sources.keys.map(&:to_s)
      validate_keys("gem '#{name}'", opts, valid_keys + git_names)

      groups = @groups.dup
      opts["group"] = opts.delete("groups") || opts["group"]
      groups.concat Array(opts.delete("group"))
      groups = [:default] if groups.empty?

      install_if = @install_conditionals.dup
      install_if.concat Array(opts.delete("install_if"))
      install_if = install_if.reduce(true) do |memo, val|
        memo && (val.respond_to?(:call) ? val.call : val)
      end

      platforms = @platforms.dup
      opts["platforms"] = opts["platform"] || opts["platforms"]
      platforms.concat Array(opts.delete("platforms"))
      platforms.map! { |p| p.to_sym }
      platforms.each do |p|
        next if VALID_PLATFORMS.include?(p)
        raise GemfileError, "`#{p}` is not a valid platform. The available options are: #{VALID_PLATFORMS.inspect}"
      end

      # Save sources passed in a key
      if opts.has_key?("source")
        source = normalize_source(opts["source"])
        opts["source"] = @sources.add_rubygems_source("remotes" => source)
      end

      git_name = (git_names & opts.keys).last
      if @git_sources[git_name]
        opts["git"] = @git_sources[git_name].call(opts[git_name])
      end

      ["git", "path"].each do |type|
        if param = opts[type]
          if version.first && version.first =~ /^\s*=?\s*(\d[^\s]*)\s*$/
            options = opts.merge("name" => name, "version" => $1)
          else
            options = opts.dup
          end
          source = send(type, param, options) {}
          opts["source"] = source
        end
      end

      opts["source"]       ||= @source
      opts["env"]          ||= @env
      opts["platforms"]      = platforms.dup
      opts["group"]          = groups
      opts["should_include"] = install_if
    end

    def normalize_group_options(opts, groups)
      normalize_hash(opts)

      groups = groups.map {|group| ":#{group}" }.join(", ")
      validate_keys("group #{groups}", opts, %w(optional))

      opts["optional"] ||= false
    end

    def validate_keys(command, opts, valid_keys)
      invalid_keys = opts.keys - valid_keys
      if invalid_keys.any?
        message = "You passed #{invalid_keys.map{|k| ':'+k }.join(", ")} "
        message << if invalid_keys.size > 1
                     "as options for #{command}, but they are invalid."
                   else
                     "as an option for #{command}, but it is invalid."
                   end

        message << " Valid options are: #{valid_keys.join(", ")}"
        raise InvalidOption, message
      end
    end

    def normalize_source(source)
      case source
      when :gemcutter, :rubygems, :rubyforge
        Bundler.ui.warn "The source :#{source} is deprecated because HTTP " \
          "requests are insecure.\nPlease change your source to 'https://" \
          "rubygems.org' if possible, or 'http://rubygems.org' if not."
        "http://rubygems.org"
      when String
        source
      else
        raise GemfileError, "Unknown source '#{source}'"
      end
    end

    def check_primary_source_safety(source)
      return unless source.rubygems_primary_remotes.any?

      if Bundler.settings[:disable_multisource]
        raise GemspecError, "Warning: this Gemfile contains multiple primary sources. " \
          "Each source after the first must include a block to indicate which gems " \
          "should come from that source. To downgrade this error to a warning, run " \
          "`bundle config --delete disable_multisource`."
      else
        Bundler.ui.warn "Warning: this Gemfile contains multiple primary sources. " \
          "Using `source` more than once without a block is a security risk, and " \
          "may result in installing unexpected gems. To resolve this warning, use " \
          "a block to indicate which gems should come from the secondary source. " \
          "To upgrade this warning to an error, run `bundle config " \
          "disable_multisource true`."
      end
    end

    class DSLError < GemfileError
      # @return [String] the description that should be presented to the user.
      #
      attr_reader :description

      # @return [String] the path of the dsl file that raised the exception.
      #
      attr_reader :dsl_path

      # @return [Exception] the backtrace of the exception raised by the
      #         evaluation of the dsl file.
      #
      attr_reader :backtrace

      # @param [Exception] backtrace @see backtrace
      # @param [String]    dsl_path  @see dsl_path
      #
      def initialize(description, dsl_path, backtrace, contents = nil)
        @status_code = $!.respond_to?(:status_code) && $!.status_code

        @description = description
        @dsl_path    = dsl_path
        @backtrace   = backtrace
        @contents    = contents
      end

      def status_code
        @status_code || super
      end

      # @return [String] the contents of the DSL that cause the exception to
      #         be raised.
      #
      def contents
        @contents ||= begin
          dsl_path && File.exist?(dsl_path) && File.read(dsl_path)
        end
      end

      # The message of the exception reports the content of podspec for the
      # line that generated the original exception.
      #
      # @example Output
      #
      #   Invalid podspec at `RestKit.podspec` - undefined method
      #   `exclude_header_search_paths=' for #<Pod::Specification for
      #   `RestKit/Network (0.9.3)`>
      #
      #       from spec-repos/master/RestKit/0.9.3/RestKit.podspec:36
      #       -------------------------------------------
      #           # because it would break: #import <CoreData/CoreData.h>
      #    >      ns.exclude_header_search_paths = 'Code/RestKit.h'
      #         end
      #       -------------------------------------------
      #
      # @return [String] the message of the exception.
      #
      def to_s
        @to_s ||= begin
          trace_line, description = parse_line_number_from_description

          m = "\n[!] "
          m << description
          m << ". Bundler cannot continue.\n"

          return m unless backtrace && dsl_path && contents

          trace_line = backtrace.find { |l| l.include?(dsl_path.to_s) } || trace_line
          return m unless trace_line
          line_numer = trace_line.split(':')[1].to_i - 1
          return m unless line_numer

          lines      = contents.lines.to_a
          indent     = ' #  '
          indicator  = indent.gsub('#', '>')
          first_line = (line_numer.zero?)
          last_line  = (line_numer == (lines.count - 1))

          m << "\n"
          m << "#{indent}from #{trace_line.gsub(/:in.*$/, '')}\n"
          m << "#{indent}-------------------------------------------\n"
          m << "#{indent}#{    lines[line_numer - 1] }" unless first_line
          m << "#{indicator}#{ lines[line_numer] }"
          m << "#{indent}#{    lines[line_numer + 1] }" unless last_line
          m << "\n" unless m.end_with?("\n")
          m << "#{indent}-------------------------------------------\n"
        end
      end

      private

      def parse_line_number_from_description
        description = self.description
        if dsl_path && description =~ /((#{Regexp.quote File.expand_path(dsl_path)}|#{Regexp.quote dsl_path.to_s}):\d+)/
          trace_line = Regexp.last_match[1]
          description = description.sub(/#{Regexp.quote trace_line}:\s*/, '').sub("\n", ' - ')
        end
        [trace_line, description]
      end
    end

  end

end
