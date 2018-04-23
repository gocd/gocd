#--
# Copyright (c) 2005-2017 David Heinemeier Hansson
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#++

require "securerandom"
require "active_support/dependencies/autoload"
require "active_support/version"
require "active_support/logger"
require "active_support/lazy_load_hooks"
require "active_support/core_ext/date_and_time/compatibility"

module ActiveSupport
  extend ActiveSupport::Autoload

  autoload :Concern
  autoload :Dependencies
  autoload :DescendantsTracker
  autoload :ExecutionWrapper
  autoload :Executor
  autoload :FileUpdateChecker
  autoload :EventedFileUpdateChecker
  autoload :LogSubscriber
  autoload :Notifications
  autoload :Reloader

  eager_autoload do
    autoload :BacktraceCleaner
    autoload :ProxyObject
    autoload :Benchmarkable
    autoload :Cache
    autoload :Callbacks
    autoload :Configurable
    autoload :Deprecation
    autoload :Gzip
    autoload :Inflector
    autoload :JSON
    autoload :KeyGenerator
    autoload :MessageEncryptor
    autoload :MessageVerifier
    autoload :Multibyte
    autoload :NumberHelper
    autoload :OptionMerger
    autoload :OrderedHash
    autoload :OrderedOptions
    autoload :StringInquirer
    autoload :TaggedLogging
    autoload :XmlMini
    autoload :ArrayInquirer
  end

  autoload :Rescuable
  autoload :SafeBuffer, "active_support/core_ext/string/output_safety"
  autoload :TestCase

  def self.eager_load!
    super

    NumberHelper.eager_load!
  end

  cattr_accessor :test_order # :nodoc:

  def self.halt_callback_chains_on_return_false
    ActiveSupport::Deprecation.warn(<<-MSG.squish)
      ActiveSupport.halt_callback_chains_on_return_false is deprecated and will be removed in Rails 5.2.
    MSG
  end

  def self.halt_callback_chains_on_return_false=(value)
    ActiveSupport::Deprecation.warn(<<-MSG.squish)
      ActiveSupport.halt_callback_chains_on_return_false= is deprecated and will be removed in Rails 5.2.
    MSG
  end

  def self.to_time_preserves_timezone
    DateAndTime::Compatibility.preserve_timezone
  end

  def self.to_time_preserves_timezone=(value)
    DateAndTime::Compatibility.preserve_timezone = value
  end
end

autoload :I18n, "active_support/i18n"
