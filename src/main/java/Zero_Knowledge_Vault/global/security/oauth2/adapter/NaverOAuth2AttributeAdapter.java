package Zero_Knowledge_Vault.global.security.oauth2.adapter;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Component
public class NaverOAuth2AttributeAdapter implements OAuth2AttributeAdapter {
    @Override
    public String provider() {
        return "naver";
    }

    @Override
    public Map<String, Object> convertMap(
            String provider,
            String attributeKey,
            Map<String, Object> attributes){

        Map<String, Object> response =
                (Map<String, Object>) attributes.get("response");

        String id = (String) response.get(attributeKey);
        String email = (String) response.get("email");
        String name = (String) response.get("name");
        String mobile = (String) response.get("mobile");

        Map<String,Object> convertAttributes = new HashMap<>();
        convertAttributes.put("id", id);
        convertAttributes.put("provider", provider);
        convertAttributes.put("email", email);
        convertAttributes.put("name", name);
        convertAttributes.put("mobile", mobile);

        return convertAttributes;
    }
}
