require 'uglifier'
Go::Application.configure do
  # Settings specified here will take precedence over those in config/application.rb.

  # Code is not reloaded between requests.
  config.cache_classes = true

  # Eager load code on boot. This eager loads most of Rails and
  # your application in memory, allowing both thread web servers
  # and those relying on copy on write to perform better.
  # Rake tasks automatically ignore this option for performance.
  config.eager_load = true

  # Full error reports are disabled and caching is turned on.
  config.consider_all_requests_local = false
  config.action_controller.perform_caching = true

  # Enable Rack::Cache to put a simple HTTP cache in front of your application
  # Add `rack-cache` to your Gemfile before enabling this.
  # For large-scale production use, consider using a caching reverse proxy like nginx, varnish or squid.
  # config.action_dispatch.rack_cache = true

  # Disable Rails's static asset server (Apache or nginx will already do this).
  config.serve_static_assets = false

  # Compress JavaScripts and CSS.
  config.assets.js_compressor = Uglifier.new({
                                                 # rubocop:disable LineLength
                                                 :output => {
                                                     :ascii_only => true, # Escape non-ASCII characterss
                                                     :comments => :all, # Preserve comments (:all, :jsdoc, :copyright, :none)
                                                     :inline_script => false, # Escape occurrences of </script in strings
                                                     :quote_keys => false, # Quote keys in object literals
                                                     :max_line_len => 32 * 1024, # Maximum line length in minified code
                                                     :bracketize => true, # Bracketize if, for, do, while or with statements, even if their body is a single statement
                                                     :semicolons => true, # Separate statements with semicolons
                                                     :preserve_line => false, # Preserve line numbers in outputs
                                                     :beautify => false, # Beautify output
                                                     :indent_level => 4, # Indent level in spaces
                                                     :indent_start => 0, # Starting indent level
                                                     :space_colon => false, # Insert space before colons (only with beautifier)
                                                     :width => 80, # Specify line width when beautifier is used (only with beautifier)
                                                     :preamble => nil # Preamble for the generated JS file. Can be used to insert any code or comment.
                                                 },
                                                 :mangle => {
                                                     :eval => false, # Mangle names when eval of when is used in scope
                                                     :except => ["$super"], # Argument names to be excluded from mangling
                                                     :sort => false, # Assign shorter names to most frequently used variables. Often results in bigger output after gzip.
                                                     :toplevel => false # Mangle names declared in the toplevel scope
                                                 }, # Mangle variable and function names, set to false to skip mangling
                                                 :compress => {
                                                     :sequences => true, # Allow statements to be joined by commas
                                                     :properties => true, # Rewrite property access using the dot notation
                                                     :dead_code => true, # Remove unreachable code
                                                     :drop_debugger => true, # Remove debugger; statements
                                                     :unsafe => false, # Apply "unsafe" transformations
                                                     :conditionals => true, # Optimize for if-s and conditional expressions
                                                     :comparisons => true, # Apply binary node optimizations for comparisons
                                                     :evaluate => true, # Attempt to evaluate constant expressions
                                                     :booleans => true, # Various optimizations to boolean contexts
                                                     :loops => true, # Optimize loops when condition can be statically determined
                                                     :unused => true, # Drop unreferenced functions and variables
                                                     :hoist_funs => true, # Hoist function declarations
                                                     :hoist_vars => false, # Hoist var declarations
                                                     :if_return => true, # Optimizations for if/return and if/continue
                                                     :join_vars => true, # Join consecutive var statements
                                                     :cascade => true, # Cascade sequences
                                                     :negate_iife => true, # Negate immediately invoked function expressions to avoid extra parens
                                                     :pure_getters => false, # Assume that object property access does not have any side-effects
                                                     :pure_funcs => nil, # List of functions without side-effects. Can safely discard function calls when the result value is not used
                                                     :drop_console => false, # Drop calls to console.* functions
                                                     :angular => false, # Process @ngInject annotations
                                                     :keep_fargs => false # Preserve unused function arguments
                                                 }, # Apply transformations to code, set to false to skip
                                                 :define => {}, # Define values for symbol replacement
                                                 :enclose => false, # Enclose in output function wrapper, define replacements as key-value pairs
                                                 :source_filename => nil, # The filename of the input file
                                                 :source_root => nil, # The URL of the directory which contains :source_filename
                                                 :output_filename => nil, # The filename or URL where the minified output can be found
                                                 :input_source_map => nil, # The contents of the source map describing the input
                                                 :screw_ie8 => true # Don't bother to generate safe code for IE8
                                             })
  config.assets.css_compressor = :sass

  # Do not fallback to assets pipeline if a precompiled asset is missed.
  config.assets.compile = false

  # Generate digests for assets URLs.
  config.assets.digest = true

  # Version of your assets, change this if you want to expire all your assets.
  config.assets.version = '1.0'

  # Specifies the header that your server uses for sending files.
  # config.action_dispatch.x_sendfile_header = "X-Sendfile" # for apache
  # config.action_dispatch.x_sendfile_header = 'X-Accel-Redirect' # for nginx

  # Force all access to the app over SSL, use Strict-Transport-Security, and use secure cookies.
  # config.force_ssl = true

  # Set to :debug to see everything in the log.
  config.log_level = :info

  # Prepend all log lines with the following tags.
  # config.log_tags = [ :subdomain, :uuid ]

  # Use a different logger for distributed setups.
  # config.logger = ActiveSupport::TaggedLogging.new(SyslogLogger.new)

  # Use a different cache store in production.
  # config.cache_store = :mem_cache_store

  # Enable serving of images, stylesheets, and JavaScripts from an asset server.
  # config.action_controller.asset_host = "http://assets.example.com"

  # Precompile additional assets.
  # application.js, application.css, and all non-JS/CSS in app/assets folder are already added.
  # config.assets.precompile += %w( search.js )

  # Ignore bad email addresses and do not raise email delivery errors.
  # Set this to true and configure the email server for immediate delivery to raise delivery errors.
  # config.action_mailer.raise_delivery_errors = false

  # Enable locale fallbacks for I18n (makes lookups for any locale fall back to
  # the I18n.default_locale when a translation can not be found).
  config.i18n.fallbacks = true

  # Send deprecation notices to registered listeners.
  # config.active_support.deprecation = :notify

  # Disable automatic flushing of the log to improve performance.
  # config.autoflush_log = false

  # Use default logging formatter so that PID and timestamp are not suppressed.
  config.log_formatter = ::Logger::Formatter.new

  config.java_services_cache = :ServiceCache
end
