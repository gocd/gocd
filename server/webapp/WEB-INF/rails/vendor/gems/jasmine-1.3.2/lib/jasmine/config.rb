module Jasmine
  require 'yaml'
  require 'erb'
  def self.configure(&block)
    block.call(self.config)
  end

  def self.initialize_config
    return if @config
    @config = Jasmine::Configuration.new
    core_config = Jasmine::CoreConfiguration.new

    @config.add_path_mapper(Jasmine::PathMapper.method(:new))

    @config.jasmine_path = jasmine_path = "/__jasmine__"
    @config.src_path = src_path = "/"
    @config.spec_path = spec_path = "/__spec__"
    @config.boot_path = boot_path = "/__boot__"

    @config.jasmine_dir = core_config.path
    @config.boot_dir = core_config.boot_path
    @config.boot_files = lambda { core_config.boot_files }
    @config.jasmine_files = lambda { core_config.js_files }
    @config.jasmine_css_files = lambda { core_config.css_files }
    @config.add_rack_path(jasmine_path, lambda { Rack::File.new(config.jasmine_dir) })
    @config.add_rack_path(boot_path, lambda { Rack::File.new(config.boot_dir) })
    @config.add_rack_path(spec_path, lambda { Rack::File.new(config.spec_dir) })
    @config.add_rack_path(src_path, lambda {
      Rack::Cascade.new([
        Rack::URLMap.new('/' => Rack::File.new(config.src_dir)),
        Rack::Jasmine::Runner.new(Jasmine::Page.new(config))
      ])
    })

    @config.add_rack_app(Rack::Head)
    @config.add_rack_app(Rack::Jasmine::CacheControl)

    if Jasmine::Dependencies.rails_3_asset_pipeline?
      @config.add_path_mapper(lambda { |config|
        asset_expander = Jasmine::AssetExpander.new(
          Jasmine::AssetPipelineUtility.method(:bundled_asset_factory),
          Jasmine::AssetPipelineUtility.method(:asset_path_for)
        )
        Jasmine::AssetPipelineMapper.new(config, asset_expander.method(:expand))
      })
      @config.add_rack_path('/assets', lambda {
        # In order to have asset helpers like asset_path and image_path, we need to require 'action_view/base'.  This
        # triggers run_load_hooks on action_view which, in turn, causes sprockets/railtie to load the Sprockets asset
        # helpers.  Alternatively, you can include the helpers yourself without loading action_view/base:
        Rails.application.assets.context_class.instance_eval do
          include ::Sprockets::Helpers::IsolatedHelper
          include ::Sprockets::Helpers::RailsHelper
        end
        Rails.application.assets
      })
    end

  end

  def self.config
    initialize_config
    @config
  end

  def self.load_configuration_from_yaml(path = nil)
    path ||= File.join(Dir.pwd, 'spec', 'javascripts', 'support', 'jasmine.yml')
    if File.exist?(path)
      yaml_loader = lambda do |filepath|
        YAML::load(ERB.new(File.read(filepath)).result(binding)) if File.exist?(filepath)
      end
      yaml_config = Jasmine::YamlConfigParser.new(path, Dir.pwd, Jasmine::PathExpander.method(:expand), yaml_loader)
      Jasmine.configure do |config|
        config.jasmine_dir = yaml_config.jasmine_dir if yaml_config.jasmine_dir
        config.jasmine_files = lambda { yaml_config.jasmine_files } if yaml_config.jasmine_files.any?
        config.jasmine_css_files = lambda { yaml_config.jasmine_css_files } if yaml_config.jasmine_css_files.any?
        config.src_files = lambda { yaml_config.src_files }
        config.spec_files = lambda { yaml_config.helpers + yaml_config.spec_files }
        config.css_files = lambda { yaml_config.css_files }
        config.src_dir = yaml_config.src_dir
        config.spec_dir = yaml_config.spec_dir
      end
      require yaml_config.spec_helper if File.exist?(yaml_config.spec_helper)
    end
  end

end
