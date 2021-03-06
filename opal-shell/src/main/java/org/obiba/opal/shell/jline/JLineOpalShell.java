/*
 * Copyright (c) 2021 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.shell.jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import org.obiba.opal.shell.AbstractOpalShell;
import org.obiba.opal.shell.CommandRegistry;

import jline.ConsoleReader;
import jline.Terminal;
import jline.UnixTerminal;

/**
 * Implements {@code OpalShell} using {@code JLine}
 */
public class JLineOpalShell extends AbstractOpalShell {

  private final ConsoleReader consoleReader;

  public JLineOpalShell(CommandRegistry registry, InputStream in, Writer out) {
    super(registry);
    try {

      // OPAL-222
      // Forcing a Unix terminal to fix a Putty compatibility issue when connecting from Windows through SSH.
      Terminal terminal = new UnixTerminal();
      consoleReader = new ConsoleReader(in, out, null, terminal);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void printf(String format, Object... args) {
    try {
      consoleReader.printString(String.format(format, args));
      // OPAL-267: need to flush the console to make sure the string is actually printed.
      consoleReader.flushConsole();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public char[] passwordPrompt(String format, Object... args) {
    try {
      return consoleReader.readLine(String.format(format, args), (char) 0).toCharArray();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String prompt(String format, Object... args) {
    try {
      return consoleReader.readLine(String.format(format, args));
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }
}
