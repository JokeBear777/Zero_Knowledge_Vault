package Zero_Knowledge_Vault.domain.member.type;

public enum MemberRole {
    ADMIN,
    USER,
    GUEST,
    INACTIVE;


    @Override
    public String toString() {
        return "ROLE_" + name();
    }
}
