<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Авторы</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/style.css">
</head>
<body>
<nav><a href="authors">Авторы</a> | <a href="books">Книги</a></nav>
<h1>Авторы</h1>
<p class="error"><c:out value="${error}"/></p>

<table>
    <tr><th>Имя</th><th>Страна</th><th>Действия</th></tr>
    <c:forEach var="author" items="${authors}">
        <tr>
            <td><c:out value="${author.name}"/></td>
            <td><c:out value="${author.country}"/></td>
            <td>
                <a href="authors?edit=${author.id}">Изменить</a>
                <form class="inline" method="post" action="authors">
                    <input type="hidden" name="id" value="${author.id}">
                    <button name="action" value="delete">Удалить</button>
                </form>
            </td>
        </tr>
    </c:forEach>
</table>

<h2>${empty editing ? 'Добавить автора' : 'Изменить автора'}</h2>
<form method="post" action="authors">
    <input type="hidden" name="id" value="${editing.id}">
    <label>Имя <input name="name" required maxlength="120" value="<c:out value='${editing.name}'/>"></label>
    <label>Страна <input name="country" required maxlength="80" value="<c:out value='${editing.country}'/>"></label>
    <button name="action" value="save">Сохранить</button>
    <a href="authors">Отмена</a>
</form>
</body>
</html>
