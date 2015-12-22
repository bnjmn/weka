package weka.gui;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PerspectiveInfo {

  /**
   * The ID of this perspective
   *
   * @return the ID of this perspective
   */
  String ID();

  /**
   * The title of this perspective
   * 
   * @return the title of this perspective
   */
  String title();

  /**
   * The tool tip text for this perspective
   * 
   * @return the tool tip text
   */
  String toolTipText();

  /**
   * Path (as a resource on the classpath) to the icon for this perspective
   * 
   * @return the path to the icon for this perspective
   */
  String iconPath();
}
