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
      @source          = nil
      @sources         = SourceList.new
      @git_sources     = {}
      @dependencies    = []
      @groups          = []
      @platforms       = []
      @env             = nil
      @ruby_version    = nil
      add_git_sources
    end

    def eval_gemfile(gemfile, contents = nil)
      contents ||= Bundler.read_file(gemfile.to_s)
      instance_eval(contents, gemfile.to_s, 1)
    rescue SyntaxError => e
      syntax_msg = e.message.gsub("#{gemfile}:", 'on line ')
      raise GemfileError, "Gemfile syntax error #{syntax_msg}"
    rescue ScriptError, RegexpError, NameError, ArgumentError, RuntimeError => e
      e.backtrace[0] = "#{e.backtrace[0]}: #{e.message} (#{e.class})"
      Bundler.ui.warn e.backtrace.join("\n       ")
      raise GemfileError, "There was an error in your Gemfile," \
        " and Bundler cannot continue."
    end

    def gemspec(opts = nil)
      path              = opts && opts[:path] || '.'
      name              = opts && opts[:name] || '{,*}'
      development_group = opts && opts[:development_group] || :development
      expanded_path     = File.expand_path(path, Bundler.default_gemfile.dirname)

      gemspecs = Dir[File.join(expanded_path, "#{name}.gemspec")]

      case gemspecs.size
      when 1
        spec = Bundler.load_gemspec(gemspecs.first)
        raise InvalidOption, "There was an error loading the gemspec at #{gemspecs.first}." unless spec
        gem spec.name, :path => path
        group(development_group) do
          spec.development_dependencies.each do |dep|
            gem dep.name, *(dep.requirement.as_list + [:type => :development])
          end
        end
      when 0
        raise InvalidOption, "There are no gemspecs at #{expanded_path}."
      else
        raise InvalidOption, "There are multiple gemspecs at #{expanded_path}. Please use the :name option to specify which one."
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
      Definition.new(lockfile, @dependencies, @sources, unlock, @ruby_version)
    end

    def group(*args, &blk)
      @groups.concat args
      yield
    ensure
      args.each { @groups.pop }
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
      location = caller[0].split(':')[0..1].join(':')
      raise GemfileError, "Undefined local variable or method `#{name}' for Gemfile\n" \
        "        from #{location}"
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
      @valid_keys ||= %w(group groups git path name branch ref tag require submodules platform platforms type source)
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

      invalid_keys = opts.keys - (valid_keys + git_names)
      if invalid_keys.any?
        message = "You passed #{invalid_keys.map{|k| ':'+k }.join(", ")} "
        message << if invalid_keys.size > 1
                     "as options for gem '#{name}', but they are invalid."
                   else
                     "as an option for gem '#{name}', but it is invalid."
                   end

        message << " Valid options are: #{valid_keys.join(", ")}"
        raise InvalidOption, message
      end

      groups = @groups.dup
      opts["group"] = opts.delete("groups") || opts["group"]
      groups.concat Array(opts.delete("group"))
      groups = [:default] if groups.empty?

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

      opts["source"]  ||= @source
      opts["env"]     ||= @env
      opts["platforms"] = platforms.dup
      opts["group"]     = groups
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

  end
end
