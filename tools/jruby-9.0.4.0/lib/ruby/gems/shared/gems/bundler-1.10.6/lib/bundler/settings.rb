require 'uri'

module Bundler
  class Settings
    BOOL_KEYS = %w(frozen cache_all no_prune disable_local_branch_check ignore_messages gem.mit gem.coc).freeze
    NUMBER_KEYS = %w(retry timeout redirect).freeze
    DEFAULT_CONFIG = {:retry => 3, :timeout => 10, :redirect => 5}

    def initialize(root = nil)
      @root          = root
      @local_config  = load_config(local_config_file)
      @global_config = load_config(global_config_file)
    end

    def [](name)
      key = key_for(name)
      value = (@local_config[key] || ENV[key] || @global_config[key] || DEFAULT_CONFIG[name])

      case
      when !value.nil? && is_bool(name)
        to_bool(value)
      when !value.nil? && is_num(name)
        value.to_i
      else
        value
      end
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

    def mirror_for(uri)
      uri = URI(uri.to_s) unless uri.is_a?(URI)

      # Settings keys are all downcased
      normalized_key = normalize_uri(uri.to_s.downcase)
      gem_mirrors[normalized_key] || uri
    end

    def credentials_for(uri)
      self[uri.to_s] || self[uri.host]
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
      locations[:default] = DEFAULT_CONFIG[key] if DEFAULT_CONFIG.key?(key)
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
      set_array(:without, array)
    end

    def with=(array)
      set_array(:with, array)
    end

    def without
      get_array(:without)
    end

    def with
      get_array(:with)
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

    def app_cache_path
      @app_cache_path ||= begin
        path = self[:cache_path] || "vendor/cache"
        raise InvalidOption, "Cache path must be relative to the bundle path" if path.start_with?("/")
        path
      end
    end

  private
    def key_for(key)
      if key.is_a?(String) && /https?:/ =~ key
        key = normalize_uri(key).to_s
      end
      key = key.to_s.gsub(".", "__").upcase
      "BUNDLE_#{key}"
    end

    def parent_setting_for(name)
      split_specfic_setting_for(name)[0]
    end

    def specfic_gem_for(name)
      split_specfic_setting_for(name)[1]
    end

    def split_specfic_setting_for(name)
      name.split(".")
    end

    def is_bool(name)
      BOOL_KEYS.include?(name.to_s) || BOOL_KEYS.include?(parent_setting_for(name.to_s))
    end

    def to_bool(value)
      !(value.nil? || value == '' || value =~ /^(false|f|no|n|0)$/i || value == false)
    end

    def is_num(value)
      NUMBER_KEYS.include?(value.to_s)
    end

    def get_array(key)
      self[key] ? self[key].split(":").map { |w| w.to_sym } : []
    end

    def set_array(key, array)
     self[key] = (array.empty? ? nil : array.join(":")) if array
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
        config_regex = /^(BUNDLE_.+): (['"]?)(.*(?:\n(?!BUNDLE).+)?)\2$/
        config_pairs = config_file.read.scan(config_regex).map do |m|
          key, _, value = m
          [convert_to_backward_compatible_key(key), value.gsub(/\s+/, " ").tr('"', "'")]
        end
        Hash[config_pairs]
      else
        {}
      end
    end

    def convert_to_backward_compatible_key(key)
      key = "#{key}/" if key =~ /https?:/i && key !~ %r[/\Z]
      key = key.gsub(".", "__") if key.include?(".")
      key
    end

    # TODO: duplicates Rubygems#normalize_uri
    # TODO: is this the correct place to validate mirror URIs?
    def normalize_uri(uri)
      uri = uri.to_s
      uri = "#{uri}/" unless uri =~ %r[/\Z]
      uri = URI(uri)
      unless uri.absolute?
        raise ArgumentError, "Gem sources must be absolute. You provided '#{uri}'."
      end
      uri
    end

  end
end
