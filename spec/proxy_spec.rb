require 'webrick'
include WEBrick

describe 'HTTP proxying' do

  before(:all) do
    config = {
      :Port => 9090,
      :BindAddress => '127.0.0.1',
      :DocumentRoot => Dir.pwd + '/spec/data/',
      #comment out these two lines to enable webrick logging
      :Logger => WEBrick::Log.new("/dev/null"),
      :AccessLog => [nil, nil],
    }
    
    @http_proc = fork {
      server = HTTPServer.new(config)
      trap('INT') { server.stop }
      server.start
    }
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @http_proc)
  end


  it 'should do stuff' do
    1.should == 1
  end
end
