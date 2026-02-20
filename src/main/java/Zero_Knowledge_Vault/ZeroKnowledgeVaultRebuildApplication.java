package Zero_Knowledge_Vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ZeroKnowledgeVaultRebuildApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZeroKnowledgeVaultRebuildApplication.class, args);
	}

}
