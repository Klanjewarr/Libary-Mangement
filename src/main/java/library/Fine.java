package library;

/** Outcome of assessing a returned loan: either nothing owed, or an overdue charge. */
public sealed interface Fine permits Fine.NoFine, Fine.OverdueFine {

    String loanId();

    record NoFine(String loanId) implements Fine { }

    record OverdueFine(String loanId, String memberId, long daysLate, long amount) implements Fine { }
}
