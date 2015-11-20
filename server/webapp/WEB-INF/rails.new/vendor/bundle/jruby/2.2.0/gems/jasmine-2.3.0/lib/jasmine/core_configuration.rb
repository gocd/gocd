module Jasmine
  class CoreConfiguration
    def initialize(core = Jasmine::Core)
      @core = core
    end

    def path
      @core.path
    end

    def boot_dir
      @core.boot_dir
    end

    def boot_files
      @core.boot_files
    end

    def js_files
      @core.js_files
    end

    def css_files
      @core.css_files
    end

    def images_dir
      @core.images_dir
    end
  end
end
