appraise "rails32" do
  gem "railties", "~> 3.2.22.5"
  gem 'tzinfo'
end

def define_appraisal(rails, version, sprockets)
  sprockets.each do |sprocket|
    appraise "#{rails}-sprockets-#{sprocket}" do
      gem "railties", "~> #{version}"
      gem "sprockets", "~> #{sprocket}.0"
    end
  end
end

[
  [:rails40, '4.0.13', [2, 3]],
  [:rails41, '4.1.16', [2, 3]],
  [:rails42, '4.2.9',  [2, 3]],
  [:rails50, '5.0.5',  [3]],
  [:rails51, '5.1.3',  [3]]
].each do |name, version, sprockets|
  define_appraisal(name, version, sprockets)
end
