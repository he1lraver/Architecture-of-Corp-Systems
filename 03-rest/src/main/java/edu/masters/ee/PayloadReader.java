package edu.masters.ee;

import jakarta.annotation.Priority;
import jakarta.json.bind.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import jakarta.xml.bind.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.*;

// - Ограничивает размер ввода; XML не может загружать внешние сущности (XXE).
@Provider
@Priority(1000)
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class PayloadReader implements MessageBodyReader<Object> {
    public boolean isReadable(
            Class<?> type, Type generic, Annotation[] annotations, MediaType media) {
        return type == Api.AuthorData.class || type == Api.BookData.class;
    }

    public Object readFrom(
            Class<Object> type,
            Type generic,
            Annotation[] annotations,
            MediaType media,
            MultivaluedMap<String, String> headers,
            InputStream input)
            throws IOException {
        byte[] bytes = input.readNBytes(65537);
        if (bytes.length > 65536) throw new WebApplicationException("Payload too large", 413);
        String text = new String(bytes, StandardCharsets.UTF_8);
        try {
            Object value;
            if (media.isCompatible(MediaType.APPLICATION_XML_TYPE)) {
                if (text.contains("<!DOCTYPE")) throw new BadRequestException("DTD is not allowed");
                XMLInputFactory factory = XMLInputFactory.newFactory();
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
                XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(text));
                try {
                    value = JAXBContext.newInstance(type).createUnmarshaller().unmarshal(reader);
                } finally {
                    reader.close();
                }
            } else {
                try (Jsonb json = JsonbBuilder.create()) {
                    value = json.fromJson(text, type);
                }
            }
            if (value == null) throw new BadRequestException("Request body is required");
            return value;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid JSON/XML body", e);
        }
    }

    @Provider
    public static class BadInput implements ExceptionMapper<BadRequestException> {
        public Response toResponse(BadRequestException e) {
            return Response.status(400)
                    .entity(new Api.Problem("Некорректное тело JSON/XML запроса"))
                    .build();
        }
    }
}
