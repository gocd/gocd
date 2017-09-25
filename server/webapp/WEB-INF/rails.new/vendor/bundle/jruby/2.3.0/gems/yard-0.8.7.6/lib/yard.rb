require File.expand_path('../yard/version.rb', __FILE__)

module YARD
  # The root path for YARD source libraries
  ROOT = File.expand_path(File.dirname(__FILE__))

  # The root path for YARD builtin templates
  TEMPLATE_ROOT = File.join(ROOT, '..', 'templates')

  # @deprecated Use {Config::CONFIG_DIR}
  CONFIG_DIR = File.expand_path('~/.yard')

  # An alias to {Parser::SourceParser}'s parsing method
  #
  # @example Parse a glob of files
  #   YARD.parse('lib/**/*.rb')
  # @see Parser::SourceParser.parse
  def self.parse(*args) Parser::SourceParser.parse(*args) end

  # An alias to {Parser::SourceParser}'s parsing method
  #
  # @example Parse a string of input
  #   YARD.parse_string('class Foo; end')
  # @see Parser::SourceParser.parse_string
  def self.parse_string(*args) Parser::SourceParser.parse_string(*args) end

  # (see YARD::Config.load_plugins)
  # @deprecated Use {Config.load_plugins}
  def self.load_plugins; YARD::Config.load_plugins end

  # @return [Boolean] whether YARD is being run inside of Windows
  def self.windows?
    return @windows if defined? @windows
    require 'rbconfig'
    if ::RbConfig::CONFIG['host_os'] =~ /mingw|win32|cygwin/
      @windows = true
    else
      @windows = false
    end
  ensure
    @windows ||= false
  end

  # @return [Boolean] whether YARD is being run in Ruby 1.8 mode
  def self.ruby18?; !ruby19? end

  # @return [Boolean] whether YARD is being run in Ruby 1.9 mode
  def self.ruby19?; @ruby19 ||= (RUBY_VERSION >= "1.9.1") end

  # @return [Boolean] whether YARD is being run in Ruby 2.0
  def self.ruby2?; @ruby2 ||= (RUBY_VERSION >= '2.0.0') end
end

# Keep track of Ruby version for compatibility code
# @deprecated Use {YARD.ruby18?} or {YARD.ruby19?} instead.
RUBY18, RUBY19 = YARD.ruby18?, YARD.ruby19?

# Load Ruby core extension classes
Dir.glob(File.join(YARD::ROOT, 'yard', 'core_ext', '*.rb')).each do |file|
  require file
end

# Backport RubyGems SourceIndex and other classes
require File.join(YARD::ROOT, 'yard', 'rubygems', 'backports')

['autoload', 'globals'].each do |file|
  require File.join(YARD::ROOT, 'yard', file)
end

# Load YARD configuration options (and plugins)
YARD::Config.load
