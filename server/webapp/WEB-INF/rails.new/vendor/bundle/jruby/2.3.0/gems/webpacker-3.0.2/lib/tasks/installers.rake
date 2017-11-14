INSTALLERS = {
  "Angular": :angular,
  "Elm": :elm,
  "React": :react,
  "Vue": :vue
}.freeze

namespace :webpacker do
  namespace :install do
    INSTALLERS.each do |name, task_name|
      desc "Install everything needed for #{name}"
      task task_name => ["webpacker:verify_install"] do
        template = File.expand_path("../install/#{task_name}.rb", __dir__)
        if Rails::VERSION::MAJOR >= 5
          exec "#{RbConfig.ruby} ./bin/rails app:template LOCATION=#{template}"
        else
          exec "#{RbConfig.ruby} ./bin/rake rails:template LOCATION=#{template}"
        end
      end
    end
  end
end
