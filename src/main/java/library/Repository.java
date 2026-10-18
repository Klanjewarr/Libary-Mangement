package library;

import java.util.List;
import java.util.Optional;

/** Generic storage contract. Services depend on this interface, never on a concrete store. */
public interface Repository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
}
