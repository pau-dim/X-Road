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

package org.niis.xroad.proxy.test.glue;

import ee.ria.xroad.common.crypto.identifier.DigestAlgorithm;
import ee.ria.xroad.common.crypto.identifier.KeyAlgorithm;
import ee.ria.xroad.common.hashchain.HashChainReferenceResolver;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.signature.MessagePart;
import ee.ria.xroad.common.signature.SignatureData;
import ee.ria.xroad.common.util.MessageFileNames;

import io.cucumber.java.en.Step;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.niis.xroad.common.test.glue.BaseStepDefs;
import org.niis.xroad.globalconf.impl.signature.SignatureVerifier;
import org.niis.xroad.keyconf.KeyConfProvider;
import org.niis.xroad.keyconf.impl.CachingKeyConfImpl;
import org.niis.xroad.proxy.core.conf.SigningCtxProvider;
import org.niis.xroad.proxy.core.conf.SigningCtxProviderImpl;
import org.niis.xroad.proxy.core.signature.BatchSigner;
import org.niis.xroad.proxy.core.signature.SignatureBuilder;
import org.niis.xroad.proxy.test.hook.BatchSignerInitHook;
import org.niis.xroad.serverconf.impl.ServerConfImpl;
import org.niis.xroad.signer.api.dto.KeyInfo;
import org.niis.xroad.signer.api.dto.TokenInfo;
import org.niis.xroad.signer.client.SignerRpcClient;
import org.niis.xroad.signer.proto.CertificateRequestFormat;
import org.niis.xroad.signer.protocol.dto.KeyUsageInfo;
import org.niis.xroad.test.globalconf.TestGlobalConfImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static ee.ria.xroad.common.crypto.Digests.calculateDigest;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Slf4j
@SuppressWarnings("checkstyle:MagicNumber")
public class ProxyStepDefs extends BaseStepDefs {
    private String scenarioKeyId;

    private final SignerRpcClient signerRpcClient = BatchSignerInitHook.signerRpcClient;

    @Step("tokens are listed")
    public void listTokens() {
        var tokens = signerRpcClient.getTokens();
        testReportService.attachJson("Tokens", tokens.toArray());
    }

    @Step("token is initialized with pin {string}")
    public void initToken(String pin) {
        signerRpcClient.initSoftwareToken(pin.toCharArray());
    }

    @Step("token with id {string} is logged in with pin {string}")
    public void tokenIsActivatedWithPin(String tokenId, String pin) {
        signerRpcClient.activateToken(tokenId, pin.toCharArray());
    }

    @Step("new key {string} generated for token with id {string}")
    public void newKeyGeneratedForToken(String keyLabel, String tokenId) {
        final KeyInfo keyInfo = signerRpcClient.generateKey(tokenId, keyLabel, KeyAlgorithm.RSA);
        scenarioKeyId = keyInfo.getId();

        testReportService.attachJson("keyInfo", keyInfo);
    }

    @Step("the {} cert request is generated with created key for client {string}")
    public void certRequestIsGeneratedForTokenKey(String keyUsage, String client) throws Exception {
        var clientId = getClientId(client);
        var subjectName = format("C=%s, O=%s, CN=%s",
                clientId.getXRoadInstance(),
                clientId.getMemberClass(),
                clientId.getMemberCode());

        SignerRpcClient.GeneratedCertRequestInfo csrInfo = signerRpcClient.generateCertRequest(scenarioKeyId, clientId,
                KeyUsageInfo.valueOf(keyUsage), subjectName, CertificateRequestFormat.DER);


        File csrFile = File.createTempFile("tmp", keyUsage.toLowerCase() + "_csr" + System.currentTimeMillis());
        FileUtils.writeByteArrayToFile(csrFile, csrInfo.certRequest());
        putStepData(StepDataKey.DOWNLOADED_FILE, csrFile);
    }

    @Step("Generated certificate with initial status {string} is imported for client {string}")
    public void importCertFromFile(String initialStatus, String client) throws Exception {
        final Optional<File> cert = getStepData(StepDataKey.CERT_FILE);
        final ClientId.Conf clientId = getClientId(client);
        final byte[] certBytes = FileUtils.readFileToByteArray(cert.orElseThrow());

        scenarioKeyId = signerRpcClient.importCert(certBytes, initialStatus, clientId);
    }

    @Step("token info can be retrieved by key id")
    public void tokenInfoCanBeRetrievedByKeyId() throws Exception {
        final TokenInfo tokenForKeyId = signerRpcClient.getTokenForKeyId(this.scenarioKeyId);
        testReportService.attachJson("tokenInfo", tokenForKeyId);
        assertThat(tokenForKeyId).isNotNull();
    }


