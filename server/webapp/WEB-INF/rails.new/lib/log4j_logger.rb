##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

class Log4jLogger
  # SLF4J severity levels
  LEVELS = %w{ trace debug info warn error }

  RUBY_LEVELS = {
    ::Logger::Severity::FATAL => 'fatal',
    ::Logger::Severity::ERROR => 'error',
    ::Logger::Severity::WARN  => 'warn',
    ::Logger::Severity::INFO  => 'info',
    ::Logger::Severity::DEBUG => 'debug'
  }

  # Logger compatible facade over org.slf4j.Logger
  #
  # === Generated Methods
  #
  # Corresponding methods are generated for each of the SLF4J levels:
  #
  # * trace
  # * debug
  # * info
  # * warn
  # * error
  # * fatal (alias to error)
  #
  # These have the form (using _info_ as example):
  #
  #   log = Logger.new("name")
  #   log.info?                  # Is this level enabled for logging?
  #   log.info("message")        # Log message
  #   log.info { "message" }     # Execute block if enabled and log returned value
  #   log.info("message", ex)    # Log message with exception message/stack trace
  #   log.info(ex) { "message" } # Log message with exception message/stack trace
  #   log.info(ex)               # Log exception with default "Exception:" message
  #
  # Note that the exception variants are aware of JRuby's
  # NativeException class (a wrapped java exception) and will log
  # using the Java ex.cause in this case.
  #
  class Logger
    attr_accessor :level
    attr_reader :name

    # Create new or find existing Logger by name. If name is a Module (Class, etc.)
    # then use SLF4J.to_log_name(name) as the name
    #
    # Note that loggers are arranged in a hiearchy by dot '.' name
    # notation using java package/class name conventions:
    #
    # * "pmodule"
    # * "pmodule.cmodule."
    # * "pmodule.cmodule.ClassName"
    #
    # Which enables hierarchical level setting and abbreviation in some output adapters.
    #
    def initialize(name='com.thoughtworks.go.server.Rails')
      @name = name.is_a?(Module) ? SLF4J.to_log_name(name) : name
      @logger = org.slf4j.LoggerFactory.getLogger(@name)
    end

    def level
      @logger.level
    end

    def level=(lvl)
      lvl_string = (RUBY_LEVELS[lvl] || 'trace').upcase
      @logger.level = Java::ch.qos.logback.classic.Level.const_get(lvl_string)
    end

    # Return underlying org.slf4j.Logger
    def java_logger
      @logger
    end

    # Define logging methods for each level: debug(), error(), etc.
    LEVELS.each do |lvl|
      module_eval( %Q{
          def #{lvl}?
            @logger.is#{lvl.capitalize}Enabled
          end
          def #{lvl}( msg=nil, ex=nil )
            if msg.is_a?( Exception ) && ex.nil?
              msg, ex = "Exception:", msg
            end
            msg = yield if ( block_given? && #{lvl}? )
            if msg
              if ex
                #{lvl}_ex( msg, ex )
              else
                @logger.#{lvl}( msg.to_s )
              end
            end
          end
          def #{lvl}_ex( msg, ex )
            if ex.is_a?( NativeException )
              @logger.#{lvl}( msg.to_s, ex.cause )
            elsif #{lvl}?
              log = msg.to_s.dup
              log << '\n'
              log << ex.class.name << ': ' << ex.message << '\n'
              ex.backtrace.each do |b|
                log << '\t' << b << '\n'
              end
              @logger.#{lvl}( log )
            end
          end
        } )
    end

    # Alias fatal to error for Logger compatibility
    alias_method :fatal, :error
    alias_method :fatal?, :error?

    def <<(msg)
      add @level, msg
    end

    def add(severity, message = nil, progname = nil)
      level = (RUBY_LEVELS[severity] || severity || 'trace').downcase
      level = 'trace' unless self.respond_to?("#{level}?".to_sym)

      if self.send("#{level}?".to_sym)
        if message.nil?
          if block_given?
            message = yield
          end
        end
        msg = [progname, message].reject { |f| f.nil? }
        self.send(level.to_sym, msg.join(' - ')) unless msg.empty?
      end

      return true
    end

    alias_method :unknown, :warn

    # aliased for Logger compatibility
    alias_method :log, :add
  end

end
