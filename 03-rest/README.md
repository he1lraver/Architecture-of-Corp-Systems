# Третья лабораторная работа - RESTful веб-сервис

Библиотека из первой работы дополнена REST API. Сохранены три слоя: JavaBeans Author и Book с JPA, бизнес-операции в EJB LibraryService и веб-слой. У автора есть имя, страна и год рождения, у книги - название, год, цена, остаток и автор.

## Выбор технологии

JAX-RS описывает REST-ресурсы аннотациями @Path, @GET, @POST, @PUT и @DELETE; реализацию предоставляет WildFly. Spring REST строится на контроллерах Spring MVC с @RestController и HTTP-сопоставлениями и удобно продолжает приложение на Spring.

Выбран JAX-RS, потому что продолжается первая работа на Jakarta EE: EJB, EntityManager и сервер WildFly уже используются. Переход на Spring для этой версии потребовал бы перенести управление компонентами и транзакциями. Spring REST был бы естественным выбором при продолжении второй работы. Отдельный Spring-контейнер здесь не нужен.

## API

Базовый адрес: http://localhost:18380/library/api.

| Метод | Путь | Результат |
| --- | --- | --- |
| GET | /authors | Список авторов |
| GET | /authors/{id} | Один автор |
| POST | /authors | Добавление автора, 201 и Location |
| PUT | /authors/{id} | Изменение автора, 200 |
| DELETE | /authors/{id} | Удаление автора, 204 |
| GET | /books | Список книг |
| GET | /books/{id} | Одна книга |
| POST | /books | Добавление книги, 201 и Location |
| PUT | /books/{id} | Изменение книги, 200 |
| DELETE | /books/{id} | Удаление книги, 204 |

Content-Type задаёт формат тела запроса, Accept - формат ответа: application/json или application/xml. Можно отправить XML и получить JSON или наоборот. При удалении с кодом 204 тела ответа нет. Списки JSON представлены объектом с массивом items; XML имеет корень authors или books. Для XML в браузере можно добавить ?format=xml.

Неверные поля дают 400, отсутствующая запись - 404, удаление автора с книгами - 409. Тело больше 64 КиБ отклоняется с 413, неподдерживаемый формат входа - с 415. XML с DTD не принимается. Цена неотрицательная, максимум два десятичных знака; остаток неотрицательный; год от 1 до следующего календарного года.

Пример JSON автора:

```json
{"name":"Иван Бунин","country":"Россия","birthYear":1870}
```

Пример XML книги. В authorId укажите ID существующего автора:

```xml
<book>
  <title>Тёмные аллеи</title>
  <publishedYear>1943</publishedYear>
  <price>450.00</price>
  <stock>3</stock>
  <authorId>1</authorId>
</book>
```

DTO AuthorData и BookData используются и для JSON-B, и для JAXB. Они передают поля без внутренних JPA-связей. PayloadReader читает и проверяет входной формат; XmlSupport записывает XML с инструкцией xml-stylesheet перед корневым элементом.

## XML и XSL

- /library/api/books?format=xml - XML списка книг с xml-stylesheet.
- /library/api/authors?format=xml - XML списка авторов.
- /library/library.xsl - статический XSL-файл.
- /library/view/books - HTML, полученный тем же XSL на сервере.
- /library/view/authors - просмотр авторов через XSL.
- /library/ui - формы добавления, изменения и удаления.

XSL показывает свойства объектов и ссылки на карточки, автора книги и оба списка. Режим /view нужен браузерам, в которых клиентское XSLT-преобразование недоступно. Он дополняет XML-ответы с xml-stylesheet, а не заменяет их.

## Сборка и запуск на Windows

Нужны JDK 21, Maven и WildFly 39. Команды ниже предназначены для cmd.exe и выполняются из папки работы. Java и Maven должны быть доступны через PATH.

1. Соберите WAR:

```text
mvn clean package
```

2. Укажите каталог WildFly и один раз создайте новую файловую БД H2:

```bat
set WILDFLY_HOME=C:\wildfly-39.0.1.Final
java -cp "%WILDFLY_HOME%\modules\system\layers\base\com\h2database\h2\main\*" org.h2.tools.RunScript -url "jdbc:h2:~/practice3" -user sa -password practice -script database.sql
```

