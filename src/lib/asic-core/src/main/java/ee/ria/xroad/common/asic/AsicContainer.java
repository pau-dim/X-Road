/*
 * The MIT License
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
package ee.ria.xroad.common.asic;

import ee.ria.xroad.common.crypto.identifier.DigestAlgorithm;
import ee.ria.xroad.common.signature.SignatureData;
import ee.ria.xroad.common.util.MimeTypes;

import lombok.Getter;
import lombok.NonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import static ee.ria.xroad.common.ErrorCodes.translateException;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_ASIC_MANIFEST;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_ATTACHMENT;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_MANIFEST;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_MESSAGE;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_MIMETYPE;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_SIGNATURE;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_SIG_HASH_CHAIN;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_SIG_HASH_CHAIN_RESULT;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_TIMESTAMP;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_TS_HASH_CHAIN;
import static ee.ria.xroad.common.asic.AsicContainerEntries.ENTRY_TS_HASH_CHAIN_RESULT;
import static ee.ria.xroad.common.asic.AsicContainerEntries.MIMETYPE;
import static ee.ria.xroad.common.crypto.Digests.calculateDigest;
import static ee.ria.xroad.common.crypto.Digests.getAlgorithmIdentifier;
import static ee.ria.xroad.common.util.EncoderUtils.encodeBase64;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Encapsulates the creation of the ASiC-container, which is essentially a
 * ZIP file containing the message and signature.
 */
public class AsicContainer {

    /** Holds the entries in the container. */
    private final Map<String, String> entries = new HashMap<>();
    @Getter
    private final List<InputStream> attachments;
    private final Map<String, byte[]> attachmentDigests;
    @Getter
    private final long creationTime;

    AsicContainer(Map<String, String> entries, @NonNull Map<String, byte[]> attachmentDigests) throws Exception {
        this.entries.putAll(entries);
        this.attachments = List.of();
        this.attachmentDigests = Map.copyOf(attachmentDigests);
        this.creationTime = new Date().getTime();
        verifyContents();
    }


    /**
     * Creates an AsicContainer containing given message, signature and timestamp.
     * Attempts to verify its contents.
     * @param message content of the signed message
     * @param signature signature of the message
     * @param timestamp timestamp data of the message
     * @param attachments message attachments. For Rest messages, the first attachment is message body.
     * @param time logrecord creation time
     * @throws Exception if container content verification fails
     */
    public AsicContainer(String message, SignatureData signature,
                         TimestampData timestamp, List<InputStream> attachments, long time) throws Exception {
        put(ENTRY_MIMETYPE, MIMETYPE);
        put(ENTRY_MESSAGE, message);
        put(ENTRY_SIGNATURE, signature.getSignatureXml());
        put(ENTRY_SIG_HASH_CHAIN_RESULT, signature.getHashChainResult());
        put(ENTRY_SIG_HASH_CHAIN, signature.getHashChain());
        this.attachments = attachments;
        this.attachmentDigests = Map.of();
        this.creationTime = time;

        if (timestamp != null) {
            if (isNotBlank(timestamp.getHashChainResult())) { // batch ts
                put(ENTRY_TIMESTAMP, timestamp.getTimestampBase64());
            }
            put(ENTRY_TS_HASH_CHAIN, timestamp.getHashChain());
            put(ENTRY_TS_HASH_CHAIN_RESULT, timestamp.getHashChainResult());
        }

        createManifests();
        verifyContents();
    }

    /**
     * Returns the message within the container.
     * @return message within the container
     */
    public String getMessage() {
        return get(ENTRY_MESSAGE);
    }

    /**
     * Returns the signature within the container.
     * @return signature within the container
     */
    public SignatureData getSignature() {
        return new SignatureData(get(ENTRY_SIGNATURE),
                get(ENTRY_SIG_HASH_CHAIN_RESULT),
                get(ENTRY_SIG_HASH_CHAIN));
    }

    /**
     * Returns the timestamp within the container.
     * @return timestamp within the container
     */
    public TimestampData getTimestamp() {
        if (entries.containsKey(ENTRY_TS_HASH_CHAIN_RESULT)
                && entries.containsKey(ENTRY_TS_HASH_CHAIN)) {
            return new TimestampData(get(ENTRY_TIMESTAMP),
                    get(ENTRY_TS_HASH_CHAIN_RESULT),
                    get(ENTRY_TS_HASH_CHAIN));
        }

        return null;
    }

    /**
     * Gets the generated manifest of the container.
     * @return generated manifest of the container
     */
    public String getManifest() {
        return get(ENTRY_MANIFEST);
    }

    /**
     * Gets the generated manifest of the time-stamped data object.
     * @return generated manifest of the time-stamped data object.
     */
    public String getAsicManifest() {
        return get(ENTRY_ASIC_MANIFEST);
    }

