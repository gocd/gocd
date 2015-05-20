require 'spec_helper'

describe Listen do
  describe '#to' do
    context 'with one path to listen to' do
      let(:listener)       { mock(Listen::Listener) }
      let(:listener_class) { Listen::Listener }

      before { listener_class.stub(:new => listener) }

      context 'without options' do
        it 'creates an instance of Listner' do
          listener_class.should_receive(:new).with('/path')
          described_class.to('/path')
        end
      end

      context 'with options' do
        it 'creates an instance of Listner with the passed params' do
          listener_class.should_receive(:new).with('/path', :filter => '**/*')
          described_class.to('/path', :filter => '**/*')
        end
      end

      context 'without a block' do
        it 'returns the listener' do
          described_class.to('/path', :filter => '**/*').should eq listener
        end
      end

      context 'with a block' do
        it 'starts the listner after creating it' do
          listener.should_receive(:start)
          described_class.to('/path', :filter => '**/*') { |modified, added, removed| }
        end
      end
    end

    context 'with multiple paths to listen to' do
      let(:multi_listener)       { mock(Listen::MultiListener) }
      let(:multi_listener_class) { Listen::MultiListener }

      before { multi_listener_class.stub(:new => multi_listener) }

      context 'without options' do
        it 'creates an instance of MultiListner' do
          multi_listener_class.should_receive(:new).with('path1', 'path2')
          described_class.to('path1', 'path2')
        end
      end

      context 'with options' do
        it 'creates an instance of MultiListner with the passed params' do
          multi_listener_class.should_receive(:new).with('path1', 'path2', :filter => '**/*')
          described_class.to('path1', 'path2', :filter => '**/*')
        end
      end

      context 'without a block' do
        it 'returns a MultiListener instance created with the passed params' do
          described_class.to('path1', 'path2', :filter => '**/*').should eq multi_listener
        end
      end

      context 'with a block' do
        it 'starts a MultiListener instance after creating it with the passed params' do
          multi_listener.should_receive(:start)
          described_class.to('path1', 'path2', :filter => '**/*') { |modified, added, removed| }
        end
      end
    end
  end
end
