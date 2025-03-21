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

import ee.ria.xroad.common.message.SoapMessageImpl;
import ee.ria.xroad.common.message.SoapParserImpl;

import org.niis.xroad.proxy.core.test.Message;
import org.niis.xroad.proxy.core.test.MessageTestCase;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;

/**
 * Ensure comments are not stripped from messages.
 */
public class CommentMessage extends MessageTestCase {

    private static final String EXPECTED_COMMENT = " multiline\n        comment ";

    /**
     * Constructs the test case.
     */
    public CommentMessage() {
        requestFileName = "simple.query";
        responseFile = "simple-comment.answer";
    }

    @Override
    protected void validateNormalResponse(Message receivedResponse) throws Exception {
        SoapMessageImpl msg = (SoapMessageImpl) new SoapParserImpl().parse(receivedResponse.getContentType(),
                new ByteArrayInputStream(((SoapMessageImpl) receivedResponse.getSoap()).getBytes()));

        Node firstChild = msg.getSoap().getSOAPHeader().getFirstChild().getNextSibling();
        short nodeType = firstChild.getNodeType();
        if (nodeType != Node.COMMENT_NODE) {
            throw new Exception("Expected comment not found!");
        }
        if (!firstChild.getTextContent().equals(EXPECTED_COMMENT)) {
            throw new Exception("Comment '" + firstChild.getTextContent() + "' did not match expected value");
        }

    }
}
