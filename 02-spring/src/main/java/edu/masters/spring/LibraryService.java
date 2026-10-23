package edu.masters.spring;

import edu.masters.domain.*;

import jakarta.persistence.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class LibraryService {
    @PersistenceContext private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Author> authors() {
        return entityManager.createQuery("from Author order by id", Author.class).getResultList();
    }

    @Transactional(readOnly = true)
    public List<Book> books() {
        return entityManager
                .createQuery("select b from Book b join fetch b.author order by b.id", Book.class)
                .getResultList();
    }

    private <T> T find(Class<T> type, long id) {
        T entity = entityManager.find(type, id);
        if (entity == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена");
        return entity;
    }

    public void saveAuthor(Long id, String name, String country, int year) {
        name = Rules.text(name, 120, "Имя");
        country = Rules.text(country, 80, "Страна");
        Rules.year(year);
        Author author = id == null ? new Author() : find(Author.class, id);
        author.setName(name);
        author.setCountry(country);
        author.setBirthYear(year);
        if (id == null) entityManager.persist(author);
    }

    public void saveBook(
            Long id, String title, int year, BigDecimal price, int stock, long authorId) {
        title = Rules.text(title, 160, "Название");
        Rules.year(year);
        Rules.price(price);
        Rules.stock(stock);
        Author author = find(Author.class, authorId);
        Book book = id == null ? new Book() : find(Book.class, id);
        book.setTitle(title);
        book.setPublishedYear(year);
        book.setPrice(price);
        book.setStock(stock);
        book.setAuthor(author);
        if (id == null) entityManager.persist(book);
    }

    public void deleteAuthor(long id) {
        Author author = find(Author.class, id);
        if (entityManager
                        .createQuery(
                                "select count(b) from Book b where b.author.id=:id", Long.class)
                        .setParameter("id", id)
                        .getSingleResult()
                > 0)
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Сначала удалите или перенесите книги автора");
        entityManager.remove(author);
    }

    public void deleteBook(long id) {
        entityManager.remove(find(Book.class, id));
    }
}
