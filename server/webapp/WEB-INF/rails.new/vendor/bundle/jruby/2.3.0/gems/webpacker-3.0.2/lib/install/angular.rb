require "webpacker/configuration"

puts "Copying angular example entry file to #{Webpacker.config.source_entry_path}"
copy_file "#{__dir__}/examples/angular/hello_angular.js", "#{Webpacker.config.source_entry_path}/hello_angular.js"

puts "Copying hello_angular app to #{Webpacker.config.source_path}"
directory "#{__dir__}/examples/angular/hello_angular", "#{Webpacker.config.source_path}/hello_angular"

puts "Copying tsconfig.json to the Rails root directory for typescript"
copy_file "#{__dir__}/examples/angular/tsconfig.json", "tsconfig.json"

puts "Installing all angular dependencies"
run "yarn add typescript ts-loader core-js zone.js rxjs @angular/core @angular/common @angular/compiler @angular/platform-browser @angular/platform-browser-dynamic"

puts "Webpacker now supports angular and typescript 🎉"
