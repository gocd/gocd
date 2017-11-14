namespace :webpacker do
  desc "Verifies that bin/webpack & bin/webpack-dev-server are present."
  task :check_binstubs do
    unless File.exist?("bin/webpack") && File.exist?("bin/webpack-dev-server")
      $stderr.puts "Webpack binstubs not found.\n"\
           "Have you run rails webpacker:install ?\n"\
           "Make sure the bin directory or binstubs are not included in .gitignore\n"\
           "Exiting!"
      exit!
    end
  end
end
