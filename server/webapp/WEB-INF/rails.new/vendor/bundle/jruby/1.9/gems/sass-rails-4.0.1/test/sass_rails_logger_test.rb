require 'test_helper'

class SassRailsLoggerTest < Sass::Rails::TestCase
  test "setting a sass-rails logger as the sass default logger" do
    within_rails_app "scss_project" do
      logger_class_name = runcmd 'ruby script/rails runner "print Sass::logger.class.name"'
      assert logger_class_name =~ /#{Regexp.escape(Sass::Rails::Logger.name)}/
    end
  end

  test "sending a log messages to the sass logger writes to the environment log file" do
    within_rails_app "scss_project" do |app_root|
      [:debug, :warn, :info, :error, :trace].each do |level|
        message = "[#{level}]: sass message"
        runcmd %{ruby script/rails runner "Sass::logger.log_level = :#{level}; Sass::logger.log(:#{level}, %Q|#{message}|)"}, Dir.pwd, true, 'Gemfile', { 'RAILS_ENV' => 'development' }

        assert File.exists?("#{app_root}/log/development.log"), "log file was not created"

        log_output = File.open("#{app_root}/log/development.log").read
        assert log_output.include?(message), "the #{level} log message was not found in the log file"
      end
    end
  end
end
