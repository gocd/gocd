require 'validatable'
require 'ext/ssl_certificate.rb'
require 'ext/validatable_ext.rb'
require 'gadgets'
if RUBY_PLATFORM =~ /java/
  java.lang.System.setProperty('java.awt.headless', 'true')
  require 'shindig'
end

['app/models', 'app/controllers'].each do |dir|
  Dir[File.join(File.dirname(__FILE__), dir, '**', '*.rb')].each do |f|
    require f
  end
end
