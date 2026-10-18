package library;

import java.util.Objects;

/** A registered library member. */
public record Member(String id, String name, MembershipType type) {

    public Member {
        if (id == null || id.isBlank())     throw new IllegalArgumentException("id required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
        Objects.requireNonNull(type, "type required");
    }
}
