/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * WekaFileChooser.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package weka.gui;

import com.googlecode.jfilechooserbookmarks.AbstractBookmarksPanel;
import com.googlecode.jfilechooserbookmarks.AbstractFactory;
import com.googlecode.jfilechooserbookmarks.AbstractPropertiesHandler;
import com.googlecode.jfilechooserbookmarks.DefaultFactory;
import weka.core.WekaPackageManager;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

/**
 * Customized WekaFileChooser with support for bookmarks.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class WekaFileChooser
  extends JFileChooser {

  public static class PropertiesHandler
    extends AbstractPropertiesHandler {

    protected String getFilename() {
      return WekaPackageManager.PROPERTIES_DIR + File.separator + "bookmarks.props";
    }
  }

  public static class Factory
    extends DefaultFactory {

    public AbstractPropertiesHandler newPropertiesHandler() {
      return new PropertiesHandler();
    }
  }

  public static class FileChooserBookmarksPanel
    extends AbstractBookmarksPanel {

    protected AbstractFactory newFactory() {
      return new Factory();
    }
  }

  /** the accessory panel. */
  protected JPanel m_AccessoryPanel;

  /** the bookmarks. */
  protected FileChooserBookmarksPanel m_BookmarksPanel;

  /**
   * Constructs a <code>WekaFileChooser</code> pointing to the user's
   * default directory. This default depends on the operating system.
   * It is typically the "My Documents" folder on Windows, and the
   * user's home directory on Unix.
   */
  public WekaFileChooser() {
    super();
    initialize();
  }

  /**
   * Constructs a <code>WekaFileChooser</code> using the given path.
   * Passing in a <code>null</code>
   * string causes the file chooser to point to the user's default directory.
   * This default depends on the operating system. It is
   * typically the "My Documents" folder on Windows, and the user's
   * home directory on Unix.
   *
   * @param currentDirectoryPath  a <code>String</code> giving the path
   *                          to a file or directory
   */
  public WekaFileChooser(String currentDirectoryPath) {
    super(currentDirectoryPath);
    initialize();
  }

  /**
   * Constructs a <code>WekaFileChooser</code> using the given <code>File</code>
   * as the path. Passing in a <code>null</code> file
   * causes the file chooser to point to the user's default directory.
   * This default depends on the operating system. It is
   * typically the "My Documents" folder on Windows, and the user's
   * home directory on Unix.
   *
   * @param currentDirectory  a <code>File</code> object specifying
   *                          the path to a file or directory
   */
  public WekaFileChooser(File currentDirectory) {
    super(currentDirectory);
    initialize();
  }

  /**
   * Constructs a <code>WekaFileChooser</code> using the given
   * <code>FileSystemView</code>.
   *
   * @param fsv a {@code FileSystemView}
   */
  public WekaFileChooser(FileSystemView fsv) {
    super(fsv);
    initialize();
  }


  /**
   * Constructs a <code>WekaFileChooser</code> using the given current directory
   * and <code>FileSystemView</code>.
   *
   * @param currentDirectory a {@code File} object specifying the path to a
   *                         file or directory
   * @param fsv a {@code FileSystemView}
   */
  public WekaFileChooser(File currentDirectory, FileSystemView fsv) {
    super(currentDirectory, fsv);
    initialize();
  }

  /**
   * Constructs a <code>WekaFileChooser</code> using the given current directory
   * path and <code>FileSystemView</code>.
   *
   * @param currentDirectoryPath a {@code String} specifying the path to a file
   *                             or directory
   * @param fsv a {@code FileSystemView}
   */
  public WekaFileChooser(String currentDirectoryPath, FileSystemView fsv) {
    super(currentDirectoryPath, fsv);
    initialize();
  }

  /**
   * Initializes the accessory panel.
   */
  protected void initialize() {
    setPreferredSize(new Dimension(750, 500));
    m_AccessoryPanel = new JPanel(new BorderLayout());
    m_BookmarksPanel = new FileChooserBookmarksPanel();
    m_BookmarksPanel.setOwner(this);
    m_AccessoryPanel.add(m_BookmarksPanel);
    setAccessory(m_AccessoryPanel);
  }
}
