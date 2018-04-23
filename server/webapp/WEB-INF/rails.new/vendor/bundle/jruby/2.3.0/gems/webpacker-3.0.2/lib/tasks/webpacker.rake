tasks = {
  "webpacker:install"                 => "Installs and setup webpack with yarn",
  "webpacker:compile"                 => "Compiles webpack bundles based on environment",
  "webpacker:check_node"              => "Verifies if Node.js is installed",
  "webpacker:check_yarn"              => "Verifies if yarn is installed",
  "webpacker:check_binstubs"          => "Verifies that bin/webpack & bin/webpack-dev-server are present",
  "webpacker:verify_install"          => "Verifies if webpacker is installed",
  "webpacker:yarn_install"            => "Support for older Rails versions. Install all JavaScript dependencies as specified via Yarn",
  "webpacker:install:react"           => "Installs and setup example React component",
  "webpacker:install:vue"             => "Installs and setup example Vue component",
  "webpacker:install:angular"         => "Installs and setup example Angular component",
  "webpacker:install:elm"             => "Installs and setup example Elm component"
}.freeze

desc "Lists all available tasks in webpacker"
task :webpacker do
  puts "Available webpacker tasks are:"
  tasks.each { |task, message| puts task.ljust(30) + message }
end
