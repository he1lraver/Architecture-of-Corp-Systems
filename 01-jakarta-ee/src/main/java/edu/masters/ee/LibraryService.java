package edu.masters.ee;

import edu.masters.domain.*;

import jakarta.ejb.Stateless;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

/** Транзакции и контекст JPA управляются контейнером EJB. */
@Stateless
public class LibraryService {
    @PersistenceContext(unitName = "library")
    private EntityManager entityManager;

    public List<Author> authors() {
        return entityManager
                .createQuery("select a from Author a order by a.id", Author.class)
                .getResultList();
    }

    public List<Book> books() {
        return entityManager
                .createQuery("select b from Book b join fetch b.author order by b.id", Book.class)
                .getResultList();
    }

    public Author author(long id) {
        Author author = entityManager.find(Author.class, id);
        if (author == null) throw new LibraryException(404, "Автор не найден");
        return author;
    }

    public Book book(long id) {
        Book book = entityManager.find(Book.class, id);
        if (book == null) throw new LibraryException(404, "Книга не найдена");
        return book;
    }

    public Author saveAuthor(Long id, String name, String country, int birthYear) {
        try {
            name = Rules.text(name, 120, "Имя");
            country = Rules.text(country, 80, "Страна");
            Rules.year(birthYear);
        } catch (IllegalArgumentException e) {
            throw new LibraryException(400, e.getMessage());
        }
        Author author = id == null ? new Author() : author(id);
        author.setName(name);
        author.setCountry(country);
        author.setBirthYear(birthYear);
        if (id == null) entityManager.persist(author);
        entityManager.flush();
        return author;
    }

    public Book saveBook(
            Long id, String title, int year, BigDecimal price, int stock, long authorId) {
        try {
            title = Rules.text(title, 160, "Название");
            Rules.year(year);
            Rules.price(price);
            Rules.stock(stock);
        } catch (IllegalArgumentException e) {
            throw new LibraryException(400, e.getMessage());
        }
        Author author = author(authorId);
        Book book = id == null ? new Book() : book(id);
        book.setTitle(title);
        book.setPublishedYear(year);
        book.setPrice(price);
        book.setStock(stock);
        book.setAuthor(author);
        if (id == null) entityManager.persist(book);
        entityManager.flush();
        return book;
    }

    public void deleteAuthor(long id) {
        Author author = author(id);
        if (entityManager
                        .createQuery(
                                "select count(b) from Book b where b.author.id=:id", Long.class)
                        .setParameter("id", id)
                        .getSingleResult()
                > 0) throw new LibraryException(409, "Сначала удалите или перенесите книги автора");
        entityManager.remove(author);
        entityManager.flush();
    }

    public void deleteBook(long id) {
        Book book = book(id);
        entityManager.remove(book);
        entityManager.flush();
    }
}
