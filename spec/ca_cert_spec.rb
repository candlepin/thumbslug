require 'net/https'
require 'uri'
require 'thumbslug_common'
require 'timeout'

describe 'CA Certificate checking' do
  include ThumbslugMethods

  around(:each) do |test|
    Timeout::timeout(6) {
      test.run
    }
  end

  before(:all) do
    @https_proc = create_httpd(true)
    @tslugs_pipe = create_thumbslug()
    @tslugs_bad_ca_pipe = create_thumbslug({'port' => '9997',
                                            'cdn.ssl.ca.keystore' => 'spec/data/unknown-ca-pub.pem'})
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
    Process.kill('INT', @tslugs_pipe.pid)
    Process.kill('INT', @tslugs_bad_ca_pipe.pid)
    puts "Waiting for forked procs to terminate.."
    Process.waitall()
    print "done"
  end

  it "should return when the cdn's ssl certificate can't be validated" do
    response = get('https://localhost:9997/this_will_404')
    response.code.should == '502'
  end

  it "should error out when the client's CA is unknown" do
    lambda do
      response = get('https://localhost:8088/this_will_404', nil,
                     'spec/data/unknown-ca.pem')
    end.should raise_exception(OpenSSL::SSL::SSLError)
  end
end
