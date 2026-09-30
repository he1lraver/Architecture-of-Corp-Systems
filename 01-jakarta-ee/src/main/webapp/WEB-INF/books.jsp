<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Книги</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<nav><a href="authors">Авторы</a> | <a href="books">Книги</a></nav>
<h1>Книги</h1>
<p class="error"><c:out value="${error}"/></p>

<table>
    <tr><th>Название</th><th>Год</th><th>Автор</th><th>Действия</th></tr>
    <c:forEach var="book" items="${books}">
        <tr>
            <td><c:out value="${book.title}"/></td>
            <td>${book.publishedYear}</td>
            <td><c:out value="${book.author.name}"/></td>
            <td>
                <a href="books?edit=${book.id}">Изменить</a>
                <form class="inline" method="post" action="books">
                    <input type="hidden" name="id" value="${book.id}">
                    <button name="action" value="delete">Удалить</button>
                </form>
            </td>
        </tr>
    </c:forEach>
</table>

<h2>${empty editing ? 'Добавить книгу' : 'Изменить книгу'}</h2>
<c:if test="${empty authors}"><p>Сначала добавьте хотя бы одного автора.</p></c:if>
<form method="post" action="books">
    <input type="hidden" name="id" value="${editing.id}">
    <label>Название <input name="title" required maxlength="160" value="<c:out value='${editing.title}'/>"></label>
    <label>Год <input type="number" name="publishedYear" required min="1" max="2100" value="${editing.publishedYear}"></label>
    <label>Автор
        <select name="authorId" required>
            <option value="">Выберите автора</option>
            <c:forEach var="author" items="${authors}">
                <option value="${author.id}" ${editing.author.id == author.id ? 'selected' : ''}><c:out value="${author.name}"/></option>
            </c:forEach>
        </select>
    </label>
    <button name="action" value="save">Сохранить</button>
    <a href="books">Отмена</a>
</form>
</body>
</html>
