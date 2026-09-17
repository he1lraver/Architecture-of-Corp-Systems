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
                                "Библиотека · Jakarta EE",
                                base,
                                service.authors(),
                                service.books(),
                                "<p>Практика 1 · данные, EJB и формы</p>"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            Long id =
                    request.getParameter("id") == null
                            ? null
                            : Long.valueOf(request.getParameter("id"));
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
                        new BigDecimal(request.getParameter("price")),
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
}
