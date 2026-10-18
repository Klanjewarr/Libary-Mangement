package library;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibraryServiceTest {

    /** Test clock whose date can be moved forward. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        void advanceDays(long d) { now = now.plusSeconds(d * 86400); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    static final String ISBN = "111";
    MutableClock clock;
    LibraryService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new LibraryService(new InMemoryRepository<>(Book::isbn), new InMemoryRepository<>(Member::id),
                new InMemoryRepository<>(Fine.OverdueFine::loanId), new InMemoryLoanRepository(), clock);
        service.addBook(new Book(ISBN, "Clean Code", "Robert Martin", 1));
        service.registerMember(new Member("M1", "Asha", MembershipType.STANDARD));
    }

    // ---- records & validation ----
    @Test void bookRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> new Book(" ", "t", "a", 1));
        assertThrows(IllegalArgumentException.class, () -> new Book("i", null, "a", 1));
        assertThrows(IllegalArgumentException.class, () -> new Book("i", "t", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new Book("i", "t", "a", -1));
    }

    @Test void memberRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> new Member("", "n", MembershipType.STANDARD));
        assertThrows(IllegalArgumentException.class, () -> new Member("i", " ", MembershipType.STANDARD));
        assertThrows(NullPointerException.class, () -> new Member("i", "n", null));
    }

    @Test void membershipRules() {
        assertEquals(3, MembershipType.STANDARD.maxLoans());
        assertEquals(21, MembershipType.PREMIUM.loanDays());
        assertEquals(1, MembershipType.PREMIUM.finePerDay());
    }

    // ---- addBook / registerMember ----
    @Test void addingSameIsbnMergesCopies() {
        Book b = service.addBook(new Book(ISBN, "Clean Code", "Robert Martin", 2));
        assertEquals(3, b.copies());
    }

    @Test void duplicateMemberIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> service.registerMember(new Member("M1", "Other", MembershipType.PREMIUM)));
    }

    // ---- borrow ----
    @Test void borrowSucceedsAndReducesStock() {
        BorrowResult r = service.borrow("M1", ISBN);
        Loan loan = assertInstanceOf(BorrowResult.Success.class, r).loan();
        assertEquals(LocalDate.of(2026, 10, 15), loan.dueOn());
        assertTrue(loan.isActive());
        assertTrue(loan.returnedOn().isEmpty());
        assertEquals(BorrowResult.Reason.NO_COPIES_AVAILABLE,
                assertInstanceOf(BorrowResult.Rejected.class, service.borrow("M1", ISBN)).reason());
    }

    @Test void borrowRejectsUnknownMemberAndBook() {
        assertEquals(BorrowResult.Reason.UNKNOWN_MEMBER,
                ((BorrowResult.Rejected) service.borrow("nope", ISBN)).reason());
        assertEquals(BorrowResult.Reason.UNKNOWN_BOOK,
                ((BorrowResult.Rejected) service.borrow("M1", "999")).reason());
    }

    @Test void borrowRespectsLoanLimit() {
        service.addBook(new Book(ISBN, "Clean Code", "Robert Martin", 5));
        for (int i = 0; i < 3; i++) assertInstanceOf(BorrowResult.Success.class, service.borrow("M1", ISBN));
        assertEquals(BorrowResult.Reason.LOAN_LIMIT_REACHED,
                ((BorrowResult.Rejected) service.borrow("M1", ISBN)).reason());
    }

    // ---- return & fines ----
    @Test void returnOnTimeHasNoFine() {
        Loan loan = ((BorrowResult.Success) service.borrow("M1", ISBN)).loan();
        clock.advanceDays(14);
        Fine fine = service.returnBook(loan.id()).orElseThrow();
        assertInstanceOf(Fine.NoFine.class, fine);
        assertEquals(0, service.totalFines("M1"));
    }

    @Test void lateReturnCreatesFineAndRestocksBook() {
        Loan loan = ((BorrowResult.Success) service.borrow("M1", ISBN)).loan();
        clock.advanceDays(19);
        Fine.OverdueFine fine = assertInstanceOf(Fine.OverdueFine.class, service.returnBook(loan.id()).orElseThrow());
        assertEquals(5, fine.daysLate());
        assertEquals(10, fine.amount());
        assertEquals(10, service.totalFines("M1"));
        assertInstanceOf(BorrowResult.Success.class, service.borrow("M1", ISBN));
    }

    @Test void returnUnknownOrAlreadyReturnedLoanIsEmpty() {
        assertTrue(service.returnBook("nope").isEmpty());
        Loan loan = ((BorrowResult.Success) service.borrow("M1", ISBN)).loan();
        assertTrue(service.returnBook(loan.id()).isPresent());
        assertTrue(service.returnBook(loan.id()).isEmpty());
    }

    // ---- pattern-matching switch descriptions ----
    @Test void describeBorrowResults() {
        Loan loan = ((BorrowResult.Success) service.borrow("M1", ISBN)).loan();
        assertTrue(LibraryService.describe(new BorrowResult.Success(loan)).startsWith("Borrowed 111"));
        assertEquals("Member not found", LibraryService.describe(new BorrowResult.Rejected(BorrowResult.Reason.UNKNOWN_MEMBER)));
        assertEquals("Book not found", LibraryService.describe(new BorrowResult.Rejected(BorrowResult.Reason.UNKNOWN_BOOK)));
        assertEquals("No copies available", LibraryService.describe(new BorrowResult.Rejected(BorrowResult.Reason.NO_COPIES_AVAILABLE)));
        assertEquals("Loan limit reached", LibraryService.describe(new BorrowResult.Rejected(BorrowResult.Reason.LOAN_LIMIT_REACHED)));
    }

    @Test void describeFines() {
        assertEquals("Returned on time, no fine", LibraryService.describe(new Fine.NoFine("L1")));
        assertEquals("3 day(s) late, fine Rs. 6", LibraryService.describe(new Fine.OverdueFine("L1", "M1", 3, 6)));
    }

    @Test void mainRuns() {
        Main.main(new String[0]);
    }
}
