package library;

/** A catalogue entry. 'copies' is the number of copies currently on the shelf. */
public record Book(String isbn, String title, String author, int copies) {

    public Book {
        if (isbn == null || isbn.isBlank())   throw new IllegalArgumentException("isbn required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title required");
        if (author == null || author.isBlank()) throw new IllegalArgumentException("author required");
        if (copies < 0) throw new IllegalArgumentException("copies cannot be negative");
    }

    public Book withCopies(int newCopies) {
        return new Book(isbn, title, author, newCopies);
    }
}
