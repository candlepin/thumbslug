require 'webrick'
require 'net/http'
require 'uri'

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
    @tslug_proc = fork {
      exec("java -jar " + Dir.pwd + "/target/thumbslug-1.0.0.jar")
    } 
    Process.detach(@tslug_proc)
    #give the webrick a few seconds to start up
    sleep(3)
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @http_proc)
    puts @tslug_proc
    Process.kill('INT', @tslug_proc)
  end


  it 'validate mocked env' do
    #sleep 60
    filename = Dir.pwd + '/spec/data/random.10k'
    uri = URI.parse('http://127.0.0.1:9090/random.10k')
    response = Net::HTTP.get_response(uri)

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a file from thumbslug' do
    #sleep 60
    filename = Dir.pwd + '/spec/data/random.10k'
    uri = URI.parse('http://127.0.0.1:8088/random.10k')
    response = Net::HTTP.get_response(uri)

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end
end
