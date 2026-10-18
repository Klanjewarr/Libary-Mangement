package library;

import java.time.LocalDate;
import java.util.Optional;

/** A book borrowed by a member. 'returnedDate' is null while the loan is still active. */
public record Loan(String id, String isbn, String memberId,
                   LocalDate borrowedOn, LocalDate dueOn, LocalDate returnedDate) {

    public Optional<LocalDate> returnedOn() {
        return Optional.ofNullable(returnedDate);
    }

    public boolean isActive() {
        return returnedDate == null;
    }

    public Loan closedOn(LocalDate date) {
        return new Loan(id, isbn, memberId, borrowedOn, dueOn, date);
    }
}
