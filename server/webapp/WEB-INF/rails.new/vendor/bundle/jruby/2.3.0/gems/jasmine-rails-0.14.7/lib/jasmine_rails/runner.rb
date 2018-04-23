require 'jasmine_rails/offline_asset_paths'

module JasmineRails
  module Runner
    class << self
      # Run the Jasmine testsuite via phantomjs CLI
      # raises an exception if any errors are encountered while running the testsuite
      def run(spec_filter = nil, reporters = 'console')
        override_rails_config do
          require 'phantomjs' if JasmineRails.use_phantom_gem?
          require 'fileutils'

          prepend_offline_asset_paths_helper
          html = get_spec_runner(spec_filter, reporters)
          FileUtils.mkdir_p JasmineRails.tmp_dir
          runner_path = JasmineRails.tmp_dir.join('runner.html')
          asset_prefix = Rails.configuration.assets.prefix.gsub(/\A\//,'')
          File.open(runner_path, 'w') {|f| f << html.gsub("/#{asset_prefix}", "./#{asset_prefix}")}

          phantomjs_runner_path = File.join(File.dirname(__FILE__), '..', 'assets', 'javascripts', 'jasmine-runner.js')
          phantomjs_cmd = JasmineRails.use_phantom_gem? ? Phantomjs.path : 'phantomjs'
          phantomjs_opts = JasmineRails.phantom_options.map { |option| "#{option}" }.join(' ')
          run_cmd %{"#{phantomjs_cmd}" #{phantomjs_opts} "#{phantomjs_runner_path}" "file://#{runner_path.to_s}?root=#{Rails.root.to_s}&spec=#{spec_filter}"}
        end
      end

      private
      def prepend_offline_asset_paths_helper
        action_view = Rails::VERSION::MAJOR >= 4 ? ActionView::Base : ActionView::AssetPaths
        module_extender = Gem.ruby_version >= Gem::Version.new("2") ? :prepend : :include
        action_view.send module_extender, JasmineRails::OfflineAssetPaths
      end

      # temporarily override internal rails settings for the given block
      # and reset the settings after work is complete.
      #
      # * disable Rails assets debug setting to ensure generated application
      # is built into one JS file
      # * disable asset host so that generated runner.html file uses
      # relative paths to included javascript files
      def override_rails_config
        config = Rails.application.config

        original_assets_debug = config.assets.debug
        original_assets_host = ActionController::Base.asset_host
        config.assets.debug = false
        ActionController::Base.asset_host = nil
        yield
      ensure
        config.assets.debug = original_assets_debug
        ActionController::Base.asset_host = original_assets_host
      end

      def get_spec_runner(spec_filter, reporters)
        app = ActionDispatch::Integration::Session.new(Rails.application)
        app.https!(JasmineRails.force_ssl)
        path = JasmineRails.route_path
        JasmineRails::OfflineAssetPaths.disabled = false

        if Rails::VERSION::MAJOR >= 5
          app.get path, :params => { :reporters => reporters, :spec => spec_filter }
        else
          app.get path, :reporters => reporters, :spec => spec_filter
        end

        JasmineRails::OfflineAssetPaths.disabled = true
        unless app.response.success?
          raise "Jasmine runner at '#{path}' returned a #{app.response.status} error: #{app.response.message} \n\n" +
                "The most common cause is an asset compilation failure. Full HTML response: \n\n #{app.response.body}"
        end
        app.response.body
      end

      def run_cmd(cmd)
        puts "Running `#{cmd}`"
        unless system(cmd)
          raise "Error executing command: #{cmd}"
        end
      end
    end
  end
end
