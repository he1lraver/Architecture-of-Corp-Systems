package edu.masters.web;

import edu.masters.service.LibraryException;
import edu.masters.service.LibraryService;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/authors")
public class AuthorServlet extends HttpServlet {
    @EJB
    private LibraryService service;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String edit = request.getParameter("edit");
            if (edit != null) {
                request.setAttribute("editing", service.getAuthor(Long.parseLong(edit)));
            }
        } catch (NumberFormatException | LibraryException e) {
            response.setStatus(400);
            request.setAttribute("error", "Не удалось открыть автора для редактирования");
        }
        request.setAttribute("authors", service.getAuthors());
        request.getRequestDispatcher("/WEB-INF/authors.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            String idText = request.getParameter("id");
            Long id = idText == null || idText.isBlank() ? null : Long.valueOf(idText);
            if ("delete".equals(request.getParameter("action"))) {
                if (id == null) {
                    throw new LibraryException("Не выбран автор");
                }
                service.deleteAuthor(id);
            } else {
                service.saveAuthor(id, request.getParameter("name"), request.getParameter("country"));
            }
            response.sendRedirect(request.getContextPath() + "/authors");
        } catch (NumberFormatException e) {
            response.setStatus(400);
            request.setAttribute("error", "Неверный номер автора");
            doGet(request, response);
        } catch (LibraryException e) {
            response.setStatus(400);
            request.setAttribute("error", e.getMessage());
            doGet(request, response);
        }
    }
}
