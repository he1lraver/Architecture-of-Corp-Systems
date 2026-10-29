package edu.masters.ee;

import edu.masters.domain.*;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.xml.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Path("/")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class Api {
    @EJB private LibraryService service;

    @XmlRootElement(name = "author")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AuthorData {
        public Long id;
        public String name, country;
        public int birthYear;

        public AuthorData() {}

        AuthorData(Author author) {
            id = author.getId();
            name = author.getName();
            country = author.getCountry();
            birthYear = author.getBirthYear();
        }
    }

    @XmlRootElement(name = "book")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BookData {
        public Long id;
        public String title;
        public int publishedYear, stock;
        public BigDecimal price;
        public long authorId;

        public BookData() {}

        BookData(Book book) {
            id = book.getId();
            title = book.getTitle();
            publishedYear = book.getPublishedYear();
            stock = book.getStock();
            price = book.getPrice();
            authorId = book.getAuthor().getId();
        }
    }

    @XmlRootElement(name = "authors")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Authors {
        @XmlElement(name = "author")
        public List<AuthorData> items;

        public Authors() {}

        Authors(List<Author> data) {
            items = data.stream().map(AuthorData::new).toList();
        }
    }

    @XmlRootElement(name = "books")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Books {
        @XmlElement(name = "book")
        public List<BookData> items;

        public Books() {}

        Books(List<Book> data) {
            items = data.stream().map(BookData::new).toList();
        }
    }

    @XmlRootElement(name = "error")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Problem {
        public String message;

        public Problem() {}

        public Problem(String message) {
            this.message = message;
        }
    }

    @GET
    @Path("authors")
    public Authors authors() {
        return new Authors(service.authors());
    }

    @GET
    @Path("authors/{id}")
    public AuthorData author(@PathParam("id") long id) {
        return new AuthorData(service.author(id));
    }

    @POST
    @Path("authors")
    public Response createAuthor(AuthorData author, @Context UriInfo uri) {
        AuthorData saved =
                new AuthorData(
                        service.saveAuthor(null, author.name, author.country, author.birthYear));
        return Response.created(uri.getAbsolutePathBuilder().path(saved.id.toString()).build())
                .entity(saved)
                .build();
    }

    @PUT
    @Path("authors/{id}")
    public AuthorData updateAuthor(@PathParam("id") long id, AuthorData author) {
        return new AuthorData(
                service.saveAuthor(id, author.name, author.country, author.birthYear));
    }

    @DELETE
    @Path("authors/{id}")
    public Response deleteAuthor(@PathParam("id") long id) {
        service.deleteAuthor(id);
        return Response.noContent().build();
    }

    @GET
    @Path("books")
    public Books books() {
        return new Books(service.books());
    }

    @GET
    @Path("books/{id}")
    public BookData book(@PathParam("id") long id) {
        return new BookData(service.book(id));
    }

    @POST
    @Path("books")
    public Response createBook(BookData book, @Context UriInfo uri) {
        BookData saved =
                new BookData(
                        service.saveBook(
                                null,
                                book.title,
                                book.publishedYear,
                                book.price,
                                book.stock,
                                book.authorId));
        return Response.created(uri.getAbsolutePathBuilder().path(saved.id.toString()).build())
                .entity(saved)
                .build();
    }

    @PUT
    @Path("books/{id}")
    public BookData updateBook(@PathParam("id") long id, BookData book) {
        return new BookData(
                service.saveBook(
                        id, book.title, book.publishedYear, book.price, book.stock, book.authorId));
    }

    @DELETE
    @Path("books/{id}")
    public Response deleteBook(@PathParam("id") long id) {
        service.deleteBook(id);
        return Response.noContent().build();
    }
}
