module MithrilRails
  module Rails
    class Railtie < ::Rails::Railtie
      # Watch .msx files for changes in dev, so we can reload the JS VMs with the new JS code.
      initializer "mithril_rails.add_watchable_files" do |app|
        app.config.watchable_files.concat Dir["#{app.root}/app/assets/javascripts/**/*.msx*"]
      end
    end
  end
end
