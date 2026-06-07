package Zero_Knowledge_Vault.domain.share.policy;

public interface ShareKeyPolicy {

    String supportedAlgorithm();

    int maxKeyTextLength();
}