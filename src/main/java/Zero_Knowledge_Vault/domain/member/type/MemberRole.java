package Zero_Knowledge_Vault.domain.member.type;

public enum MemberRole {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_GUEST,
    ROLE_INACTIVE;


    @Override
    public String toString() {
        return name();
    }
}
