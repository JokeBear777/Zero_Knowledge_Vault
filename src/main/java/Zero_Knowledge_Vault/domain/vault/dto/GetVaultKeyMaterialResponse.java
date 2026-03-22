package Zero_Knowledge_Vault.domain.vault.dto;

import Zero_Knowledge_Vault.domain.vault.entity.MemberVaultKeyMaterial;

import java.util.Base64;

public record GetVaultKeyMaterialResponse(
        String wrappedVaultKeyBase64,
        String saltWrapBase64,
        String wrapKdfAlgorithm,
        Object wrapKdfParams,
        Integer keyVersion,
        String status
) {

    public static GetVaultKeyMaterialResponse from(MemberVaultKeyMaterial material) {
        return new GetVaultKeyMaterialResponse(
                Base64.getEncoder().encodeToString(material.getWrappedVaultKey()),
                Base64.getEncoder().encodeToString(material.getSaltWrap()),
                material.getKdfAlgorithm().toString(),
                material.getKdfParams(),
                material.getKeyVersion(),
                material.getStatus().toString()
        );
    }
}
