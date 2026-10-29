package edu.masters.ee;

import edu.masters.view.Html;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet({"/ui", "/authors", "/books"})
public class LibraryServlet extends HttpServlet {
    @EJB private LibraryService service;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String base = request.getContextPath();
        response.getWriter()
                .print(
                        Html.library(
                                "Библиотека · REST",
                                base,
                                service.authors(),
                                service.books(),
                                "<nav><a href='"
                                        + base
                                        + "/api/authors?format=xml'>XML / XSL</a><a href='"
                                        + base
                                        + "/api/books'>JSON API</a><a href='"
                                        + base
                                        + "/view/books'>Просмотр через XSL</a></nav>"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            String idText = request.getParameter("id");
            Long id = idText == null || idText.isBlank() ? null : Long.valueOf(idText);
            String operation = request.getParameter("operation");
            if (operation != null && !"save".equals(operation) && !"delete".equals(operation)) {
                throw new IllegalArgumentException("Неизвестное действие");
            }
            if ("/ui".equals(request.getServletPath())) {
                response.sendError(405);
                return;
            }
            boolean isAuthor = request.getServletPath().equals("/authors"),
                    isDelete = "delete".equals(request.getParameter("operation"));
            if (isDelete) {
                if (id == null) throw new IllegalArgumentException("Нужен id");
                if (isAuthor) service.deleteAuthor(id);
                else service.deleteBook(id);
            } else if (isAuthor)
                service.saveAuthor(
                        id,
                        request.getParameter("name"),
                        request.getParameter("country"),
                        Integer.parseInt(request.getParameter("birthYear")));
            else
                service.saveBook(
                        id,
                        request.getParameter("title"),
                        Integer.parseInt(request.getParameter("publishedYear")),
                        new BigDecimal(required(request, "price")),
                        Integer.parseInt(request.getParameter("stock")),
                        Long.parseLong(request.getParameter("authorId")));
            response.setStatus(303);
            response.setHeader("Location", request.getContextPath() + "/ui");
        } catch (LibraryException | IllegalArgumentException e) {
            response.setStatus(e instanceof LibraryException problem ? problem.status : 400);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter()
                    .print(
                            Html.page(
                                    "Операция отклонена",
                                    "<p>"
                                            + Html.escape(e.getMessage())
                                            + "</p><a href='"
                                            + request.getContextPath()
                                            + "/ui'>Вернуться</a>"));
        }
    }

    private String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Заполните поле: " + name);
        }
        return value.trim();
    }
}