    @Step("client {string} signs the messages {} random messages using {} threads")
    public void execBatchSign(String client, int count, int threads) throws Exception {
        exec(client, count, threads);
    }

    private void exec(String client, int count, int threads) throws InterruptedException {
        final var clientId = getClientId(client);
        final var signingCtx = createSigningCtxProvider().createSigningCtx(clientId);

        List<String> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add("random-msg:" + RandomStringUtils.secure().nextAlphabetic(100, 1000));
        }

        List<Callable<BatchSignResult>> callables = new ArrayList<>();
        for (final String message : messages) {
            callables.add(() -> {
                try {
                    MessagePart hashPart = new MessagePart(MessageFileNames.MESSAGE, DigestAlgorithm.SHA512,
                            calculateDigest(DigestAlgorithm.SHA512, message.getBytes()), message.getBytes());

                    List<MessagePart> hashes = Collections.singletonList(hashPart);

                    SignatureBuilder builder = new SignatureBuilder();
                    builder.addPart(hashPart);
                    SignatureData signatureData = signingCtx.buildSignature(builder);

                    return new BatchSignResult(clientId, message, signatureData, hashes);

                } catch (Exception e) {
                    log.error("Error", e);
                    return new BatchSignResult(clientId, message, null, null);
                }

            });
        }

        List<Future<BatchSignResult>> results = invokeCallables(callables, threads);

        final AtomicInteger batchSignatureDetectCounter = new AtomicInteger();
        for (Future<BatchSignResult> result : results) {
            try {
                var signResult = result.get();

                assertThat(signResult.signatureData()).isNotNull();

                verify(signResult);

                assertThat(signResult.signatureData().getSignatureXml()).isNotEmpty();

                if (signResult.signatureData().isBatchSignature()) {
                    batchSignatureDetectCounter.incrementAndGet();
                }
            } catch (Exception e) {
                fail("Verification has failed.", e);
            }
        }

        if (batchSignatureDetectCounter.get() == 0) {
            fail("Batch signature was not detected.");
        } else {
            testReportService.attachText("Batch signature was triggered " + batchSignatureDetectCounter.get() + " times", "");
        }
    }

    @SneakyThrows
    private KeyConfProvider createKeyConf() {
        var globalConf = new TestGlobalConfImpl();
        var serverConf = new ServerConfImpl(globalConf);
        return new CachingKeyConfImpl(globalConf, serverConf, signerRpcClient);
    }

    @SneakyThrows
    private SigningCtxProvider createSigningCtxProvider() {
        var globalConf = new TestGlobalConfImpl();
        var serverConf = new ServerConfImpl(globalConf);
        var keyconf = new CachingKeyConfImpl(globalConf, serverConf, signerRpcClient);

        return new SigningCtxProviderImpl(globalConf, keyconf, new BatchSigner(signerRpcClient));
    }

    private List<Future<BatchSignResult>> invokeCallables(List<Callable<BatchSignResult>> callables, int threads)
            throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        try {
            return executorService.invokeAll(callables);
        } finally {
            executorService.shutdown();
        }
    }

    private record BatchSignResult(ClientId.Conf clientId, String message, SignatureData signatureData,
                                   List<MessagePart> messageParts) {
    }

    private static void verify(final BatchSignResult batchSignResult)
            throws Exception {
        var globalConfProvider = new TestGlobalConfImpl();
        SignatureVerifier verifier = new SignatureVerifier(globalConfProvider, batchSignResult.signatureData());
        verifier.addParts(batchSignResult.messageParts());

        HashChainReferenceResolver resolver = new HashChainReferenceResolver() {
            @Override
            public InputStream resolve(String uri) {
                return switch (uri) {
                    case MessageFileNames.SIG_HASH_CHAIN ->
                            new ByteArrayInputStream(batchSignResult.signatureData().getHashChain().getBytes(StandardCharsets.UTF_8));
                    case MessageFileNames.MESSAGE -> new ByteArrayInputStream(batchSignResult.message().getBytes(StandardCharsets.UTF_8));
                    default -> null;
                };
            }

            @Override
            public boolean shouldResolve(String uri, byte[] digestValue) {
                return true;
            }
        };

        if (batchSignResult.signatureData().getHashChainResult() != null) {
            verifier.setHashChainResourceResolver(resolver);
        }

        verifier.verify(batchSignResult.clientId(), new Date());
    }

    private ClientId.Conf getClientId(String client) {
        final String[] parts = client.split(":");
        return ClientId.Conf.create(parts[0], parts[1], parts[2]);
    }
}
