package Zero_Knowledge_Vault.infra.security.oauth2.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuth2AdapterRegistry {

    private final Map<String, OAuth2AttributeAdapter> adapterMap;

    public OAuth2AdapterRegistry(List<OAuth2AttributeAdapter> adapters) {

        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(
                        OAuth2AttributeAdapter::provider,
                        Function.identity()
                ));
    }

    public OAuth2AttributeAdapter get(String registrationId) {
        OAuth2AttributeAdapter adapter = adapterMap.get(registrationId);

        if (adapter == null) {
            throw new IllegalArgumentException(
                    "No adapter found for registrationId: " + registrationId
            );
        }
        return adapter;
    }
}
