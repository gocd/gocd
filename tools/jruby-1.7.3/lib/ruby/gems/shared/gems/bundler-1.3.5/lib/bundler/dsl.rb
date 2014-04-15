require 'bundler/dependency'

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
      @rubygems_source = Source::Rubygems.new
      @source          = nil
      @sources         = []
      @dependencies    = []
      @groups          = []
      @platforms       = []
      @env             = nil
      @ruby_version    = nil
    end

    def eval_gemfile(gemfile, contents = nil)
      contents ||= Bundler.read_file(gemfile.to_s)
      instance_eval(contents, gemfile.to_s, 1)
    rescue SyntaxError => e
      bt = e.message.split("\n")[1..-1]
      raise GemfileError, ["Gemfile syntax error:", *bt].join("\n")
    rescue ScriptError, RegexpError, NameError, ArgumentError => e
      e.backtrace[0] = "#{e.backtrace[0]}: #{e.message} (#{e.class})"
      Bundler.ui.warn e.backtrace.join("\n       ")
      raise GemfileError, "There was an error in your Gemfile," \
        " and Bundler cannot continue."
    end

    def gemspec(opts = nil)
      path              = opts && opts[:path] || '.'
      name              = opts && opts[:name] || '{,*}'
      development_group = opts && opts[:development_group] || :development
      path              = File.expand_path(path, Bundler.default_gemfile.dirname)
      gemspecs = Dir[File.join(path, "#{name}.gemspec")]

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
        raise InvalidOption, "There are no gemspecs at #{path}."
      else
        raise InvalidOption, "There are multiple gemspecs at #{path}. Please use the :name option to specify which one."
      end
    end

    def gem(name, *args)
      if name.is_a?(Symbol)
        raise GemfileError, %{You need to specify gem names as Strings. Use 'gem "#{name.to_s}"' instead.}
      end

      options = Hash === args.last ? args.pop : {}
      version = args

      _normalize_options(name, version, options)

      dep = Dependency.new(name, version, options)

      # if there's already a dependency with this name we try to prefer one
      if current = @dependencies.find { |d| d.name == dep.name }
        if current.requirement != dep.requirement
          if current.type == :development
            @dependencies.delete current
          elsif dep.type == :development
            return
          else
            raise GemfileError, "You cannot specify the same gem twice with different version requirements. \n" \
                            "You specified: #{current.name} (#{current.requirement}) and " \
                            "#{dep.name} (#{dep.requirement})\n"
          end
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

    def source(source, options = {})
      case source
      when :gemcutter, :rubygems, :rubyforge then
        Bundler.ui.warn "The source :#{source} is deprecated because HTTP " \
          "requests are insecure.\nPlease change your source to 'https://" \
          "rubygems.org' if possible, or 'http://rubygems.org' if not."
        @rubygems_source.add_remote "http://rubygems.org"
        return
      when String
        @rubygems_source.add_remote source
        return
      else
        @source = source
        if options[:prepend]
          @sources = [@source] | @sources
        else
          @sources = @sources | [@source]
        end

        yield if block_given?
        return @source
      end
    ensure
      @source = nil
    end

    def path(path, options = {}, source_options = {}, &blk)
      source Source::Path.new(_normalize_hash(options).merge("path" => Pathname.new(path))), source_options, &blk
    end

    def git(uri, options = {}, source_options = {}, &blk)
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

      source Source::Git.new(_normalize_hash(options).merge("uri" => uri)), source_options, &blk
    end

    def to_definition(lockfile, unlock)
      @sources << @rubygems_source unless @sources.include?(@rubygems_source)
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

    def _normalize_hash(opts)
      # Cannot modify a hash during an iteration in 1.9
      opts.keys.each do |k|
        next if String === k
        v = opts[k]
        opts.delete(k)
        opts[k.to_s] = v
      end
      opts
    end

    def _normalize_options(name, version, opts)
      _normalize_hash(opts)

      valid_keys = %w(group groups git gist github path name branch ref tag require submodules platform platforms type)
      invalid_keys = opts.keys - valid_keys
      if invalid_keys.any?
        plural = invalid_keys.size > 1
        message = "You passed #{invalid_keys.map{|k| ':'+k }.join(", ")} "
        if plural
          message << "as options for gem '#{name}', but they are invalid."
        else
          message << "as an option for gem '#{name}', but it is invalid."
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

      if github = opts.delete("github")
        github = "#{github}/#{github}" unless github.include?("/")
        opts["git"] = "git://github.com/#{github}.git"
      end

      if gist = opts.delete("gist")
        opts["git"] = "https://gist.github.com/#{gist}.git"
      end

      ["git", "path"].each do |type|
        if param = opts[type]
          if version.first && version.first =~ /^\s*=?\s*(\d[^\s]*)\s*$/
            options = opts.merge("name" => name, "version" => $1)
          else
            options = opts.dup
          end
          source = send(type, param, options, :prepend => true) {}
          opts["source"] = source
        end
      end

      opts["source"]  ||= @source
      opts["env"]     ||= @env
      opts["platforms"] = platforms.dup
      opts["group"]     = groups
    end

  end
end
