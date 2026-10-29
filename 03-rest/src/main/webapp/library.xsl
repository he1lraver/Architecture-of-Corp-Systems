<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8"/>
  <xsl:param name="displayBase" select="'/library/api'"/>
  <xsl:template match="/">
    <html lang="ru"><head><meta name="viewport" content="width=device-width, initial-scale=1"/><title>Библиотека · XML и XSLT</title><style>body{font:14px Arial,sans-serif;color:#222;background:white;max-width:1000px;margin:24px auto;padding:0 16px}h1{font-size:24px}h2{font-size:19px}article{padding:12px 0;border-bottom:1px solid #ccc}a{display:inline-block;margin:0 16px 6px 0}dt{font-weight:bold}dd{margin-bottom:8px}</style></head>
      <body><h1>XML - HTML через XSLT</h1><nav><a href="/library/ui">Формы CRUD</a><a href="{$displayBase}/authors?format=xml">Все авторы</a><a href="{$displayBase}/books?format=xml">Все книги</a></nav>
        <xsl:apply-templates select="authors/author|books/book|author|book|error"/>
      </body></html>
  </xsl:template>
  <xsl:template match="author|book">
    <article><h2><xsl:value-of select="name|title"/></h2><dl><xsl:for-each select="*"><dt><xsl:value-of select="name()"/></dt><dd><xsl:value-of select="."/></dd></xsl:for-each></dl>
      <xsl:choose><xsl:when test="self::author"><a href="{$displayBase}/authors/{id}?format=xml">Карточка автора</a><a href="{$displayBase}/books?format=xml">К списку книг</a></xsl:when>
        <xsl:otherwise><a href="{$displayBase}/books/{id}?format=xml">Карточка книги</a><a href="{$displayBase}/authors/{authorId}?format=xml">Автор книги</a></xsl:otherwise></xsl:choose>
    </article>
  </xsl:template>
  <xsl:template match="error"><p><xsl:value-of select="message"/></p></xsl:template>
</xsl:stylesheet>
