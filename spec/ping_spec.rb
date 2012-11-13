require 'uri'
require 'openssl'
require 'net/https'
require 'webrick/https'
require 'thumbslug_common'
require 'timeout'

describe 'Thumbslug Ping' do
  include ThumbslugMethods

  around(:each) do |test|
    Timeout::timeout(6) {
      test.run
    }
  end

  before(:all) do
    @tslugs_pipe = create_thumbslug({:ssl_client_dynamic_ssl => 'true'})
    @tslugs_no_dynamic_ssl_pipe = create_thumbslug({:port => '9998'})
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @tslugs_pipe.pid)
    Process.kill('INT', @tslugs_no_dynamic_ssl_pipe.pid)
    puts "Waiting for forked procs to terminate.."
    Process.waitall()
    print "done"
  end

  it 'ping fails with candlepin not running' do
    response = get('https://127.0.0.1:8088/ping')
    response.code.should == '502'
  end

  it 'pings thumbslug with dynamicSsl turned on' do
    cpin_proc = create_candlepin()
    begin
      response = get('https://127.0.0.1:8088/ping')
      response.code.should == '204'
    ensure
      Process.kill('INT', cpin_proc)
      Process.wait(cpin_proc)
    end
  end

  it 'pings thumbslug with dynamicSsl turned off' do
    response = get('https://127.0.0.1:9998/ping')
    response.code.should == '204'
  end

  def create_candlepin(params = {})
      privkey = OpenSSL::PKey::RSA.new(File.read("spec/data/cdn-key.pem"))
      server_cert = OpenSSL::X509::Certificate.new(File.read("spec/data/cdn.pem"))
      ca_cert = "spec/data/CA/ca-cert.pem"
        config = {
          :Port => 9898,
          :BindAddress => '127.0.0.1',
          :DocumentRoot => Dir.pwd + '/spec/data/',
          :Debugger => true,
          :Verbose => true,
          :SSLEnable => true,
          #:SSLVerifyClient => OpenSSL::SSL::VERIFY_PEER | OpenSSL::SSL::VERIFY_FAIL_IF_NO_PEER_CERT,
          :SSLPrivateKey => privkey,
          :SSLCertificate => server_cert,
          :SSLCACertificateFile => ca_cert,
          #comment out these two lines to enable webrick logging
          #:Logger => WEBrick::Log.new('/dev/null'),
          #:AccessLog => [nil, nil],
        }

      params.each_pair do |key, value|
        config[key] = value
      end

      rd, wr = IO.pipe
      pid = fork {
        rd.close #no need for this side in here
        $stderr = File.open('/dev/null', 'w')
        begin
          server = WEBrick::HTTPServer.new(config)
          trap('INT') { server.stop }
          server.mount "/candlepin/status", StatusHandler
          wr.write "Started webrick"
        rescue Exception => e
          wr.write "Error starting webrick:\n"
          wr.write  e.message
        ensure
          wr.close  #this makes the parent process unblock and continue
        end
        server.start
      }
      wr.close #no need for this side in here
      puts "#{rd.read}" #this will wait for the forked process to send an EOF
      rd.close
      return pid
    end

  class StatusHandler < WEBrick::HTTPServlet::AbstractServlet
    def do_GET(request, response)
      response.status = 200
      response['Content-Type'] = "application/json"
      response.body = '{
        "result" : true,
        "version" : "0.7.17",
        "release" : "1",
        "standalone" : true,
        "timeUTC" : "2012-11-08T16:38:06.924+0000"
      }'
    end
  end
end
