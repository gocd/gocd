module Webpacker
  class Runner
    def self.run(argv)
      $stdout.sync = true

      new(argv).run
    end

    def initialize(argv)
      @argv = argv

      @app_path          = File.expand_path(".", Dir.pwd)
      @node_modules_path = File.join(@app_path, "node_modules")
      @webpack_config    = File.join(@app_path, "config/webpack/#{ENV["NODE_ENV"]}.js")

      unless File.exist?(@webpack_config)
        puts "Webpack config #{@webpack_config} not found, please run 'bundle exec rails webpacker:install' to install webpacker with default configs or add the missing config file for your custom environment."
        exit!
      end
    end
  end
end
