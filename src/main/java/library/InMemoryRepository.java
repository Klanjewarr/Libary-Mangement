package library;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Map-backed repository used for the lab and for tests. */
public class InMemoryRepository<T, ID> implements Repository<T, ID> {

    private final Map<ID, T> store = new LinkedHashMap<>();
    private final Function<T, ID> idOf;

    public InMemoryRepository(Function<T, ID> idOf) {
        this.idOf = idOf;
    }

    @Override public T save(T entity)          { store.put(idOf.apply(entity), entity); return entity; }
    @Override public Optional<T> findById(ID id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<T> findAll()         { return new ArrayList<>(store.values()); }
}
