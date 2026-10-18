package library;

public class InMemoryLoanRepository extends InMemoryRepository<Loan, String> implements LoanRepository {
    public InMemoryLoanRepository() {
        super(Loan::id);
    }
}
