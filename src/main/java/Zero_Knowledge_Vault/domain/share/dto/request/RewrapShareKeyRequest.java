package Zero_Knowledge_Vault.domain.share.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Batch request to re-wrap existing shared item wrappers after the caller regenerates a member share key.")
public record RewrapShareKeyRequest(
        @Schema(description = "Current ACTIVE share key version that the client wrapped against", example = "2")
        @NotNull
        Integer targetKeyVersion,
        @Schema(description = "Shared item wrappers to update with the new public key")
        @NotNull
        @Size(min = 1)
        @Valid
        List<RewrapItemRequest> items
) {
}
