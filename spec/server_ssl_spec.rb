require 'thumbslug_common'
require 'openssl'
require 'socket'

describe 'Server SSL' do
  include ThumbslugMethods

# I was trying to read the stderr message for these two, and verify that
# the server exited with -1. but darn, this is hard!

#  it 'provides a friendly error message if the keystore cannot be read' do
    # XXX add me
#    fail
#  end

#  it 'provides a friendly error message if the keystore password is wrong' do
    # XXX add me
#    fail
#  end

  around(:each) do |test|
    Timeout::timeout(6) {
      test.run
    }
  end

  before(:all) do
    @https_proc = create_httpd(true)
    @tslugs_pipe = create_thumbslug({'ssl.keystore' => 'spec/data/keystore.p12',
                                    'ssl.keystore.password' => 'password'})
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
    Process.kill('INT', @tslugs_pipe.pid)
    puts "Waiting for forked procs to terminate.."
    Process.waitall()
    print "done"
  end



  it 'uses the configured ssl certificate for its server certificate' do
    socket = TCPSocket.new('127.0.0.1', 8088)
    ssl_context = OpenSSL::SSL::SSLContext.new()
    ssl_context.key = OpenSSL::PKey::RSA.new(File.read("spec/data/spec/test-entitlement.pem"))
    ssl_context.cert = OpenSSL::X509::Certificate.new(File.read("spec/data/spec/test-entitlement.pem"))
    ssl_socket = OpenSSL::SSL::SSLSocket.new(socket, ssl_context)

    ssl_socket.sync_close = true

    ssl_socket.connect

    serial = ssl_socket.peer_cert.serial

    # shut everything down before asserting
    ssl_socket.close

    # this is the magic number from our cert
    serial.should == 18235744395953304678
  end
end
