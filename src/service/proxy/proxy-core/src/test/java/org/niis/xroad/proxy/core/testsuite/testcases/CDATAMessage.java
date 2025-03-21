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

import ee.ria.xroad.common.message.SoapHeader;
import ee.ria.xroad.common.message.SoapMessageImpl;

import jakarta.xml.soap.SOAPElement;
import org.niis.xroad.proxy.core.test.Message;
import org.niis.xroad.proxy.core.test.MessageTestCase;

import javax.xml.namespace.QName;

/**
 * Client sends and receives a message with a CDATA block.
 * Result: all CDATA blocks remain intact.
 */
public class CDATAMessage extends MessageTestCase {

    private static final String CDATA =
            "<a><b><c>12345678</c></b></a>";

    /**
     * Constructs the test case.
     */
    public CDATAMessage() {
        requestFileName = "cdata.query";
        responseFile = "cdata.answer";
    }

    @Override
    protected void onServiceReceivedRequest(Message receivedRequest)
            throws Exception {
        validateCDATA(receivedRequest);
    }

    @Override
    protected void validateNormalResponse(Message receivedResponse)
            throws Exception {
        validateCDATA(receivedResponse);

    }

    private void validateCDATA(Message receivedResponse) throws Exception {
        SoapMessageImpl soap = (SoapMessageImpl) receivedResponse.getSoap();
        SOAPElement cdata = ((SOAPElement) soap.getSoap().getSOAPHeader()
                .getChildElements(new QName(SoapHeader.NS_XROAD, "xml"))
                .next());
        if (!cdata.getTextContent().equals(CDATA)) {
            throw new Exception("CDATA block is incorrect '"
                    + cdata.getTextContent() + "', should be " + CDATA);
        }
    }
}
