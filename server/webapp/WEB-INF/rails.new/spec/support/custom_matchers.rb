module GoCDCustomMatchers
  RSpec::Matchers.define :receive_render_with do |args|
    match do |controller|
      expect(controller).to receive(:render) do |actual|
        expect(actual).to eq(args)
      end
    end
  end

  RSpec::Matchers.define :receive_redirect_to do |expected_url|
    match do |controller|
      expect(controller).to receive(:redirect_to) do |actual|
        expect(actual).to RSpec::Matchers::BuiltIn::Match.new(expected_url)
      end
    end
  end

  RSpec::Matchers.define :be_nil_or_empty do
    match do |actual|
      actual.nil? or actual.size == 0
    end
  end
end