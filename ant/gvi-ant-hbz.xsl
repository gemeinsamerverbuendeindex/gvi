<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:strip-space elements="*"/>
    <xsl:output method="text"/>
    
    <xsl:template match="controlfield[@tag='001']">
        <xsl:value-of select="concat(normalize-space(.),'&#xA;')"/>
    </xsl:template>
    
    <xsl:template match="leader|controlfield[@tag!='001']|datafield"/>

</xsl:stylesheet>
