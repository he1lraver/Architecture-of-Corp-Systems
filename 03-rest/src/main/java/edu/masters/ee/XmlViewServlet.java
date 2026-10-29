package edu.masters.ee;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.xml.bind.JAXBContext;

import java.io.*;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

// - Формирует HTML на сервере через ту же таблицу стилей, которая указана в XML-ответах.
@WebServlet("/view/*")
public class XmlViewServlet extends HttpServlet {
    @EJB private LibraryService service;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo();
        try {
            Object value;
            if ("/authors".equals(path)) value = new Api.Authors(service.authors());
            else if ("/books".equals(path)) value = new Api.Books(service.books());
            else if (path != null && path.matches("/authors/[0-9]+"))
                value = new Api.AuthorData(service.author(Long.parseLong(path.substring(9))));
            else if (path != null && path.matches("/books/[0-9]+"))
                value = new Api.BookData(service.book(Long.parseLong(path.substring(7))));
            else {
                response.sendError(404);
                return;
            }
            StringWriter xml = new StringWriter();
            JAXBContext.newInstance(value.getClass()).createMarshaller().marshal(value, xml);
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            try (InputStream xsl = getServletContext().getResourceAsStream("/library.xsl")) {
                var transformer = factory.newTransformer(new StreamSource(xsl));
                transformer.setParameter("displayBase", request.getContextPath() + "/view");
                response.setContentType("text/html;charset=UTF-8");
                transformer.transform(
                        new StreamSource(new StringReader(xml.toString())),
                        new StreamResult(response.getWriter()));
            }
        } catch (LibraryException e) {
            response.sendError(e.status, e.getMessage());
        } catch (NumberFormatException e) {
            response.sendError(400);
        } catch (Exception e) {
            throw new IOException("XSL preview failed", e);
        }
    }
}
