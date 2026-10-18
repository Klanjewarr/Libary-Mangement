package library;

import java.time.Clock;

public class Main {
    public static void main(String[] args) {
        var service = new LibraryService(
                new InMemoryRepository<>(Book::isbn), new InMemoryRepository<>(Member::id),
                new InMemoryRepository<>(Fine.OverdueFine::loanId), new InMemoryLoanRepository(),
                Clock.systemDefaultZone());
        service.addBook(new Book("978-0134685991", "Effective Java", "Joshua Bloch", 2));
        service.registerMember(new Member("M1", "Kanchan", MembershipType.STANDARD));
        var result = service.borrow("M1", "978-0134685991");
        System.out.println(LibraryService.describe(result));
        if (result instanceof BorrowResult.Success(Loan loan)) {
            service.returnBook(loan.id()).ifPresent(f -> System.out.println(LibraryService.describe(f)));
        }
    }
}
