module MithrilRails
  module Rails
    class Engine < ::Rails::Engine
      initializer "mithril_rails.setup_engine", :group => :all do |app|
        app.assets.register_engine '.msx', MithrilRails::MSX::Template
      end
    end
  end
end