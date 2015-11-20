appraise "rails32" do
  gem "railties", "~> 3.2.18"
  gem 'tzinfo'
end

{rails40: '4.0.5', rails41: '4.1.1', rails42: '4.2.1'}.each do |rails, version|
  appraise "#{rails}" do
    gem "railties", "~> #{version}"
    gem "sprockets", "< 3"
  end

  appraise "#{rails}-sprockets3" do
    gem "railties", "~> #{version}"
    gem "sprockets", "~> 3.0"
  end
end

appraise "rails-edge" do
  gem "railties", github: 'rails/rails'
  gem "sprockets", "~> 3.0"
end