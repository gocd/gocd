# Install webpacker
copy_file "#{__dir__}/config/webpacker.yml", "config/webpacker.yml"

puts "Copying webpack core config and loaders"
directory "#{__dir__}/config/webpack", "config/webpack"

puts "Copying .postcssrc.yml to app root directory"
copy_file "#{__dir__}/config/.postcssrc.yml", ".postcssrc.yml"

puts "Copying .babelrc to app root directory"
copy_file "#{__dir__}/config/.babelrc", ".babelrc"

puts "Creating javascript app source directory"
directory "#{__dir__}/javascript", Webpacker.config.source_path

puts "Installing binstubs"
run "bundle binstubs webpacker"

if File.exists?(".gitignore")
  append_to_file ".gitignore", <<-EOS
/public/packs
/public/packs-test
/node_modules
EOS
end

puts "Installing all JavaScript dependencies"
run "yarn add @rails/webpacker coffeescript@1.12.7"

puts "Installing dev server for live reloading"
run "yarn add --dev webpack-dev-server"

puts "Webpacker successfully installed 🎉 🍰"
