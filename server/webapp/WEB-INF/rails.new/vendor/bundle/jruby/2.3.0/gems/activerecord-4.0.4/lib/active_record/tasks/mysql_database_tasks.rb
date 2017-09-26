module ActiveRecord
  module Tasks # :nodoc:
    class MySQLDatabaseTasks # :nodoc:
      DEFAULT_CHARSET     = ENV['CHARSET']   || 'utf8'
      DEFAULT_COLLATION   = ENV['COLLATION'] || 'utf8_unicode_ci'
      ACCESS_DENIED_ERROR = 1045

      delegate :connection, :establish_connection, to: ActiveRecord::Base

      def initialize(configuration)
        @configuration = configuration
      end

      def create
        establish_connection configuration_without_database
        connection.create_database configuration['database'], creation_options
        establish_connection configuration
      rescue ActiveRecord::StatementInvalid => error
        if /database exists/ === error.message
          raise DatabaseAlreadyExists
        else
          raise
        end
      rescue error_class => error
        if error.respond_to?(:errno) && error.errno == ACCESS_DENIED_ERROR
          $stdout.print error.error
          establish_connection root_configuration_without_database
          connection.create_database configuration['database'], creation_options
          if configuration['username'] != 'root'
            connection.execute grant_statement.gsub(/\s+/, ' ').strip
          end
          establish_connection configuration
        else
          $stderr.puts "Couldn't create database for #{configuration.inspect}, #{creation_options.inspect}"
          $stderr.puts "(If you set the charset manually, make sure you have a matching collation)" if configuration['encoding']
        end
      end

      def drop
        establish_connection configuration
        connection.drop_database configuration['database']
      end

      def purge
        establish_connection :test
        connection.recreate_database configuration['database'], creation_options
      end

      def charset
        connection.charset
      end

      def collation
        connection.collation
      end

      def structure_dump(filename)
        args = prepare_command_options('mysqldump')
        args.concat(["--result-file", "#{filename}"])
        args.concat(["--no-data"])
        args.concat(["#{configuration['database']}"])
        unless Kernel.system(*args)
          $stderr.puts "Could not dump the database structure. "\
                       "Make sure `mysqldump` is in your PATH and check the command output for warnings."
        end
      end

      def structure_load(filename)
        args = prepare_command_options('mysql')
        args.concat(['--execute', %{SET FOREIGN_KEY_CHECKS = 0; SOURCE #{filename}; SET FOREIGN_KEY_CHECKS = 1}])
        args.concat(["--database", "#{configuration['database']}"])
        Kernel.system(*args)
      end

      private

      def configuration
        @configuration
      end

      def configuration_without_database
        configuration.merge('database' => nil)
      end

      def creation_options
        Hash.new.tap do |options|
          options[:charset]     = configuration['encoding']   if configuration.include? 'encoding'
          options[:collation]   = configuration['collation']  if configuration.include? 'collation'

          # Set default charset only when collation isn't set.
          options[:charset]   ||= DEFAULT_CHARSET unless options[:collation]

          # Set default collation only when charset is also default.
          options[:collation] ||= DEFAULT_COLLATION if options[:charset] == DEFAULT_CHARSET
        end
      end

      def error_class
        if configuration['adapter'] =~ /jdbc/
          require 'active_record/railties/jdbcmysql_error'
          ArJdbcMySQL::Error
        elsif defined?(Mysql2)
          Mysql2::Error
        elsif defined?(Mysql)
          Mysql::Error
        else
          StandardError
        end
      end

      def grant_statement
        <<-SQL
GRANT ALL PRIVILEGES ON #{configuration['database']}.*
  TO '#{configuration['username']}'@'localhost'
IDENTIFIED BY '#{configuration['password']}' WITH GRANT OPTION;
        SQL
      end

      def root_configuration_without_database
        configuration_without_database.merge(
          'username' => 'root',
          'password' => root_password
        )
      end

      def root_password
        $stdout.print "Please provide the root password for your mysql installation\n>"
        $stdin.gets.strip
      end

      def prepare_command_options(command)
        args = [command]
        args.concat(['--user', configuration['username']]) if configuration['username']
        args << "--password=#{configuration['password']}"  if configuration['password']
        args.concat(['--default-character-set', configuration['encoding']]) if configuration['encoding']
        configuration.slice('host', 'port', 'socket').each do |k, v|
          args.concat([ "--#{k}", v.to_s ]) if v
        end

        args
      end
    end
  end
end
