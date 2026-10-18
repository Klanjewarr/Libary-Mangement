package library;

import java.util.List;

/** Loan-specific queries on top of the generic repository. */
public interface LoanRepository extends Repository<Loan, String> {

    default List<Loan> findActiveByMember(String memberId) {
        return findAll().stream()
                .filter(l -> l.memberId().equals(memberId) && l.isActive())
                .toList();
    }
}
