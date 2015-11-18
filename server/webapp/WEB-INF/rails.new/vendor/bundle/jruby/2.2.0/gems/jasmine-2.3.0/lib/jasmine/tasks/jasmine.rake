if Rake.application.tasks.any? {|t| t.name == 'jasmine/ci' }
  message = <<-EOF

                        WARNING
Detected that jasmine rake tasks have been loaded twice.
This will cause the 'rake jasmine:ci' and 'rake jasmine' tasks to fail.

To fix this problem, you should ensure that you only load 'jasmine/tasks/jasmine.rake'
once. This should be done for you automatically if you installed jasmine's rake tasks
with either 'jasmine init' or 'rails g jasmine:install'.


EOF
  raise Exception.new(message)
end

namespace :jasmine do
  task :configure do
    require 'jasmine/config'

    begin
      Jasmine.load_configuration_from_yaml(ENV['JASMINE_CONFIG_PATH'])
    rescue Jasmine::ConfigNotFound => e
      puts e.message
      exit 1
    end
  end

  task :require do
    require 'jasmine'
  end

  task :require_json do
    begin
      require 'json'
    rescue LoadError
      puts "You must have a JSON library installed to run jasmine:ci. Try \"gem install json\""
      exit
    end
  end

  task :configure_plugins

  desc 'Run continuous integration tests'
  task :ci => %w(jasmine:require_json jasmine:require jasmine:configure jasmine:configure_plugins) do
    ci_runner = Jasmine::CiRunner.new(Jasmine.config)
    exit(1) unless ci_runner.run
  end

  task :server => %w(jasmine:require jasmine:configure jasmine:configure_plugins) do
    config = Jasmine.config
    port = config.port(:server)
    server = Jasmine::Server.new(port, Jasmine::Application.app(Jasmine.config), config.rack_options)
    puts "your server is running here: http://localhost:#{port}/"
    puts "your tests are here:         #{config.spec_dir}"
    puts "your source files are here:  #{config.src_dir}"
    puts ''
    server.start
  end
end

desc 'Run specs via server:ci'
task :jasmine => %w(jasmine:server)
