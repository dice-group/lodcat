<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:html="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" indent="no" />
  <xsl:template match="/">
    <html>
      <style type="text/css">
        table.datasets { width: 100%; }
        table.datasets td:nth-child(3) { width: 50%; }
        .topicProbability {
        display: inline-block;
        height: 1em;
        }
        table.datasets a { white-space: nowrap; text-overflow: ellipsis; }
        /* colors from https://github.com/d3/d3-scale-chromatic/blob/master/src/categorical/category10.js*/
        .topic-0, .topic:nth-child(10n+1) { background: #1f77b4; }
        .topic-1, .topic:nth-child(10n+2) { background: #ff7f0e; }
        .topic-2, .topic:nth-child(10n+3) { background: #2ca02c; }
        .topic-3, .topic:nth-child(10n+4) { background: #d62728; }
        .topic-4, .topic:nth-child(10n+5) { background: #9467bd; }
        .topic-5, .topic:nth-child(10n+6) { background: #8c564b; }
        .topic-6, .topic:nth-child(10n+7) { background: #e377c2; }
        .topic-7, .topic:nth-child(10n+8) { background: #7f7f7f; }
        .topic-8, .topic:nth-child(10n+9) { background: #bcbd22; }
        .topic-9, .topic:nth-child(10n) { background: #17becf; }
      </style>
    <h2>Topics</h2>
    <table>
      <thead>
        <tr><th>ID</th><th>Label</th><th>Datasets</th></tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="/report/topic"/>
      </tbody>
    </table>
    <h2>Datasets</h2>
    <table class="datasets">
      <xsl:apply-templates select="/report/dataset"/>
    </table>
    </html>
  </xsl:template>
  <xsl:template match="topic">
    <tr class="topic">
      <xsl:variable name="topicId" select="@id"/>
      <td>
        <xsl:value-of select="@id"/>
      </td>
      <td>
        <xsl:value-of select="label"/>
      </td>
      <td>
        <xsl:value-of select="count(/report/dataset[topicId = $topicId])"/>
      </td>
    </tr>
  </xsl:template>
  <xsl:template match="dataset">
    <tr>
      <xsl:variable name="topicId" select="topicId"/>
      <xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
      <td>
        <a>
          <xsl:attribute name="href"><xsl:value-of select="url"/></xsl:attribute>
          <xsl:attribute name="title"><xsl:value-of select="@id"/></xsl:attribute>
          <xsl:value-of select="url"/>
        </a>
      </td>
      <td>
        <xsl:attribute name="class">topic-<xsl:value-of select="$topicId"/></xsl:attribute>
        <xsl:value-of select="/report/topic[@id = $topicId]/label"/>
      </td>
      <td>
        <xsl:apply-templates select="./topicProbability"/>
      </td>
    </tr>
  </xsl:template>
  <xsl:template match="topicProbability">
    <span class="topic topicProbability">
      <xsl:attribute name="style">width: <xsl:value-of select="100 * ."/>%;</xsl:attribute>
      <xsl:attribute name="title"><xsl:value-of select="."/></xsl:attribute>
      <xsl:comment/>
    </span>
  </xsl:template>
</xsl:stylesheet>
