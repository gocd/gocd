appraise "rails32" do
  gem "railties", "~> 3.2.22.5"
  gem 'tzinfo'
end

{rails40: '4.0.13', rails41: '4.1.16', rails42: '4.2.8', rails50: '5.0.2'}.each do |rails, version|
  appraise "#{rails}" do
    gem "railties", "~> #{version}"
    gem "sprockets", "< 3"
  end

  appraise "#{rails}-sprockets3" do
    gem "railties", "~> #{version}"
    gem "sprockets", "~> 3.0"
  end
end
