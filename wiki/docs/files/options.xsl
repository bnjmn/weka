<?xml version= "1.0"?>

<!--  
  This little XSLT script parses an Experimenter XML setup file and outputs
  the classifiers and their options in two formats:
    1. array-like, i.e., each option on its separate line
    2. commandline, i.e., as the class and the options as they would be
       necessary on the commandline

  Note: Doesn't perform escaping of inner double quotes! Has to be done
        manually.

  Usage: 
      xsltproc <this xslt script> <experimenter xml file>

  Example: 
      parses the xml file "setup.xml" and pipes the output into the file
      "setup.txt" for future use:

      xsltproc options.xsl setup.xml > setup.txt         


  FracPete, 2005-10-18
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>

  <xsl:template match="/">
    <!-- output classifiers in an array-like fashion -->
    <xsl:text>&#10;--&gt; Classifiers (array-like):&#10;&#10;</xsl:text>
    <xsl:for-each select="//object[@name='propertyArray']">
      <xsl:call-template name="classifiers-array"/>
    </xsl:for-each>

    <!-- output classifiers in a commandline format -->
    <xsl:text>&#10;--&gt; Classifiers (commandline):&#10;&#10;</xsl:text>
    <xsl:for-each select="//object[@name='propertyArray']">
      <xsl:call-template name="classifiers-commandline"/>
    </xsl:for-each>
  </xsl:template>

  <!-- traverse classifiers (array) -->
  <xsl:template name="classifiers-array">
    <xsl:for-each select="./object">
      <xsl:value-of select="@class"/><xsl:text>&#10;</xsl:text>
      <xsl:for-each select="./object[@name='options']">
        <xsl:call-template name="options-array"/>
      </xsl:for-each>
      <xsl:text>&#10;</xsl:text>
    </xsl:for-each>
  </xsl:template>

  <!-- extract options (array) -->
  <xsl:template name="options-array">
    <xsl:for-each select="./object">
      <xsl:if test=". != ''">
        <xsl:text>  </xsl:text><xsl:value-of select="."/><xsl:text>&#10;</xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- traverse classifiers (commandline) -->
  <xsl:template name="classifiers-commandline">
    <xsl:for-each select="./object">
      <xsl:value-of select="@class"/>
      <xsl:for-each select="./object[@name='options']">
        <xsl:call-template name="options-commandline"/>
      </xsl:for-each>
      <xsl:text>&#10;</xsl:text>
      <xsl:text>&#10;</xsl:text>
    </xsl:for-each>
  </xsl:template>

  <!-- extract options (commandline) -->
  <xsl:template name="options-commandline">
    <xsl:for-each select="./object">
      <xsl:if test=". != ''">
        <xsl:text> \&#10;  </xsl:text>
        
        <!-- do we have to quote this option? -->
        <xsl:if test="contains(., ' ')">
          <xsl:text>&quot;</xsl:text>
        </xsl:if>
        
        <xsl:value-of select="."/>
        
        <!-- closing quote? -->
        <xsl:if test="contains(., ' ')">
          <xsl:text>&quot;</xsl:text>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
