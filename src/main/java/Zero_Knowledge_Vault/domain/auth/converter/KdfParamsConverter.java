package Zero_Knowledge_Vault.domain.auth.converter;

import Zero_Knowledge_Vault.domain.auth.vo.KdfParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class KdfParamsConverter implements AttributeConverter<KdfParams, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(KdfParams attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("KdfParams JSON 변환 실패", e);
        }
    }

    @Override
    public KdfParams convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, KdfParams.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("KdfParams JSON 파싱 실패", e);
        }
    }
}
