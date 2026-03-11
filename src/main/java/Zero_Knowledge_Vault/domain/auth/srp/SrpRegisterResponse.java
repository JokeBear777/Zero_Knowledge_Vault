package Zero_Knowledge_Vault.domain.auth.srp;

import Zero_Knowledge_Vault.global.crypto.kdf.KdfParams;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SrpRegisterResponse(
        @JsonProperty("saltAuth") String saltAuth,
        @JsonProperty("N") String N,
        @JsonProperty("g") String g,
        @JsonProperty("kdfAlgorithm") String kdfAlgorithm,
        @JsonProperty("kdfParams") KdfParams kdfParams
) {}
