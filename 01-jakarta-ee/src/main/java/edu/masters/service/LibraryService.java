package edu.masters.service;

import edu.masters.model.Author;
import edu.masters.model.Book;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class LibraryService {
    @PersistenceContext(unitName = "library")
    private EntityManager em;

    public List<Author> getAuthors() {
        return em.createQuery("SELECT a FROM Author a ORDER BY a.id", Author.class)
                .getResultList();
    }

    public Author getAuthor(long id) {
        Author author = em.find(Author.class, id);
        if (author == null) {
            throw new LibraryException("Автор не найден");
        }
        return author;
    }

    public void saveAuthor(Long id, String name, String country) {
        if (name == null || name.isBlank() || name.length() > 120
                || country == null || country.isBlank() || country.length() > 80) {
            throw new LibraryException("Заполните имя автора и страну в пределах длины полей");
        }

        Author author;
        if (id == null) {
            author = new Author();
        } else {
            author = getAuthor(id);
        }
        author.setName(name.trim());
        author.setCountry(country.trim());
        if (id == null) {
            em.persist(author);
        }
        // Найденная через em.find сущность сохраняется при завершении транзакции.
    }

    public void deleteAuthor(long id) {
        Author author = getAuthor(id);
        long count = em.createQuery("SELECT COUNT(b) FROM Book b WHERE b.author.id = :id", Long.class)
                .setParameter("id", id).getSingleResult();
        if (count > 0) {
            throw new LibraryException("Сначала удалите книги автора или выберите для них другого автора");
        }
        em.remove(author);
    }

    public List<Book> getBooks() {
        return em.createQuery("SELECT b FROM Book b JOIN FETCH b.author ORDER BY b.id", Book.class)
                .getResultList();
    }

    public Book getBook(long id) {
        Book book = em.find(Book.class, id);
        if (book == null) {
            throw new LibraryException("Книга не найдена");
        }
        return book;
    }

    public void saveBook(Long id, String title, int year, long authorId) {
        if (title == null || title.isBlank() || title.length() > 160 || year < 1 || year > 2100) {
            throw new LibraryException("Введите название и год издания от 1 до 2100");
        }
        Author author = getAuthor(authorId);
        Book book;
        if (id == null) {
            book = new Book();
        } else {
            book = getBook(id);
        }
        book.setTitle(title.trim());
        book.setPublishedYear(year);
        book.setAuthor(author);
        if (id == null) {
            em.persist(book);
        }
    }

    public void deleteBook(long id) {
        em.remove(getBook(id));
    }
}
