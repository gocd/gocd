module Bundler
  class Settings
    def initialize(root = nil)
      @root          = root
      @local_config  = load_config(local_config_file)
      @global_config = load_config(global_config_file)
    end

    def [](key)
      the_key = key_for(key)
      value = (@local_config[the_key] || ENV[the_key] || @global_config[the_key])
      is_bool(key) ? to_bool(value) : value
    end

    def []=(key, value)
      local_config_file or raise GemfileNotFound, "Could not locate Gemfile"
      set_key(key, value, @local_config, local_config_file)
    end

    alias :set_local :[]=

    def delete(key)
      @local_config.delete(key_for(key))
    end

    def set_global(key, value)
      set_key(key, value, @global_config, global_config_file)
    end

    def all
      env_keys = ENV.keys.select { |k| k =~ /BUNDLE_.*/ }

      keys = @global_config.keys | @local_config.keys | env_keys

      keys.map do |key|
        key.sub(/^BUNDLE_/, '').gsub(/__/, ".").downcase
      end
    end

    def local_overrides
      repos = {}
      all.each do |k|
        if k =~ /^local\./
          repos[$'] = self[k]
        end
      end
      repos
    end

    def gem_mirrors
      all.inject({}) do |h, k|
        if k =~ /^mirror\./
          uri = normalize_uri($')
          h[uri] = normalize_uri(self[k])
        end
        h
      end
    end

    def locations(key)
      key = key_for(key)
      locations = {}
      locations[:local]  = @local_config[key] if @local_config.key?(key)
      locations[:env]    = ENV[key] if ENV[key]
      locations[:global] = @global_config[key] if @global_config.key?(key)
      locations
    end

    def pretty_values_for(exposed_key)
      key = key_for(exposed_key)

      locations = []
      if @local_config.key?(key)
        locations << "Set for your local app (#{local_config_file}): #{@local_config[key].inspect}"
      end

      if value = ENV[key]
        locations << "Set via #{key}: #{value.inspect}"
      end

      if @global_config.key?(key)
        locations << "Set for the current user (#{global_config_file}): #{@global_config[key].inspect}"
      end

      return ["You have not configured a value for `#{exposed_key}`"] if locations.empty?
      locations
    end

    def without=(array)
      self[:without] = (array.empty? ? nil : array.join(":")) if array
    end

    def without
      self[:without] ? self[:without].split(":").map { |w| w.to_sym } : []
    end

    # @local_config["BUNDLE_PATH"] should be prioritized over ENV["BUNDLE_PATH"]
    def path
      key  = key_for(:path)
      path = ENV[key] || @global_config[key]
      return path if path && !@local_config.key?(key)

      if path = self[:path]
        "#{path}/#{Bundler.ruby_scope}"
      else
        Bundler.rubygems.gem_dir
      end
    end

    def allow_sudo?
      !@local_config.key?(key_for(:path))
    end

    def ignore_config?
      ENV['BUNDLE_IGNORE_CONFIG']
    end

  private
    def key_for(key)
      key = key.to_s.sub(".", "__").upcase
      "BUNDLE_#{key}"
    end

    def is_bool(key)
      %w(frozen cache_all no_prune disable_local_branch_check).include? key.to_s
    end

    def to_bool(value)
      !(value.nil? || value == '' || value =~ /^(false|f|no|n|0)$/i)
    end

    def set_key(key, value, hash, file)
      key = key_for(key)

      unless hash[key] == value
        hash[key] = value
        hash.delete(key) if value.nil?
        FileUtils.mkdir_p(file.dirname)
        require 'bundler/psyched_yaml'
        File.open(file, "w") { |f| f.puts YAML.dump(hash) }
      end
      value
    end

    def global_config_file
      file = ENV["BUNDLE_CONFIG"] || File.join(Bundler.rubygems.user_home, ".bundle/config")
      Pathname.new(file)
    end

    def local_config_file
      Pathname.new(@root).join("config") if @root
    end

    def load_config(config_file)
      valid_file = config_file && config_file.exist? && !config_file.size.zero?
      if !ignore_config? && valid_file
        config_regex =/^(BUNDLE_.+): (?:['"](.*)['"]|(.+(?:\n(?!BUNDLE).+))|(.+))$/
        config_pairs = config_file.read.scan(config_regex).map{|m| m.compact.map { |n| n.gsub(/\n\s?/, "") } }
        Hash[config_pairs]
      else
        {}
      end
    end

    # TODO: duplicates Rubygems#normalize_uri
    # TODO: is this the correct place to validate mirror URIs?
    def normalize_uri(uri)
      uri = uri.to_s
      uri = "#{uri}/" unless uri =~ %r[/\Z]
      uri = URI(uri)
      raise ArgumentError, "Gem mirror sources must be absolute URIs (configured: #{mirror_source})" unless uri.absolute?
      uri
    end

  end
end
