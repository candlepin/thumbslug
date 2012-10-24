/**
 * Copyright (c) 2011 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.thumbslug.ssl;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.junit.Test;

public class PemChainLoaderTest {

    @Test
    public void testAddCertificateChainOneCert() throws Exception {
        X509Certificate [] chain = PemChainLoader.loadChain(chainZero);

        assertEquals(1, chain.length);
    }

    @Test
    public void testAddCertificateChainManyCerts() throws Exception {
        X509Certificate [] chain = PemChainLoader.loadChain(chainZero + "\n" +
            chainOne + "\n" + chainTwo);

        assertEquals(3, chain.length);
    }

    @Test(expected = SslPemException.class)
    public void testAddCertificateChainEmptyFile() throws Exception {
        PemChainLoader.loadChain("");
    }

    @Test(expected = SslPemException.class)
    public void testAddCertificateChainNoCerts() throws Exception {
        PemChainLoader.loadChain("JUNK CONTENTS");
    }

    // These are just the parts of the redhat-uep.pem file
    private static String chainZero =
        "-----BEGIN CERTIFICATE-----\n" +
            "MIIG/TCCBOWgAwIBAgIBNzANBgkqhkiG9w0BAQUFADCBsTELMAkGA1UEBhMCVVMx\n" +
            "FzAVBgNVBAgMDk5vcnRoIENhcm9saW5hMRYwFAYDVQQKDA1SZWQgSGF0LCBJbmMu\n" +
            "MRgwFgYDVQQLDA9SZWQgSGF0IE5ldHdvcmsxMTAvBgNVBAMMKFJlZCBIYXQgRW50\n" +
            "aXRsZW1lbnQgT3BlcmF0aW9ucyBBdXRob3JpdHkxJDAiBgkqhkiG9w0BCQEWFWNh\n" +
            "LXN1cHBvcnRAcmVkaGF0LmNvbTAeFw0xMDEwMDQxMzI3NDhaFw0zMDA5MjkxMzI3\n" +
            "NDhaMIGuMQswCQYDVQQGEwJVUzEXMBUGA1UECAwOTm9ydGggQ2Fyb2xpbmExFjAU\n" +
            "BgNVBAoMDVJlZCBIYXQsIEluYy4xGDAWBgNVBAsMD1JlZCBIYXQgTmV0d29yazEu\n" +
            "MCwGA1UEAwwlUmVkIEhhdCBFbnRpdGxlbWVudCBQcm9kdWN0IEF1dGhvcml0eTEk\n" +
            "MCIGCSqGSIb3DQEJARYVY2Etc3VwcG9ydEByZWRoYXQuY29tMIICIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAg8AMIICCgKCAgEA2QurMeAVnCHVsuZNQzciWMdpd4LAVk2eGugN\n" +
            "0cxmBpzoVI8lIsJOmJkpOAuFOQMX9CBr8RuQyg4r1/OH/rfhm6FgGIw8TGKZoWC/\n" +
            "1B9teZqTiM85k6/1GRNxdk6dUK77HVO0PMIKtNBHRxIsXcRzJ1q+u5WPBes9pEVG\n" +
            "nbidTNUkknrSIdynTJcqAI/I0VAsqLqX87XJSzXKvRilE+p/fLHmVTAffl1Cn/Dy\n" +
            "KULxna7ooyrKKnfqeQ5dK8aMr1ASQ1wphWohLjegly9V0amEi+HHWnOL8toxJy8v\n" +
            "WUTUzzAvZ4ZTtTV26xGetZZWEaNyv7YCv2AexjcBQ2x+ejrFJrVNo9jizHS06HK8\n" +
            "UgHVDKhmVcAe2/5yrJCjKDLwg1FJfjKwhzhLYdNVCejpy8CHQndwO0EX1hHv/AfP\n" +
            "RTAmr5qPhHFD+uuIrYrSLUpgMLmWa9dinJcGeKlA1KJvG5emGMM3k64Xr7dJToXo\n" +
            "5loGyZ6lvKPIKLmfeXMRW/4+BqyzwbO1i4aIHAZcSPDFGKWwuvF0iVUYUUVxw0nv\n" +
            "qPZA4roq5+j/YSz0q5XGVgiIt34htlvunLp/ICGYJBR6zEHcB9aZGJdDcJvoYZjw\n" +
            "7Gphw6lFF6Ta4imoyhGECWKjd1ips3opcN+DlU0yCUrcIXVIXAnkTwu5ocOgAkxr\n" +
            "f/6FjqcCAwEAAaOCAR8wggEbMB0GA1UdDgQWBBSW/bscQED/QIStsh8LJsHDam/W\n" +
            "fDCB5QYDVR0jBIHdMIHagBTESXhWRZ0eLGFgw2ZLWAU3LwMie6GBtqSBszCBsDEL\n" +
            "MAkGA1UEBhMCVVMxFzAVBgNVBAgMDk5vcnRoIENhcm9saW5hMRAwDgYDVQQHDAdS\n" +
            "YWxlaWdoMRYwFAYDVQQKDA1SZWQgSGF0LCBJbmMuMRgwFgYDVQQLDA9SZWQgSGF0\n" +
            "IE5ldHdvcmsxHjAcBgNVBAMMFUVudGl0bGVtZW50IE1hc3RlciBDQTEkMCIGCSqG\n" +
            "SIb3DQEJARYVY2Etc3VwcG9ydEByZWRoYXQuY29tggkAkYrPyoUAAAAwEgYDVR0T\n" +
            "AQH/BAgwBgEB/wIBADANBgkqhkiG9w0BAQUFAAOCAgEArWBznYWKpY4LqAzhOSop\n" +
            "t30D2/UlCSr50l33uUCNYD4D4nTr/pyX3AR6P3JcOCz0t22pVCg8D3DZc5VlzY7y\n" +
            "P5RD3KbLxFNJTloclMG0n6aIN7baA4b8zwkduMQvKZnA/YNR5xE7V7J2WJHCEBBB\n" +
            "Z+ZFwGpGsoZpPZP4hHLVke3xHm6A5F5SzP1Ug0T9W80VLK4jtgyGs8l1R7rXiOIt\n" +
            "Nik8317KGq7DU8TI2Rw/9Gc8FKNfUYcVD7uC/MMQXJTRvkADmNLtZM63nhzpg1Hr\n" +
            "hA6U5YcDCBKsPA43/wsPOONYtrAlToD5hJhU+1Rhmwcw3qvWBO3NkdilqGFOTc2K\n" +
            "50PQrqoRTCZFS41nv2WqZFfbvSq4dZRJl8xpB4LAHSspsMrbr9WZHX5fbggf6ixw\n" +
            "S9KDqQbM7asP0FEKBFXJV1rE8P/oSK6yVWQyigTsNcdGR4AUzDsTO9udcwoM2Ed4\n" +
            "XdakVkF+dXm9ZBwv5UBf5ITSyMXL3qlusIOblJVGUQizumoq0LiSnjwbkxh2XHhd\n" +
            "XD/B/qax7FnaNg+TfujR/kk3kF1OpqWx/wC/qPR+zho1+35Al31gZOfNIn/sReoM\n" +
            "tcci9LFHGvijIy4VUDQK8HmGjIxJPrIIe1nB5BkiGyjwn00D5q+BwYVst1C68Rwx\n" +
            "iRZpyzOZmeineJvhrJZ4Tvs=\n" +
            "-----END CERTIFICATE-----\n";

    private static String chainOne =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIHZTCCBU2gAwIBAgIJAJGKz8qFAAAAMA0GCSqGSIb3DQEBBQUAMIGwMQswCQYD\n" +
            "VQQGEwJVUzEXMBUGA1UECAwOTm9ydGggQ2Fyb2xpbmExEDAOBgNVBAcMB1JhbGVp\n" +
            "Z2gxFjAUBgNVBAoMDVJlZCBIYXQsIEluYy4xGDAWBgNVBAsMD1JlZCBIYXQgTmV0\n" +
            "d29yazEeMBwGA1UEAwwVRW50aXRsZW1lbnQgTWFzdGVyIENBMSQwIgYJKoZIhvcN\n" +
            "AQkBFhVjYS1zdXBwb3J0QHJlZGhhdC5jb20wHhcNMTAwMzE4MTEyNDU0WhcNMzAw\n" +
            "MzEzMTEyNDU0WjCBsTELMAkGA1UEBhMCVVMxFzAVBgNVBAgMDk5vcnRoIENhcm9s\n" +
            "aW5hMRYwFAYDVQQKDA1SZWQgSGF0LCBJbmMuMRgwFgYDVQQLDA9SZWQgSGF0IE5l\n" +
            "dHdvcmsxMTAvBgNVBAMMKFJlZCBIYXQgRW50aXRsZW1lbnQgT3BlcmF0aW9ucyBB\n" +
            "dXRob3JpdHkxJDAiBgkqhkiG9w0BCQEWFWNhLXN1cHBvcnRAcmVkaGF0LmNvbTCC\n" +
            "AiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBALsmiohDnNvIpBMZVJR5pbP6\n" +
            "GrE5B4doUmvTeR4XJ5C66uvFTwuGTVigNXAL+0UWf9r2AwxKEPCy65h7fLbyK4W7\n" +
            "/xEZPVsamQYDHpyBwlkPkJ3WhHneqQWC8bKkv8Iqu08V+86biCDDAh6uP0SiAz7a\n" +
            "NGaLEnOe5L9WNfsYyNwrG+2AfiLy/1LUtmmg5dc6Ln7R+uv0PZJ5J2iUbiT6lMz3\n" +
            "v73zAxuEjiDNurZzxzHSSEYzw0W1eO6zM4F26gcOuH2BHemPMjHi+c1OnheaafDE\n" +
            "HQJTNgECz5Xe7WGdZwOyn9a8GtMvm0PAhGVyp7RAWxxfoU1B794cBb66IKKjliJQ\n" +
            "5DKoqyxD9qJbMF8U4Kd1ZIVB0Iy2WEaaqCFMIi3xtlWVUNku5x21ewMmJvwjnWZA\n" +
            "tUeKQUFwIXqSjuOoZDu80H6NQb+4dnRSjWlx/m7HPk75m0zErshpB2HSKUnrs4wR\n" +
            "i7GsWDDcqBus7eLMwUZPvDNVcLQu/2Y4DUHNbJbn7+DwEqi5D0heC+dyY8iS45gp\n" +
            "I/yhVvq/GfKL+dqjaNaE4CorJJA5qJ9f383Ol/aub+aJeBahCBNuVa2daA9Bo3BA\n" +
            "dnL7KkILPFyCcEhQITnu70Qn9sQlwYcRoYF2LWAm9DtLrBT0Y0w7wQHh8vNhwEQ7\n" +
            "k5G87WpwzcC8y6ePR0vFAgMBAAGjggF9MIIBeTAdBgNVHQ4EFgQUxEl4VkWdHixh\n" +
            "YMNmS1gFNy8DInswgeUGA1UdIwSB3TCB2oAUiEumRcRG7I/Wz6b2Gs8mPJDMfxeh\n" +
            "gbakgbMwgbAxCzAJBgNVBAYTAlVTMRcwFQYDVQQIDA5Ob3J0aCBDYXJvbGluYTEQ\n" +
            "MA4GA1UEBwwHUmFsZWlnaDEWMBQGA1UECgwNUmVkIEhhdCwgSW5jLjEYMBYGA1UE\n" +
            "CwwPUmVkIEhhdCBOZXR3b3JrMR4wHAYDVQQDDBVFbnRpdGxlbWVudCBNYXN0ZXIg\n" +
            "Q0ExJDAiBgkqhkiG9w0BCQEWFWNhLXN1cHBvcnRAcmVkaGF0LmNvbYIJAOb+Qigl\n" +
            "yeZeMAwGA1UdEwQFMAMBAf8wCwYDVR0PBAQDAgEGMBEGCWCGSAGG+EIBAQQEAwIB\n" +
            "BjAgBgNVHREEGTAXgRVjYS1zdXBwb3J0QHJlZGhhdC5jb20wIAYDVR0SBBkwF4EV\n" +
            "Y2Etc3VwcG9ydEByZWRoYXQuY29tMA0GCSqGSIb3DQEBBQUAA4ICAQBbTSz+UIXP\n" +
            "AVIT0ZVL1flCHR113aj2j3UBZkaoDkSxtEfa1nqysmN0llpqh4NVBL3anEFYxokL\n" +
            "hQ2PB8mmuD5EuWaNxnXTc4Sr5dsOcjkFiU197lybaJK7w4OzQ2Qg/X/t4+R78cfM\n" +
            "ZK/qHpjuyT3NyHHvCug/WzkvU09pRr2aVHI+fn68u18TRzPJNKvegR4YeA3vsyQW\n" +
            "BgEc8sU7KrAvikFJ3mCTpAk+6SRgbGFLyZE637Qrzy2DDBw0V020dkTkC6YnEsZg\n" +
            "HwZWVmLtCgLlnimx6SRft+6zrXVHWZxod1GT/af7vizpmhrXt/Nu5Se7dpOhPayo\n" +
            "NwYCFNmfZeL4W/foSKNfaizZcc+tiNABRtT+tplfniv/yjr7sBAsFPhJqQB8CfsQ\n" +
            "8BVvKkHtixygyo+EO+NEotZGw3cn+/7soo9B1bWXk3PFSwEr+KwINACFGv2zcGLI\n" +
            "oeP4iK6DHZImWEV4tgMrQyXatEyPh2axPWU3SjY/fr1Ub5gEt+WpCtyYIN4ObBaN\n" +
            "eL3NPfTj79/VFZ22PhUInmGY/VK/ymvl/dkWyWi8zD8Aq55ofZ33FvQ46dcLp1pV\n" +
            "KWApIVqO27uhL6YxXDFi6n7RXACEIVz6JqDh5fGmOH1F+vfumZKzW78LlVD2QY15\n" +
            "rmCh0i9+AUCiUsNyYdJbSZDPiFPBwlwUoQ==\n" +
            "-----END CERTIFICATE-----\n";

    private static String chainTwo =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIHZDCCBUygAwIBAgIJAOb+QiglyeZeMA0GCSqGSIb3DQEBBQUAMIGwMQswCQYD\n" +
            "VQQGEwJVUzEXMBUGA1UECAwOTm9ydGggQ2Fyb2xpbmExEDAOBgNVBAcMB1JhbGVp\n" +
            "Z2gxFjAUBgNVBAoMDVJlZCBIYXQsIEluYy4xGDAWBgNVBAsMD1JlZCBIYXQgTmV0\n" +
            "d29yazEeMBwGA1UEAwwVRW50aXRsZW1lbnQgTWFzdGVyIENBMSQwIgYJKoZIhvcN\n" +
            "AQkBFhVjYS1zdXBwb3J0QHJlZGhhdC5jb20wHhcNMTAwMzE3MTkwMDQ0WhcNMzAw\n" +
            "MzEyMTkwMDQ0WjCBsDELMAkGA1UEBhMCVVMxFzAVBgNVBAgMDk5vcnRoIENhcm9s\n" +
            "aW5hMRAwDgYDVQQHDAdSYWxlaWdoMRYwFAYDVQQKDA1SZWQgSGF0LCBJbmMuMRgw\n" +
            "FgYDVQQLDA9SZWQgSGF0IE5ldHdvcmsxHjAcBgNVBAMMFUVudGl0bGVtZW50IE1h\n" +
            "c3RlciBDQTEkMCIGCSqGSIb3DQEJARYVY2Etc3VwcG9ydEByZWRoYXQuY29tMIIC\n" +
            "IjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA2Z+mW7OYcBcGxWS+RSKG2GJ2\n" +
            "csMXiGGfEp36vKVsIvypmNS60SkicKENMYREalbdSjrgfXxPJygZWsVWJ5lHPfBV\n" +
            "o3WkFrFHTIXd/R6LxnaHD1m8Cx3GwEeuSlE/ASjc1ePtMnsHH7xqZ9wdl85b1C8O\n" +
            "scgO7fwuM192kvv/veI/BogIqUQugtG6szXpV8dp4ml029LXFoNIy2lfFoa2wKYw\n" +
            "MiUHwtYgAz7TDY63e8qGhd5PoqTv9XKQogo2ze9sF9y/npZjliNy5qf6bFE+24oW\n" +
            "E8pGsp3zqz8h5mvw4v+tfIx5uj7dwjDteFrrWD1tcT7UmNrBDWXjKMG81zchq3h4\n" +
            "etgF0iwMHEuYuixiJWNzKrLNVQbDmcLGNOvyJfq60tM8AUAd72OUQzivBegnWMit\n" +
            "CLcT5viCT1AIkYXt7l5zc/duQWLeAAR2FmpZFylSukknzzeiZpPclRziYTboDYHq\n" +
            "revM97eER1xsfoSYp4mJkBHfdlqMnf3CWPcNgru8NbEPeUGMI6+C0YvknPlqDDtU\n" +
            "ojfl4qNdf6nWL+YNXpR1YGKgWGWgTU6uaG8Sc6qGfAoLHh6oGwbuz102j84OgjAJ\n" +
            "DGv/S86svmZWSqZ5UoJOIEqFYrONcOSgztZ5tU+gP4fwRIkTRbTEWSgudVREOXhs\n" +
            "bfN1YGP7HYvS0OiBKZUCAwEAAaOCAX0wggF5MB0GA1UdDgQWBBSIS6ZFxEbsj9bP\n" +
            "pvYazyY8kMx/FzCB5QYDVR0jBIHdMIHagBSIS6ZFxEbsj9bPpvYazyY8kMx/F6GB\n" +
            "tqSBszCBsDELMAkGA1UEBhMCVVMxFzAVBgNVBAgMDk5vcnRoIENhcm9saW5hMRAw\n" +
            "DgYDVQQHDAdSYWxlaWdoMRYwFAYDVQQKDA1SZWQgSGF0LCBJbmMuMRgwFgYDVQQL\n" +
            "DA9SZWQgSGF0IE5ldHdvcmsxHjAcBgNVBAMMFUVudGl0bGVtZW50IE1hc3RlciBD\n" +
            "QTEkMCIGCSqGSIb3DQEJARYVY2Etc3VwcG9ydEByZWRoYXQuY29tggkA5v5CKCXJ\n" +
            "5l4wDAYDVR0TBAUwAwEB/zALBgNVHQ8EBAMCAQYwEQYJYIZIAYb4QgEBBAQDAgEG\n" +
            "MCAGA1UdEQQZMBeBFWNhLXN1cHBvcnRAcmVkaGF0LmNvbTAgBgNVHRIEGTAXgRVj\n" +
            "YS1zdXBwb3J0QHJlZGhhdC5jb20wDQYJKoZIhvcNAQEFBQADggIBAJ1hEdNBDTRr\n" +
            "6kI6W6stoogSUwjuiWPDY8DptwGhdpyIfbCoxvBR7F52DlwyXOpCunogfKMRklnE\n" +
            "gH1Wt66RYkgNuJcenKHAhR5xgSLoPCOVF9rDjMunyyBuxjIbctM21R7BswVpsEIE\n" +
            "OpV5nlJ6wkHsrn0/E+Zk5UJdCzM+Fp4hqHtEn/c97nvRspQcpWeDg6oUvaJSZTGM\n" +
            "8yFpzR90X8ZO4rOgpoERukvYutUfJUzZuDyS3LLc6ysamemH93rZXr52zc4B+C9G\n" +
            "Em8zemDgIPaH42ce3C3TdVysiq/yk+ir7pxW8toeavFv75l1UojFSjND+Q2AlNQn\n" +
            "pYkmRznbD5TZ3yDuPFQG2xYKnMPACepGgKZPyErtOIljQKCdgcvb9EqNdZaJFz1+\n" +
            "/iWKYBL077Y0CKwb+HGIDeYdzrYxbEd95YuVU0aStnf2Yii2tLcpQtK9cC2+DXjL\n" +
            "Yf3kQs4xzH4ZejhG9wzv8PGXOS8wHYnfVNA3+fclDEQ1mEBKWHHmenGI6QKZUP8f\n" +
            "g0SQ3PNRnSZu8R+rhABOEuVFIBRlaYijg2Pxe0NgL9FlHsNyRfo6EUrB2QFRKACW\n" +
            "3Mo6pZyDjQt7O8J7l9B9IIURoJ1niwygf7VSJTMl2w3fFleNJlZTGgdXw0V+5g+9\n" +
            "Kg6Ay0rrsi4nw1JHue2GvdjdfVOaWSWC\n" +
            "-----END CERTIFICATE-----\n";
}
