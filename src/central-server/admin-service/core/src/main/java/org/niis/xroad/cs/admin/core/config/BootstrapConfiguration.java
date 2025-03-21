/*
 * The MIT License
 * <p>
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.cs.admin.core.config;

import ee.ria.xroad.common.util.process.ExternalProcessRunner;

import jakarta.servlet.Filter;
import org.niis.xroad.common.api.throttle.IpThrottlingFilter;
import org.niis.xroad.globalconf.spring.GlobalConfBeanConfig;
import org.niis.xroad.globalconf.spring.GlobalConfRefreshJobConfig;
import org.niis.xroad.restapi.config.AddCorrelationIdFilter;
import org.niis.xroad.restapi.config.AllowedFilesConfig;
import org.niis.xroad.restapi.service.FileVerifier;
import org.niis.xroad.signer.client.SignerRpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Import({GlobalConfBeanConfig.class,
        GlobalConfRefreshJobConfig.class})
@Configuration
public class BootstrapConfiguration {

    @Bean
    @Profile("!int-test")
    SignerRpcClient signerRpcClient() {
        return new SignerRpcClient();
    }

    @Bean
    public ExternalProcessRunner externalProcessRunner() {
        return new ExternalProcessRunner();
    }

    @Bean
    public FileVerifier fileVerifier(final AllowedFilesConfig allowedFilesConfig) {
        return new FileVerifier(allowedFilesConfig);
    }

    @Bean
    @Order(AddCorrelationIdFilter.CORRELATION_ID_FILTER_ORDER + 3)
    @ConditionalOnProperty(
            value = "xroad.admin-service.rate-limit-enabled",
            havingValue = "true", matchIfMissing = true)
    public Filter ipThrottlingFilter(AdminServiceProperties properties) {
        return new IpThrottlingFilter(properties);
    }
}

