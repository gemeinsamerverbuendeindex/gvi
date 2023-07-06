<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:strip-space elements="*"/>
    <xsl:output method="text"/>
    
    <xsl:template match="datafield[@tag='035']">
        <xsl:choose>
            <xsl:when test="starts-with(subfield[@code='a'],'(DE-605)')">
                <xsl:value-of select="normalize-space(.)"/>
                <xsl:text>&#xA;</xsl:text>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="leader|controlfield|datafield[@tag!='035']"/>

</xsl:stylesheet>
