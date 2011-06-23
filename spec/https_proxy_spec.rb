#require 'webrick'
require 'webrick/https'
require 'net/https'
require 'uri'

include WEBrick

describe 'HTTPS proxying' do

  before(:all) do
    config = {
      :Port => 9443,
      :BindAddress => '127.0.0.1',
      :DocumentRoot => Dir.pwd + '/spec/data/',
      :Debugger => true,
      :Verbose => true,
      :SSLEnable => true,
      :SSLVerifyClient => ::OpenSSL::SSL::VERIFY_NONE,
      :SSLCertName => [ [ "CN", WEBrick::Utils::getservername ] ],
      #comment out these two lines to enable webrick logging
      :Logger => WEBrick::Log.new("/dev/null"),
      :AccessLog => [nil, nil],
    }
    
    @https_proc = fork {
      #the dots and pluses are outputted direct to stderr by webrick's ssl
      #routines.
      server = HTTPServer.new(config)
      trap('INT') { server.stop }
      server.start
    }
    #give the webrick a few seconds to start up
    sleep(3)
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
  end


  it 'validate mocked env' do
    #sleep 60
    filename = Dir.pwd + '/spec/data/random.10k'
    uri = URI.parse('https://127.0.0.1:9443/random.10k')
    https_client = Net::HTTP.new uri.host, uri.port
    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
    https_client.use_ssl = true

    response = https_client.request(Net::HTTP::Get.new(uri.path))

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

#this won't work until tslug supports https
#  it 'pull a file from thumbslug' do
#    #sleep 60
#    filename = Dir.pwd + '/spec/data/random.10k'
#    uri = URI.parse('https://127.0.0.1:8088/random.10k')
#    https_client = Net::HTTP.new uri.host, uri.port
#    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
#    https_client.use_ssl = true
#
#    response = https_client.request(Net::HTTP::Get.new(uri.path))
#
#    file_digest = Digest::MD5.hexdigest(File.read(filename))
#    uri_digest = Digest::MD5.hexdigest(response.body)
#
#    #ensure that the file we got is the same as what's on disk
#    uri_digest.should == file_digest
#  end

end
