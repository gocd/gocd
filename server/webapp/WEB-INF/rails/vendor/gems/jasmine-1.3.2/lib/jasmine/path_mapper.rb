module Jasmine
  class PathMapper
    def initialize(config)
      @config = config
    end

    def map_src_paths(paths)
      map(paths, @config.src_dir, @config.src_path)
    end

    def map_spec_paths(paths)
      map(paths, @config.spec_dir, @config.spec_path)
    end

    def map_boot_paths(paths)
      map(paths, @config.boot_dir, @config.boot_path)
    end

    def map_jasmine_paths(paths)
      map(paths, @config.jasmine_dir, @config.jasmine_path)
    end

    private
    def map(paths, remove_path, add_path)
      paths.map { |path| File.join(add_path, (path.gsub(remove_path, ''))) }
    end

  end
end
