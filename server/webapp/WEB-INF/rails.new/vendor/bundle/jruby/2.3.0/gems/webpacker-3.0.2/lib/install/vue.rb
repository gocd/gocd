require "webpacker/configuration"

puts "Copying the example entry file to #{Webpacker.config.source_entry_path}"
copy_file "#{__dir__}/examples/vue/hello_vue.js",
  "#{Webpacker.config.source_entry_path}/hello_vue.js"

puts "Copying Vue app file to #{Webpacker.config.source_entry_path}"
copy_file "#{__dir__}/examples/vue/app.vue",
  "#{Webpacker.config.source_path}/app.vue"

puts "Installing all Vue dependencies"
run "yarn add vue vue-loader vue-template-compiler"

puts "Webpacker now supports Vue.js 🎉"
