module Jasmine
  class CoreConfiguration
    def initialize(core = Jasmine::Core)
      @core = core
    end

    def path
      @core.path
    end

    #TODO: maybe this doesn't belong in CoreConfig
    def boot_path
      Jasmine.root('javascripts')
    end

    def boot_files
      Dir.glob(File.join(boot_path, "**"))
    end

    def js_files
      @core.js_files
    end

    def css_files
      @core.css_files
    end
  end
end
