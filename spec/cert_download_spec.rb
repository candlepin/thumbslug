require 'uri'
require 'openssl'
require 'net/https'
require 'webrick/https'
require 'thumbslug_common'
require 'timeout'

describe 'Certificate download from candlepin' do
  include ThumbslugMethods

  around(:each) do |test|
    Timeout::timeout(6) {
      test.run
    }
  end

  before(:all) do
    @https_proc = create_httpd(true)
    @tslugs_pipe = create_thumbslug({:ssl_keystore => 'spec/data/keystore-spec.p12',
                                     :ssl_keystore_password => 'pass',
                                     :ssl_client_dynamic_ssl => 'true',
                                    })
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
    Process.kill('INT', @tslugs_pipe.pid)
    puts "Waiting for forked procs to terminate.."
    Process.waitall()
    print "done"
  end

  it 'returns a 502 with no open port to candlepin' do
    response = get('https://127.0.0.1:8088/lorem.ipsum')
    response.code.should == '502'
  end

  it 'returns a 502 on a non 20X response from candlepin' do
    cpin_proc = create_candlepin(FiveHundred)

    begin
      response = get('https://127.0.0.1:8088/lorem.ipsum')
      response.code.should == '502'
    ensure
      Process.kill('INT', cpin_proc)
      Process.wait(cpin_proc)
    end
  end

  it 'returns a 502 on a non certificate response from candlepin' do
    cpin_proc = create_candlepin(Garbage)

    begin
      response = get('https://127.0.0.1:8088/lorem.ipsum')
      response.code.should == '502'
    ensure
      Process.kill('INT', cpin_proc)
      Process.wait(cpin_proc)
    end
  end

  it 'allows downloading content with a valid certificate' do
    cpin_proc = create_candlepin(Cert)

    begin
      response = get('https://127.0.0.1:8088/lorem.ipsum')
      response.code.should == '200'
    ensure
      Process.kill('INT', cpin_proc)
      Process.wait(cpin_proc)
    end
  end

  it 'allows downloading content with a valid V3 certificate' do
    cpin_proc = create_candlepin(Cert)

    begin
      response = get('https://127.0.0.1:8088/lorem.ipsum', headers = nil,
                     pem = 'spec/data/spec/cert-v3.pem')
      response.code.should == '200'
    ensure
      Process.kill('INT', cpin_proc)
      Process.wait(cpin_proc)
    end

  end

  it 'returns a 401 for a revoked certificate' do
    cpin_proc = create_candlepin(Cert)

    begin
      response = get('https://127.0.0.1:8088/lorem.ipsum', headers = nil,
                    pem = 'spec/data/spec/revoked-cert.pem')
      response.code.should == '401'
    ensure
      Process.kill('INT', cpin_proc)
      Process.wait(cpin_proc)
    end
  end

end

include WEBrick

def create_candlepin(cert_handler, params = {})
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
#        :SSLVerifyClient => OpenSSL::SSL::VERIFY_PEER | OpenSSL::SSL::VERIFY_FAIL_IF_NO_PEER_CERT,
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
        server = HTTPServer.new(config)
        trap('INT') { server.stop }
        # NOTE: this is the id hardcoded into our test cert.
        sub_id = 'ff8080813a73d4b6013a73d5b82e01f4'
        ent_id = 'ff8080813a73d4b6013a7ae77cbf2f4c'
        ent_id_v3 = 'ff8080813a73d4b6013a7a1b959d2f38'
        server.mount "/candlepin/entitlements/#{ent_id}/upstream_cert", cert_handler
        server.mount "/candlepin/entitlements/#{ent_id_v3}/upstream_cert", cert_handler
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

class FourOhFour < WEBrick::HTTPServlet::AbstractServlet

  def do_GET(request, response)
    response.status = 404
    response['Content-Type'] = "text/plain"
    response.body = 'Error! Not Found'
  end

end

class Cert < WEBrick::HTTPServlet::AbstractServlet
  #pretend to return a cert

  def do_GET(request, response)
    response.status = 200
    response['Content-Type'] = "text/plain"
    body = ""
    File.open("spec/data/cdn-client.pem", 'r') do |file|
        body = file.read
    end
    response.body = body
  end
end

class Garbage < WEBrick::HTTPServlet::AbstractServlet
  #pretend to return a cert

  def do_GET(request, response)
    response.status = 200
    response['Content-Type'] = "text/plain"
    response.body = "THIS IS NOT A CERT"
  end
end
