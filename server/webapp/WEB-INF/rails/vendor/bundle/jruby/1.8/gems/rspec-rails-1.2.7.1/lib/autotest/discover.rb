Autotest.add_discovery do
  style = []
  style << "rails" if File.exist? 'config/environment.rb'
  style
end