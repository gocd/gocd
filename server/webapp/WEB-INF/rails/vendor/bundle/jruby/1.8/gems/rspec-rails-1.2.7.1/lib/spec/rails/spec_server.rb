require 'spec/deprecation'
Spec.deprecate('spec_server', 'spork (gem install spork)')

if Rails::VERSION::STRING >= '2.2' && Rails.configuration.cache_classes
  raise <<-MESSAGE

#{'*'*65}

  Rails.configuration.cache_classes == true
  
  This means that spec_server won't reload your classes when
  you change them, which defeats the purpose of spec_server.
  
  Please set 'config.cache_classes = false' (it's probably
  set to true in config/environments/test.rb) and give it
  another try.

#{'*'*65}
MESSAGE
end

require 'drb/drb'
require 'rbconfig'

# This is based on Florian Weber's TDDMate
module Spec
  module Rails
    class SpecServer
      class << self
        def restart_test_server
          puts "restarting"
          config       = ::Config::CONFIG
          ruby         = File::join(config['bindir'], config['ruby_install_name']) + config['EXEEXT']
          command_line = [ruby, $0, ARGV].flatten.join(' ')
          exec(command_line)
        end

        def daemonize(pid_file = nil)
          return yield if $DEBUG
          pid = Process.fork{
            Process.setsid
            Dir.chdir(RAILS_ROOT)
            trap("SIGINT"){ exit! 0 }
            trap("SIGTERM"){ exit! 0 }
            trap("SIGHUP"){ restart_test_server }
            File.open("/dev/null"){|f|
              STDERR.reopen f
              STDIN.reopen  f
              STDOUT.reopen f
            }
            run
          }
          puts "spec_server launched (PID: %d)" % pid
          File.open(pid_file,"w"){|f| f.puts pid } if pid_file
          exit! 0
        end

        def run
          trap("USR2") { ::Spec::Rails::SpecServer.restart_test_server } if Signal.list.has_key?("USR2")
          DRb.start_service("druby://127.0.0.1:8989", ::Spec::Rails::SpecServer.new)
          DRb.thread.join
        end
      end
      
      def run(argv, stderr, stdout)
        $stdout = stdout
        $stderr = stderr
        
        ::Rails::Configuration.extend Module.new {def cache_classes; false; end}

        ::ActiveSupport.const_defined?(:Dependencies) ?
          ::ActiveSupport::Dependencies.mechanism = :load :
          ::Dependencies.mechanism = :load
        
        require 'action_controller/dispatcher'
        dispatcher = ::ActionController::Dispatcher.new($stdout)

        if ::ActionController::Dispatcher.respond_to?(:reload_application)
          ::ActionController::Dispatcher.reload_application
        else
          dispatcher.reload_application
        end
        
        if Object.const_defined?(:Fixtures) && Fixtures.respond_to?(:reset_cache)
          Fixtures.reset_cache
        end

        unless Object.const_defined?(:ApplicationController)
          %w(application_controller.rb application.rb).each do |name|
            require_dependency(name) if File.exists?("#{RAILS_ROOT}/app/controllers/#{name}")
          end
        end
        load "#{RAILS_ROOT}/spec/spec_helper.rb"

        if in_memory_database?
          load "#{RAILS_ROOT}/db/schema.rb"
          ActiveRecord::Migrator.up('db/migrate')
        end
        
        ::Spec::Runner::CommandLine.run(
          ::Spec::Runner::OptionParser.parse(
            argv,
            $stderr,
            $stdout
          )
        )

        if ::ActionController::Dispatcher.respond_to?(:cleanup_application)
          ::ActionController::Dispatcher.cleanup_application
        else
          dispatcher.cleanup_application
        end
        
      end

      def in_memory_database?
        ENV["RAILS_ENV"] == "test" and
        ::ActiveRecord::Base.connection.class.to_s == "ActiveRecord::ConnectionAdapters::SQLite3Adapter" and
        ::Rails::Configuration.new.database_configuration['test']['database'] == ':memory:'
      end
    end
  end
end

options = Hash.new
parser = OptionParser.new
parser.on("-d", "--daemon")     {|ignore| options[:daemon] = true }
parser.on("-p", "--pid PIDFILE"){|pid|    options[:pid]    = pid  }
parser.parse!(ARGV)

if options[:daemon]
  ::Spec::Rails::SpecServer.daemonize(options[:pid])
else
  ::Spec::Rails::SpecServer.run
end
