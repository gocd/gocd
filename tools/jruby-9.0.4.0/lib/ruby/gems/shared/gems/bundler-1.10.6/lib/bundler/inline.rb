# Allows for declaring a Gemfile inline in a ruby script, optionally installing
# any gems that aren't already installed on the user's system.
#
# @note Every gem that is specified in this 'Gemfile' will be `require`d, as if
#       the user had manually called `Bundler.require`. To avoid a requested gem
#       being automatically required, add the `:require => false` option to the
#       `gem` dependency declaration.
#
# @param install [Boolean] whether gems that aren't already installed on the
#                          user's system should be installed.
#                          Defaults to `false`.
#
# @param gemfile [Proc]    a block that is evaluated as a `Gemfile`.
#
# @example Using an inline Gemfile
#
#          #!/usr/bin/env ruby
#
#          require 'bundler/inline'
#
#          gemfile do
#            source 'https://rubygems.org'
#            gem 'json', require: false
#            gem 'nap', require: 'rest'
#            gem 'cocoapods', '~> 0.34.1'
#          end
#
#          puts Pod::VERSION # => "0.34.4"
#
def gemfile(install = false, &gemfile)
  require 'bundler'
  old_root = Bundler.method(:root)
  def Bundler.root
    Pathname.pwd.expand_path
  end
  ENV['BUNDLE_GEMFILE'] ||= 'Gemfile'

  builder = Bundler::Dsl.new
  builder.instance_eval(&gemfile)

  definition = builder.to_definition(nil, true)
  def definition.lock(*); end
  definition.validate_ruby!

  if install
    Bundler.ui = Bundler::UI::Shell.new
    Bundler::Installer.install(Bundler.root, definition, :system => true)
    Bundler::Installer.post_install_messages.each do |name, message|
      Bundler.ui.info "Post-install message from #{name}:\n#{message}"
    end
  end

  runtime = Bundler::Runtime.new(nil, definition)
  runtime.setup.require

  bundler_module = class << Bundler; self; end
  bundler_module.send(:define_method, :root, old_root)
end