H2 входит в WildFly. Файл practice3.mv.db находится в домашнем каталоге пользователя Java. SQL создаёт таблицы и начальные записи; повторять INSERT при каждом старте не нужно.

3. Подготовьте отдельный каталог сервера. Исходная конфигурация должна быть стандартной standalone.xml из установленного WildFly:

```bat
set JBOSS_BASE_DIR=%CD%\runtime\server
mkdir "%JBOSS_BASE_DIR%\configuration"
xcopy "%WILDFLY_HOME%\standalone\configuration" "%JBOSS_BASE_DIR%\configuration" /E /I
```

Копирование выполняется один раз. Не перезаписывайте этим шагом уже настроенный каталог сервера. Отдельный каталог хранит настройки и развёртывание третьей работы, а данные лежат в собственной БД.

4. Запустите сервер в этом же окне:

```bat
"%WILDFLY_HOME%\bin\standalone.bat" -Djboss.socket.binding.port-offset=10300 -b 127.0.0.1 -bmanagement 127.0.0.1
```

5. В другом окне cmd.exe задайте WILDFLY_HOME и из папки работы подключитесь к CLI. Локальное подключение использует механизм local authentication WildFly:

```bat
set WILDFLY_HOME=C:\wildfly-39.0.1.Final
"%WILDFLY_HOME%\bin\jboss-cli.bat" --connect --controller=127.0.0.1:20290
```

В CLI выполните:

```text
data-source add --name=LibraryDS --jndi-name=java:/jdbc/LibraryDS --driver-name=h2 --connection-url="jdbc:h2:~/practice3" --user-name=sa --password=practice
/subsystem=datasources/data-source=LibraryDS:test-connection-in-pool
deploy target/library.war
```

Источник данных и deployment сохраняются. При следующем запуске достаточно задать WILDFLY_HOME и JBOSS_BASE_DIR и запустить сервер, как в шаге 4. Для обновления WAR после сборки используйте в CLI deploy target/library.war --force. Для остановки - shutdown в CLI. Рабочие данные сохраняются при перезапуске. При переносе используйте тот же файл БД и согласуйте путь JDBC.

6. Откройте http://localhost:18380/library/ui или один из адресов API выше. Развёртывание выполняется средствами сервера.

Пример запроса в cmd.exe с заголовками и ответом JSON:

```bat
curl.exe -i -H "Accept: application/json" http://localhost:18380/library/api/books
```

Для проверки XML замените заголовок на Accept: application/xml. Для POST/PUT удобно сохранить JSON или XML в UTF-8 файл и передать его curl.exe параметром --data-binary @имя-файла с соответствующим Content-Type.

## Соответствие заданию

1. JAX-RS и Spring REST сопоставлены выше; выбор JAX-RS объяснён продолжением первой работы.
2. Сохранены модели, бизнес-слой и операции каталога; REST API описан таблицей.
3. Обе сущности доступны на чтение и изменение в JSON и XML, форматы входа и выхода выбираются независимо.
4. library.xsl находится в статической части веб-приложения и содержит вывод свойств и навигацию.
5. XmlSupport добавляет xml-stylesheet в начало XML-ответов, включая прикладные ошибки, возвращаемые в XML.
6. Сервер, JPA, EJB, REST, формы и XSL работают совместно с файловой H2.

## Файлы

- domain - сущности Author и Book, проверки Rules.
- ee/LibraryService.java - бизнес-операции и транзакции EJB.
- ee/Api.java - REST-ресурсы и DTO.
- ee/PayloadReader.java - чтение JSON/XML.
- ee/XmlSupport.java - запись XML, выбор формата и обработка бизнес-ошибок.
- ee/LibraryServlet.java и view/Html.java - формы каталога.
- ee/XmlViewServlet.java - серверное применение XSL.
- META-INF/persistence.xml - JPA и источник данных.
- database.sql - создание и начальное заполнение базы.

Java находится в src/main/java/edu/masters, конфигурация JPA - в src/main/resources, HTML и XSL - в src/main/webapp. Для ручной проверки создайте автора и книгу, прочитайте их в обоих форматах, измените, проверьте XSL-ссылки и сохранность после перезапуска, затем удалите книгу и автора.
