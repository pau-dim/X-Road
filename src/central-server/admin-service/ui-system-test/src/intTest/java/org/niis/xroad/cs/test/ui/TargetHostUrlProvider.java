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
package org.niis.xroad.cs.test.ui;

import com.nortal.test.core.services.TestableApplicationInfoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TargetHostUrlProvider {
    private final TestableApplicationInfoProvider testableApplicationInfoProvider;

    @Value("${test-automation.custom.central-server-url-override:#{null}}")
    private String centralServerUrlOverride;

    public TargetHostUrlProvider(TestableApplicationInfoProvider testableApplicationInfoProvider) {
        this.testableApplicationInfoProvider = testableApplicationInfoProvider;
    }

    public String getUrl() {
        if (isUrlOverridden()) {
            log.warn("Using overridden testable target url {}", centralServerUrlOverride);
            return centralServerUrlOverride;
        } else {

            return String.format("https://%s:%s",
                    testableApplicationInfoProvider.getHost(),
                    testableApplicationInfoProvider.getPort());
        }
    }

    public boolean isUrlOverridden() {
        return centralServerUrlOverride != null;
    }
}
