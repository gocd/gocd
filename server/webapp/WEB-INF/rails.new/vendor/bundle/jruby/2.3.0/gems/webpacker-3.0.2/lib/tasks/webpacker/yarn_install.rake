namespace :webpacker do
  desc "Support for older Rails versions. Install all JavaScript dependencies as specified via Yarn"
  task :yarn_install, [:arg1, :arg2] do |task, args|
    system "yarn #{args[:arg1]} #{args[:arg2]}"
  end
end
