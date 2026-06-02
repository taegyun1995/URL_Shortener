package com.urlshortener.persistence;

import com.urlshortener.domain.ShortKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * ShortKey VO ↔ DB VARCHAR(7) 변환.
 * autoApply=true로 ShortKey 타입 필드에 자동 적용.
 */
@Converter(autoApply = true)
public class ShortKeyConverter implements AttributeConverter<ShortKey, String> {

    @Override
    public String convertToDatabaseColumn(ShortKey attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ShortKey convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ShortKey.of(dbData);
    }
}