    /**
     * True if the given file is an attachment.
     * @param fileName the file to check
     * @return true if the given file is an attachment, false otherwise
     */
    public boolean isAttachment(String fileName) {
        return fileName.startsWith(ENTRY_ATTACHMENT);
    }

    /**
     * True if the given file is an entry in this container.
     * @param fileName the file to check
     * @return true if the given file is an entry in this container, false otherwise
     */
    public boolean hasEntry(String fileName) {
        return entries.containsKey(AsicHelper.stripSlash(fileName));
    }

    /**
     * Gets the data for the entry with the given filename.
     * @param fileName the file for which to get the entry data
     * @return input stream containing the data for the entry with the given filename
     */
    public InputStream getEntry(String fileName) {
        String data = get(AsicHelper.stripSlash(fileName));
        return data != null ? new ByteArrayInputStream(
                data.getBytes(StandardCharsets.UTF_8)) : null;
    }

    /**
     * Gets the string contents of the entry with the given filename.
     * @param fileName the file for which to get the string contents
     * @return string contents of the entry with the given filename
     */
    public String getEntryAsString(String fileName) {
        return get(AsicHelper.stripSlash(fileName));
    }

    public byte[] getAttachmentDigest(String fileName) {
        return attachmentDigests.get(AsicHelper.stripSlash(fileName));
    }

    /**
     * Create a ASiC container from the given input stream.
     * @param is the stream containing the container ZIP data
     * @return the ASiC container that was read from the input stream
     * @throws Exception if errors occurred when reading ZIP entries from the stream
     */
    public static AsicContainer read(InputStream is) throws Exception {
        return AsicHelper.read(is);
    }

    /**
     * Write this container to the given output stream in ZIP format.
     * @param out the stream for writing container
     * @throws Exception if errors occurred when writing ZIP entries
     */
    public void write(OutputStream out) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            AsicHelper.write(this, zip);
        }
    }

    private void createManifests() throws Exception {
        createOpenDocumentManifest();
        createAsicManifest();
    }

    private void createOpenDocumentManifest() {
        OpenDocumentManifestBuilder b = new OpenDocumentManifestBuilder();
        for (String entryName : entries.keySet()) {
            // ignore mimetype and files in META-INF
            if (entryName.equalsIgnoreCase(ENTRY_MIMETYPE)
                    || entryName.startsWith("META-INF")) {
                continue;
            }

            b.addFile(entryName, MimeTypes.TEXT_XML); // assume files are XML
        }

        if (attachments != null) {
            for (int i = 1; i <= attachments.size(); i++) {
                b.addFile(ENTRY_ATTACHMENT + i, MimeTypes.BINARY);
            }
        }

        put(ENTRY_MANIFEST, b.build());
    }

    private void createAsicManifest() throws Exception {
        String tsHashChainResult = get(ENTRY_TS_HASH_CHAIN_RESULT);
        if (tsHashChainResult == null) {
            return;
        }

        AsicManifestBuilder b = new AsicManifestBuilder();
        b.setSigReference(ENTRY_TIMESTAMP, "vnd.etsi.timestamp-token");

        DigestAlgorithm algoId = DigestAlgorithm.SHA512;
        byte[] digest = calculateDigest(getAlgorithmIdentifier(algoId),
                tsHashChainResult.getBytes(StandardCharsets.UTF_8));
        b.addDataObjectReference(ENTRY_TS_HASH_CHAIN_RESULT,
                MimeTypes.TEXT_XML, algoId,
                encodeBase64(digest));

        put(ENTRY_ASIC_MANIFEST, b.build());
    }

    private void verifyContents() throws Exception {
        AsicHelper.verifyMimeType(get(ENTRY_MIMETYPE));
        AsicHelper.verifyMessage(get(ENTRY_MESSAGE));
        AsicHelper.verifySignature(get(ENTRY_SIGNATURE),
                get(ENTRY_SIG_HASH_CHAIN_RESULT), get(ENTRY_SIG_HASH_CHAIN));

        AsicHelper.verifyTimestamp(get(ENTRY_TIMESTAMP),
                get(ENTRY_TS_HASH_CHAIN_RESULT), get(ENTRY_TS_HASH_CHAIN));

        AsicHelper.verifyManifest(get(ENTRY_MANIFEST), get(ENTRY_ASIC_MANIFEST));
    }

    String get(String entryName) {
        if (entryName.equals(ENTRY_TIMESTAMP)) {
            return getTimestampValueBase64();
        }
        return entries.get(entryName);
    }

    String getTimestampValueBase64() {
        String timestampValue = entries.get(ENTRY_TIMESTAMP);
        if (timestampValue == null) {
            try {
                timestampValue = AsicHelper.readTimestampFromSignatureXml(
                        getSignature().getSignatureXml());
            } catch (Exception e) {
                throw translateException(e);
            }
        }

        return timestampValue;
    }

    void put(String entryName, String data) {
        if (isNotBlank(data)) {
            this.entries.put(entryName, data);
        }
    }
}
