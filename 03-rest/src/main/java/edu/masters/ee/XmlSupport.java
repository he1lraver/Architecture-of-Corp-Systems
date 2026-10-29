package edu.masters.ee;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import jakarta.xml.bind.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

// - JAXB сериализация + XSL-инструкция. format=xml нужен для браузерных ссылок.
@Provider
@Produces(MediaType.APPLICATION_XML)
public class XmlSupport implements MessageBodyWriter<Object> {
    @Context
    private UriInfo uri;
    public boolean isWriteable(
            Class<?> type, Type generic, Annotation[] annotations, MediaType media) {
        return type.isAnnotationPresent(jakarta.xml.bind.annotation.XmlRootElement.class);
    }

    public void writeTo(
            Object value,
            Class<?> type,
            Type generic,
            Annotation[] annotations,
            MediaType media,
            MultivaluedMap<String, Object> headers,
            OutputStream output)
            throws IOException {
        try {
            output.write(
                    ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                    + "<?xml-stylesheet type=\"text/xsl\" href=\""
                                    + uri.getBaseUri().resolve("../library.xsl").getRawPath()
                                    + "\"?>\n")
                            .getBytes(StandardCharsets.UTF_8));
            Marshaller marshaller = JAXBContext.newInstance(type).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(value, output);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @Provider
    @PreMatching
    public static class FormatFilter implements ContainerRequestFilter {
        public void filter(ContainerRequestContext request) {
            if ("xml".equals(request.getUriInfo().getQueryParameters().getFirst("format")))
                request.getHeaders().putSingle("Accept", MediaType.APPLICATION_XML);
        }
    }

    @Provider
    public static class BusinessErrors implements ExceptionMapper<LibraryException> {
        public Response toResponse(LibraryException e) {
            return Response.status(e.status).entity(new Api.Problem(e.getMessage())).build();
        }
    }
}
