/*
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.proxy.core.testsuite.testcases;

import ee.ria.xroad.common.SystemProperties;

import org.apache.commons.lang3.ArrayUtils;
import org.niis.xroad.proxy.core.test.Message;
import org.niis.xroad.proxy.core.test.TestContext;
import org.niis.xroad.proxy.core.testsuite.SslMessageTestCase;
import org.niis.xroad.proxy.core.util.SSLContextUtil;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * SSL connection should fail cause client proxy is set to use not accepted cipher suite. Test is expecting that
 * modifying system property "xroad.proxy.xroad-tls-ciphers" on runtime will only affect on client proxy (property is
 * read again per new socket conn) where as the property on the server side proxy is read only once (on startup).
 */
public class SslClientCipherSuiteSetupError extends SslMessageTestCase {

    String propertyName = "xroad.proxy.xroad-tls-ciphers";
    String[] origCipherSuites = SystemProperties.getXroadTLSCipherSuites();

    /**
     * Constructs the test case.
     */
    public SslClientCipherSuiteSetupError() {
        requestFileName = "getstate.query";
        responseFile = "getstate.answer";
    }

    /**
     * Sets up non accepted cipher suite in use, runs getstate.query request and restores cipher suite setup
     *
     * @throws Exception
     */
    @Override
    public boolean execute(TestContext testContext) throws Exception {
        try {
            // Set not accepted cipher in use
            System.setProperty(propertyName, getNotAcceptedCipher(origCipherSuites));
            // execute test
            return super.execute(testContext);
        } finally {
            // Restore cipher suite setup for rest of the tests
            System.setProperty(propertyName, String.join(",", origCipherSuites));
        }
    }

    /**
     * Underlying "javax.net.ssl.SSLHandshakeException: no cipher suites in common" cannot be validated
     *
     * @param receivedResponse
     * @throws Exception
     */
    @Override
    protected void validateFaultResponse(Message receivedResponse)
            throws Exception {
        // fault expected
    }

    private String getNotAcceptedCipher(String[] acceptedCiphers) throws NoSuchAlgorithmException,
            KeyManagementException {
        var sslCtx = SSLContextUtil.createXroadSSLContext(globalConfProvider, keyConfProvider);
        for (String cipher : sslCtx.createSSLEngine().getSupportedCipherSuites()) {
            if (cipher.contains("_RSA_") && !ArrayUtils.contains(acceptedCiphers, cipher)) {
                return cipher;
            }
        }
        return null;
    }
}
