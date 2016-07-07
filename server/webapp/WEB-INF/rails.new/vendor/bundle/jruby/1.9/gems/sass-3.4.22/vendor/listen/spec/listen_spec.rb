require 'spec_helper'

describe Listen do
  describe '#to' do
    let(:listener)       { double(Listen::Listener) }
    let(:listener_class) { Listen::Listener }
    before { listener_class.stub(:new => listener) }

    context 'with one path to listen to' do
      context 'without options' do
        it 'creates an instance of Listener' do
          listener_class.should_receive(:new).with('/path')
          described_class.to('/path')
        end
      end

      context 'with options' do
        it 'creates an instance of Listener with the passed params' do
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
        it 'starts the listener after creating it' do
          listener.should_receive(:start)
          described_class.to('/path', :filter => '**/*') { |modified, added, removed| }
        end
      end
    end

    context 'with multiple paths to listen to' do
      context 'without options' do
        it 'creates an instance of Listener' do
          listener_class.should_receive(:new).with('path1', 'path2')
          described_class.to('path1', 'path2')
        end
      end

      context 'with options' do
        it 'creates an instance of Listener with the passed params' do
          listener_class.should_receive(:new).with('path1', 'path2', :filter => '**/*')
          described_class.to('path1', 'path2', :filter => '**/*')
        end
      end

      context 'without a block' do
        it 'returns a Listener instance created with the passed params' do
          described_class.to('path1', 'path2', :filter => '**/*').should eq listener
        end
      end

      context 'with a block' do
        it 'starts a Listener instance after creating it with the passed params' do
          listener.should_receive(:start)
          described_class.to('path1', 'path2', :filter => '**/*') { |modified, added, removed| }
        end
      end
    end
  end
end
