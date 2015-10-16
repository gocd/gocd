# This file is sourced by jruby-rack and is used to perform initialization of the jruby environment
# because jruby-rack does not respect GEM_HOME/GEM_PATH set in web.xml
if $servlet_context
  ENV['GEM_HOME']       = $servlet_context.getRealPath('/WEB-INF/rails.new/vendor/bundle/jruby/1.9')
  ENV['BUNDLE_GEMFILE'] ||= $servlet_context.getRealPath('/WEB-INF/rails.new/Gemfile')
else
  ENV['GEM_HOME']       = File.expand_path(File.join('..', '/rails.new/vendor/bundle/jruby/1.9'), __FILE__)
  ENV['BUNDLE_GEMFILE'] ||= File.expand_path(File.join('..', '/rails.new/Gemfile'), __FILE__)
end

ENV['RAILS_ENV']      ||= (ENV['RACK_ENV'] || 'production')

if ENV['RAILS_ENV'] == 'production'
  ENV['EXECJS_RUNTIME'] = 'Disabled'
  ENV['BUNDLE_WITHOUT'] = 'development:test:assets'
end
