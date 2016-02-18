shared_examples_for 'a listener to changes on a file-system' do
  describe '#start' do
    before do
      subject.stub(:initialize_adapter) { adapter }
    end

    it 'starts the adapter' do
      adapter.should_receive(:start)
      subject.start
    end

    context 'with the blocking deprecated param set to true' do
      it 'displays a deprecation notice' do
        Kernel.should_receive(:warn).with(/#{Listen::Listener::BLOCKING_PARAMETER_DEPRECATION_MESSAGE}/)
        subject.start(true)
      end
    end

    context 'with the blocking deprecated param set to false' do
      it 'displays a deprecation notice' do
        Kernel.should_receive(:warn).with(/#{Listen::Listener::BLOCKING_PARAMETER_DEPRECATION_MESSAGE}/)
        subject.start(false)
      end
    end
  end

  describe '#start!' do
    before do
      subject.stub(:initialize_adapter) { adapter }
    end

    it 'starts the adapter' do
      adapter.should_receive(:start!)
      subject.start!
    end

    it 'passes the blocking param to the adapter' do
      adapter.should_receive(:start!)
      subject.start!
    end
  end

  context 'with a started listener' do
    before do
      subject.start
    end

    describe '#stop' do
      it "stops adapter" do
        adapter.should_receive(:stop)
        subject.stop
      end
    end

    describe '#pause' do
      it 'sets adapter.paused to true' do
        adapter.should_receive(:pause)
        subject.pause
      end

      it 'returns the same listener to allow chaining' do
        subject.pause.should equal subject
      end
    end

    describe '#unpause' do
      it 'sets adapter.paused to false' do
        adapter.should_receive(:unpause)
        subject.unpause
      end

      it 'returns the same listener to allow chaining' do
        subject.unpause.should equal subject
      end
    end

    describe '#paused?' do
      it 'returns false when there is no adapter' do
        subject.instance_variable_set(:@adapter, nil)
        subject.should_not be_paused
      end

      it 'returns true when adapter is paused' do
        adapter.should_receive(:paused?) { true }
        subject.should be_paused
      end

      it 'returns false when adapter is not paused' do
        adapter.should_receive(:paused?) { false }
        subject.should_not be_paused
      end
    end
  end

  describe '#change' do
    it 'sets the callback block' do
      callback = lambda { |modified, added, removed| }
      subject.change(&callback)
      subject.instance_variable_get(:@block).should eq callback
    end

    it 'returns the same listener to allow chaining' do
      subject.change(&Proc.new{}).should equal subject
    end
  end

  describe '#ignore' do
    it 'returns the same listener to allow chaining' do
      subject.ignore('some_directory').should equal subject
    end
  end

  describe '#ignore!' do
    it 'returns the same listener to allow chaining' do
      subject.ignore!('some_directory').should equal subject
    end
  end

  describe '#filter' do
    it 'returns the same listener to allow chaining' do
      subject.filter(/\.txt$/).should equal subject
    end
  end

  describe '#filter!' do
    it 'returns the same listener to allow chaining' do
      subject.filter!(/\.txt$/).should equal subject
    end
  end

  describe '#latency' do
    it 'sets the latency to @adapter_options' do
      subject.latency(0.7)
      subject.instance_variable_get(:@adapter_options).should eq(:latency => 0.7)
    end

    it 'returns the same listener to allow chaining' do
      subject.latency(0.7).should equal subject
    end
  end

  describe '#force_polling' do
    it 'sets force_polling to @adapter_options' do
      subject.force_polling(false)
      subject.instance_variable_get(:@adapter_options).should eq(:force_polling => false)
    end

    it 'returns the same listener to allow chaining' do
      subject.force_polling(true).should equal subject
    end
  end

  describe '#relative_paths' do
    it 'sets the relative paths option for paths in the callback' do
      subject.relative_paths(true)
      subject.instance_variable_get(:@use_relative_paths).should be_true
    end

    it 'returns the same listener to allow chaining' do
      subject.relative_paths(true).should equal subject
    end
  end

  describe '#polling_fallback_message' do
    it 'sets custom polling fallback message to @adapter_options' do
      subject.polling_fallback_message('custom message')
      subject.instance_variable_get(:@adapter_options).should eq(:polling_fallback_message => 'custom message')
    end

    it 'sets polling fallback message to false in @adapter_options' do
      subject.polling_fallback_message(false)
      subject.instance_variable_get(:@adapter_options).should eq(:polling_fallback_message => false)
    end

    it 'returns the same listener to allow chaining' do
      subject.polling_fallback_message('custom message').should equal subject
    end
  end
end
