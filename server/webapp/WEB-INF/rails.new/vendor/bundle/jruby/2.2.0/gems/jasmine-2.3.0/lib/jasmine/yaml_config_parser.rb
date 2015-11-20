module Jasmine
  class YamlConfigParser
    def initialize(path, pwd, path_expander = lambda {}, yaml_loader = lambda {})
      @path = path
      @path_expander = path_expander
      @pwd = pwd
      @yaml_loader = yaml_loader
    end

    def src_dir
      return @pwd unless loaded_yaml['src_dir']
      File.join(@pwd, loaded_yaml['src_dir'])
    end

    def spec_dir
      return File.join(@pwd, 'spec', 'javascripts') unless loaded_yaml['spec_dir']
      File.join(@pwd, loaded_yaml['spec_dir'])
    end

    def jasmine_dir
      return nil unless loaded_yaml['jasmine_dir']
      File.join(@pwd, loaded_yaml['jasmine_dir'])
    end

    def boot_dir
      return nil unless loaded_yaml['boot_dir']
      File.join(@pwd, loaded_yaml['boot_dir'])
    end

    def src_files
      @path_expander.call(src_dir, loaded_yaml['src_files'] || [])
    end

    def jasmine_files
      @path_expander.call(jasmine_dir, loaded_yaml['jasmine_files'] || [])
    end

    def boot_files
      @path_expander.call(boot_dir, loaded_yaml['boot_files'] || [])
    end

    def jasmine_css_files
      @path_expander.call(jasmine_dir, loaded_yaml['jasmine_css_files'] || [])
    end

    def spec_files
      @path_expander.call(spec_dir, loaded_yaml['spec_files'] || [])
    end

    def helpers
      @path_expander.call(spec_dir, loaded_yaml['helpers'] || [])
    end

    def css_files
      @path_expander.call(src_dir, loaded_yaml['stylesheets'] || [])
    end

    def spec_helper
      File.join(@pwd, loaded_yaml['spec_helper'] || File.join('spec', 'javascripts', 'support', 'jasmine_helper.rb'))
    end

    def show_console_log
      loaded_yaml['show_console_log'] || false
    end

    def stop_spec_on_expectation_failure
      loaded_yaml['stop_spec_on_expectation_failure'] || false
    end

    def phantom_config_script
      return nil unless loaded_yaml['phantom_config_script']
      File.join @pwd, loaded_yaml['phantom_config_script']
    end

    def rack_options
      loaded_yaml.fetch('rack_options', {}).inject({}) do |memo, (key, value)|
        memo[key.to_sym] = value
        memo
      end
    end

    private
    def loaded_yaml
      @yaml_loader.call(@path)
    end
  end
end
