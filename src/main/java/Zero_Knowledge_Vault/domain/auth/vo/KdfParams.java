package Zero_Knowledge_Vault.domain.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KdfParams {

    private int memory;
    private int iterations;
    private int parallelism;

}
