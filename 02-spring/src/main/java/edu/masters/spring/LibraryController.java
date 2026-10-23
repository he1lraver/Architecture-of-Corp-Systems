package edu.masters.spring;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class LibraryController {
    private final LibraryService service;

    public LibraryController(LibraryService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("authors", service.authors());
        model.addAttribute("books", service.books());
        return "library";
    }

    @PostMapping("/authors")
    public String author(@RequestParam Map<String, String> parameters) {
        Long id = readId(parameters);
        String operation = parameters.getOrDefault("operation", "save");
        if ("delete".equals(operation)) {
            if (id == null) {
                throw new IllegalArgumentException("Не выбран автор");
            }
            service.deleteAuthor(id);
        } else if ("save".equals(operation)) {
            service.saveAuthor(id, field(parameters, "name"), field(parameters, "country"),
                    Integer.parseInt(field(parameters, "birthYear")));
        } else {
            throw new IllegalArgumentException("Неизвестное действие");
        }
        return "redirect:/";
    }

    @PostMapping("/books")
    public String book(@RequestParam Map<String, String> parameters) {
        Long id = readId(parameters);
        String operation = parameters.getOrDefault("operation", "save");
        if ("delete".equals(operation)) {
            if (id == null) {
                throw new IllegalArgumentException("Не выбрана книга");
            }
            service.deleteBook(id);
        } else if ("save".equals(operation)) {
            service.saveBook(id, field(parameters, "title"),
                    Integer.parseInt(field(parameters, "publishedYear")),
                    new BigDecimal(field(parameters, "price")),
                    Integer.parseInt(field(parameters, "stock")),
                    Long.parseLong(field(parameters, "authorId")));
        } else {
            throw new IllegalArgumentException("Неизвестное действие");
        }
        return "redirect:/";
    }

    private Long readId(Map<String, String> parameters) {
        String value = parameters.get("id");
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private String field(Map<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Заполните поле: " + name);
        }
        return value.trim();
    }

    @ExceptionHandler({IllegalArgumentException.class, ResponseStatusException.class})
    public ModelAndView invalid(Exception exception) {
        ModelAndView page = new ModelAndView("error");
        if (exception instanceof ResponseStatusException error) {
            page.setStatus(error.getStatusCode());
            page.addObject("message", error.getReason());
        } else {
            page.setStatus(HttpStatus.BAD_REQUEST);
            String message = exception instanceof NumberFormatException
                    ? "Проверьте числовые поля: номер записи, год, цену, остаток и автора"
                    : exception.getMessage();
            page.addObject("message", message);
        }
        return page;
    }
}
