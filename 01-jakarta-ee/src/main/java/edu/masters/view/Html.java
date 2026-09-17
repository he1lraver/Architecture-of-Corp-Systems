package edu.masters.view;

import edu.masters.domain.Author;
import edu.masters.domain.Book;

import java.util.List;

public final class Html {
    private Html() {}

    public static String escape(Object value) {
        return value == null
                ? ""
                : value.toString()
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
    }

    private static String input(String name, Object value, String type) {
        String label =
                switch (name) {
                    case "name" -> "Имя автора";
                    case "country" -> "Страна";
                    case "birthYear" -> "Год рождения";
                    case "title" -> "Название книги";
                    case "publishedYear" -> "Год издания";
                    case "price" -> "Цена, руб.";
                    case "stock" -> "Остаток, шт.";
                    default -> name;
                };
        return "<label>"
                + label
                + "<input required name='"
                + name
                + "' type='"
                + type
                + "' value='"
                + escape(value)
                + "'"
                + (name.equals("price") ? " step='0.01' min='0'" : "")
                + "></label>";
    }

    private static String buttons(Long id) {
        return (id == null ? "" : "<input type='hidden' name='id' value='" + id + "'>")
                + "<button name='operation' value='save'>"
                + (id == null ? "Добавить" : "Сохранить")
                + "</button>"
                + (id == null
                        ? ""
                        : "<button class='danger' formnovalidate name='operation' value='delete'>Удалить</button>");
    }

    public static String page(String title, String body) {
        return "<!doctype html><html lang='ru'><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>"
                + escape(title)
                + "</title><style>"
                + "body{font:14px Arial,sans-serif;color:#222;background:white;max-width:1180px;margin:24px auto;padding:0 16px}"
                + "h1{font-size:24px}h2{font-size:19px;margin-top:28px;border-bottom:1px solid #aaa;padding-bottom:6px}"
                + "form{display:flex;flex-wrap:wrap;align-items:end;gap:10px;padding:12px 0;border-bottom:1px solid #ddd}"
                + "label{display:grid;gap:5px}input,select{font:inherit;box-sizing:border-box;width:155px;max-width:100%;padding:6px;border:1px solid #888}"
                + "input[name=name],input[name=title]{width:230px}button{font:inherit;padding:6px 10px;cursor:pointer}.danger{color:#8b2020}nav a{display:inline-block;margin:0 16px 6px 0}"
                + "small{display:block;color:#555;margin-top:18px}pre{white-space:pre-wrap;overflow-wrap:anywhere}article{padding:12px 0;border-bottom:1px solid #ccc}"
                + "@media(max-width:480px){label{width:100%}input,select,input[name=name],input[name=title]{width:100%}}"
                + "</style>"
                + "<h1>"
                + escape(title)
                + "</h1>"
                + body
                + "</html>";
    }

    public static String library(
            String title, String base, List<Author> authors, List<Book> books, String extra) {
        StringBuilder out =
                new StringBuilder(
                        "<p>Учебный каталог · авторы и книги · CRUD</p>"
                                + extra
                                + "<h2>Авторы</h2>");
        for (int i = 0; i <= authors.size(); i++) {
            Author author = i == authors.size() ? new Author() : authors.get(i);
            if (author.getId() == null) out.append("<p><b>Добавить автора</b></p>");
            out.append("<form method='post' action='")
                    .append(base)
                    .append("/authors'>")
                    .append(input("name", author.getName(), "text"))
                    .append(input("country", author.getCountry(), "text"))
                    .append(
                            input(
                                    "birthYear",
                                    author.getId() == null ? 1980 : author.getBirthYear(),
                                    "number"))
                    .append(buttons(author.getId()))
                    .append("</form>");
        }
        out.append("<h2>Книги</h2>");
        for (int i = 0; i <= books.size(); i++) {
            Book book = i == books.size() ? new Book() : books.get(i);
            if (book.getId() == null) out.append("<p><b>Добавить книгу</b></p>");
            out.append("<form method='post' action='")
                    .append(base)
                    .append("/books'>")
                    .append(input("title", book.getTitle(), "text"))
                    .append(
                            input(
                                    "publishedYear",
                                    book.getId() == null ? 2020 : book.getPublishedYear(),
                                    "number"))
                    .append(input("price", book.getId() == null ? 100 : book.getPrice(), "number"))
                    .append(input("stock", book.getStock(), "number"))
                    .append("<label>Автор<select name='authorId' required>");
            for (Author author : authors)
                out.append("<option value='")
                        .append(author.getId())
                        .append("'")
                        .append(
                                book.getAuthor() != null
                                                && author.getId().equals(book.getAuthor().getId())
                                        ? " selected"
                                        : "")
                        .append(">")
                        .append(escape(author.getName()))
                        .append("</option>");
            out.append("</select></label>").append(buttons(book.getId())).append("</form>");
        }
        out.append(
                "<small>Учебная локальная версия. Удаление автора с книгами запрещено. Нулевой остаток допустим.</small>");
        return page(title, out.toString());
    }
}
