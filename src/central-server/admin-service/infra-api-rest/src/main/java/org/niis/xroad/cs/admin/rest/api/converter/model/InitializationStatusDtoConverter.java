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
package org.niis.xroad.cs.admin.rest.api.converter.model;

import org.niis.xroad.cs.admin.api.dto.TokenInitStatus;
import org.niis.xroad.cs.openapi.model.InitializationStatusDto;
import org.niis.xroad.cs.openapi.model.TokenInitStatusDto;
import org.niis.xroad.restapi.converter.DtoConverter;
import org.springframework.stereotype.Component;

@Component
public class InitializationStatusDtoConverter extends DtoConverter<InitializationStatusDto,
        org.niis.xroad.cs.admin.api.dto.InitializationStatusDto> {

    @Override
    public org.niis.xroad.cs.admin.api.dto.InitializationStatusDto toDto(InitializationStatusDto source) {
        return org.niis.xroad.cs.admin.api.dto.InitializationStatusDto.builder()
                .centralServerAddress(source.getCentralServerAddress())
                .instanceIdentifier(source.getInstanceIdentifier())
                .softwareTokenInitStatus(TokenInitStatus.valueOf(source.getSoftwareTokenInitStatus().name()))
                .build();
    }

    @Override
    public InitializationStatusDto fromDto(org.niis.xroad.cs.admin.api.dto.InitializationStatusDto source) {
        return new InitializationStatusDto()
                .centralServerAddress(source.getCentralServerAddress())
                .instanceIdentifier(source.getInstanceIdentifier())
                .softwareTokenInitStatus(TokenInitStatusDto.valueOf(source.getSoftwareTokenInitStatus().name()));
    }
}
