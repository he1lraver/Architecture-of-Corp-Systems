package edu.masters.spring;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@AutoConfigureMockMvc
class LibraryTest {
    @Autowired MockMvc mvc;
    @Autowired LibraryService service;

    @Test
    void completeCrud() throws Exception {
        mvc.perform(
                        post("/authors")
                                .param("name", "Test author")
                                .param("country", "Test")
                                .param("birthYear", "1980"))
                .andExpect(status().is3xxRedirection());
        long author = service.authors().getFirst().getId();
        mvc.perform(
                        post("/books")
                                .param("title", "Test book")
                                .param("publishedYear", "2020")
                                .param("price", "19.50")
                                .param("stock", "4")
                                .param("authorId", "" + author))
                .andExpect(status().is3xxRedirection());
        long book = service.books().getFirst().getId();
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test book")));
        mvc.perform(post("/authors").param("id", "" + author).param("operation", "delete"))
                .andExpect(status().isConflict());
        mvc.perform(
                        post("/authors")
                                .param("id", "" + author)
                                .param("name", "Updated author")
                                .param("country", "Test")
                                .param("birthYear", "1981"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(
                        post("/books")
                                .param("id", "" + book)
                                .param("title", "Updated book")
                                .param("publishedYear", "2021")
                                .param("price", "20.00")
                                .param("stock", "2")
                                .param("authorId", "" + author))
                .andExpect(status().is3xxRedirection());
        assertEquals("Updated book", service.books().getFirst().getTitle());
        assertEquals("Updated author", service.authors().getFirst().getName());
        mvc.perform(post("/books").param("id", "" + book).param("operation", "delete"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/authors").param("id", "" + author).param("operation", "delete"))
                .andExpect(status().is3xxRedirection());
        assertTrue(service.books().isEmpty());
        assertTrue(service.authors().isEmpty());
    }

    @Test
    void badInputRejected() throws Exception {
        mvc.perform(
                        post("/authors")
                                .param("name", "")
                                .param("country", "Test")
                                .param("birthYear", "1980"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRecordIs404() throws Exception {
        mvc.perform(post("/authors").param("id", "999999").param("operation", "delete"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingPriceIsBadRequest() throws Exception {
        mvc.perform(post("/books").param("title", "Book").param("publishedYear", "2020"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("price")));
    }

    @Test
    void unknownOperationIsRejected() throws Exception {
        mvc.perform(post("/authors").param("operation", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidIdHasReadableMessage() throws Exception {
        mvc.perform(post("/books").param("id", "wrong").param("operation", "delete"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Проверьте числовые поля")));
    }
}
