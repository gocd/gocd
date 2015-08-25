module Foundation
  module Rails
    class Engine < ::Rails::Engine
      isolate_namespace Foundation::Rails
      initializer "foundation-rails.assets.precompile" do |app|
        app.config.assets.precompile += %w( vendor/modernizr.js )
      end
    end
  end
end
