package Zero_Knowledge_Vault.global.security.oauth2.adapter;

import java.util.Map;


public interface OAuth2AttributeAdapter {

    public String provider();

    public Map<String, Object> convertMap(
            String provider,
            String attributeKey,
            Map<String, Object> attributes);
}
