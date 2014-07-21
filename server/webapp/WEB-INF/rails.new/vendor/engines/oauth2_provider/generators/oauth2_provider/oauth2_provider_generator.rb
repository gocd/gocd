# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

class Oauth2ProviderGenerator < Rails::Generator::Base
  def manifest
    record do |m|
      m.template 'config/initializers/oauth2_provider.rb', "config/initializers/oauth2_provider.rb"

      m.directory 'db/migrate'
      ['create_oauth_clients', 'create_oauth_tokens', 'create_oauth_authorizations'].each_with_index do |file_name, index|
        m.template "db/migrate/#{file_name}.rb", "db/migrate/#{version_with_prefix(index)}_#{file_name}.rb", :migration_file_name => file_name
      end
      
    end
  end
  
  def after_generate
    puts "*"*80
    puts "Please edit the file 'config/initializers/oauth2_provider.rb' as per your needs!"
    puts "The readme file in the plugin contains more information about configuration."
    puts "*"*80
  end
  
  private
  def version_with_prefix(prefix)
    Time.now.utc.strftime("%Y%m%d%H%M%S") + "#{prefix}"
  end
  
end
