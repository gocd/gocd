require 'rake'
require 'rake/sprocketstask'
require 'sprockets'

module Sprockets
  module Rails
    class Task < Rake::SprocketsTask
      attr_accessor :app

      def initialize(app = nil)
        self.app = app
        super()
      end

      def environment
        if app
          app.assets
        else
          super
        end
      end

      def output
        if app
          File.join(app.root, 'public', app.config.assets.prefix)
        else
          super
        end
      end

      def assets
        if app
          app.config.assets.precompile
        else
          super
        end
      end

      def cache_path
        if app
          "#{app.config.root}/tmp/cache/assets"
        else
          @cache_path
        end
      end
      attr_writer :cache_path

      def define
        namespace :assets do
          # Override this task change the loaded dependencies
          desc "Load asset compile environment"
          task :environment do
            # Load full Rails environment by default
            Rake::Task['environment'].invoke
          end

          desc "Compile all the assets named in config.assets.precompile"
          task :precompile => :environment do
            with_logger do
              manifest.compile(assets)
            end
          end

          desc "Remove old compiled assets"
          task :clean, [:keep] => :environment do |t, args|
            keep = Integer(args.keep || 2)
            with_logger do
              manifest.clean(keep)
            end
          end

          desc "Remove compiled assets"
          task :clobber => :environment do
            with_logger do
              manifest.clobber
              rm_rf cache_path if cache_path
            end
          end
        end
      end
    end
  end
end
