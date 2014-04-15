require 'rbconfig'

# This generator bootstraps a Rails project for use with RSpec
class RspecGenerator < Rails::Generator::Base
  DEFAULT_SHEBANG = File.join(Config::CONFIG['bindir'],
                              Config::CONFIG['ruby_install_name'])

  def initialize(runtime_args, runtime_options = {})
    if test ?f, 'spec/spec_helper.rb'
      
      puts <<-HELPFUL_INSTRUCTIONS

It looks like you are upgrading rspec-rails in this application. Please let
this script overwrite everything it wants to with the following exceptions,
*if* you have customized any of these files:

  * spec/spec.opts
  * spec/rcov.opts
  * spec/spec_helper.rb
  
If you have customized spec/spec_helper.rb, please set aside a copy of that
file so that it can be updated and you can manually restore your changes.

HELPFUL_INSTRUCTIONS
    end
    Dir.mkdir('lib/tasks') unless File.directory?('lib/tasks')
    super
  end

  def manifest
    record do |m|
      script_options     = { :chmod => 0755, :shebang => options[:shebang] == DEFAULT_SHEBANG ? nil : options[:shebang] }

      m.directory 'lib/tasks'
      m.file      'rspec.rake',                    'lib/tasks/rspec.rake'

      m.file      'script/autospec',               'script/autospec',    script_options
      m.file      'script/spec',                   'script/spec',        script_options
      m.file      'script/spec_server',            'script/spec_server', script_options

      m.directory 'spec'
      m.file      'rcov.opts',                     'spec/rcov.opts'
      m.file      'spec.opts',                     'spec/spec.opts'
      m.template  'spec_helper.rb',                'spec/spec_helper.rb'
    end
  end

protected

  def banner
    "Usage: #{$0} rspec"
  end

end
