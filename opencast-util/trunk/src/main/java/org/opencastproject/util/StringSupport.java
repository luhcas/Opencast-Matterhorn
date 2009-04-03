/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *  
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides helper methods to deal with Strings.
 * <p/>
 * Contains code copied from class <code>appetizer.util.Assert</code> of project "appetizer",
 * originally create May 22, 2006.
 * Donated to REPLAY by the author.
 *
 * todo translate original german documentation
 *
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class StringSupport {

    public static final int TRIM = 1;
    public static final int STRIP_EMPTY = 2;
    public static final int GREEDY_LAST = 4;
    public static final int KEEP_DELIM = 8;

    private static Pattern COLLAPSE_PATTERN = Pattern.compile("\\s+");

    private StringSupport() {
    }

    /**
     * Removes all whitespace from the beginning and end and collapses multiple whitespace to a single one.
     *
     * @return the processed string
     */
    public static String collapseWhitespace(String s) {
        String c = s.trim();
        return COLLAPSE_PATTERN.matcher(c).replaceAll(" ");
    }

    /**
     * Printf in einen String statt in einen Stream oder Writer.
     *
     * @see java.io.PrintWriter#printf(String,Object...)
     */
    public static String printf(String format, Object... args) {
        StringWriter s = new StringWriter();
        PrintWriter w = new PrintWriter(s);
        w.printf(format, args);
        w.flush();
        w.close();
        return s.toString();
    }

    /**
     * Printf in einen String statt in einen Stream oder Writer.
     *
     * @see java.io.PrintWriter#printf(java.util.Locale,String,Object...)
     */
    public static String printf(Locale locale, String format, Object... args) {
        StringWriter s = new StringWriter();
        PrintWriter w = new PrintWriter(s);
        w.printf(locale, format, args);
        w.flush();
        w.close();
        return s.toString();
    }

    /**
     * Schreibt das Wort groß.
     *
     * @param s darf null sein
     */
    public static String capitalize(String s) {
        if (s == null || s.length() == 0 || Character.isUpperCase(s.charAt(0)))
            return s;
        char[] chars = s.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    /**
     * Schreibt das Wort klein. Abkürzungen wie "AGB" oder "URL" werden nicht kleingeschrieben.
     *
     * @param s darf null sein
     */
    public static String decapitalize(String s) {
        if (s == null || s.length() == 0 || Character.isLowerCase(s.charAt(0)))
            return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1)) && Character.isUpperCase(s.charAt(0)))
            return s;
        char[] chars = s.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    /** Begrenzt die Länge das Strings auf <i>maxSize</i>. Ist <i>s</i> null, wird null zurückgegeben. */
    public static String limit(String s, int maxSize) {
        if (s != null && s.length() > maxSize)
            return s.substring(0, maxSize);
        return s;
    }

    /**
     * Begrenzt die Länge das Strings auf <i>maxSize</i>. Ist <i>s</i> null, wird null zurückgegeben.
     * <p/>
     * Beispiel: limit("12345", 3, "...") -> "123..."
     *
     * @param maxSize die maximale Länge des Nutzstrings (also ohne die Auslassungszeichen <i>ellipsis</i>)
     * @param cutEnd true: Der String wird hinten beschnitten; false: Vorne
     */
    public static String limit(String s, int maxSize, boolean cutEnd, String ellipsis) {
        if (s != null && s.length() > maxSize) {
            StringBuilder trunc = new StringBuilder();
            if (cutEnd) {
                trunc.append(s.substring(0, maxSize));
                if (ellipsis != null) trunc.append(ellipsis);
            } else {
                if (ellipsis != null) trunc.append(ellipsis);
                return trunc.append(s.substring(s.length() - maxSize)).toString();
            }
            return trunc.toString();
        }
        return s;
    }

    /** Siehe {@link #limit(String,int,boolean,String)}. <i>ellipsis</i> == "..."; <i>cutEnd</i> == true */
    public static String limitEllipsis(String s, int maxSize) {
        return limit(s, maxSize, true, "...");
    }

    /**
     * Stellt dem String <i>s</i> den String <i>prepend</i> voran, falls dieser nicht
     * bereits mit selbigem beginnt. Ist <i>s</i> null wird null zurückgegeben.
     */
    public static String prepend(String prepend, String s) {
        if (s != null && !s.startsWith(prepend))
            return new StringBuilder(prepend).append(s).toString();
        return s;
    }

    /**
     * Trennzeichen ist '-'. Das erste Zeichen bleibt wie es ist. Beispiel:
     * hallo-liebe-leute => halloLiebeLeute
     */
    public static String camelCase(String string) {
        return camelCase(string, '-', false);
    }

    /**
     * Trennzeichen ist '-'. Beispiel: dies-ist-ein-test, capitalize == true
     * => DiesIstEinTest
     */
    public static String camelCase(String string, boolean capitalize) {
        return camelCase(string, '-', capitalize);
    }

    public static String camelCase(String string, char separator, boolean capitalize) {
        if (string == null)
            return string;
        StringBuilder b = new StringBuilder(string.length());
        boolean upper = capitalize;
        for (char c : string.toCharArray()) {
            if (c != separator) {
                b.append(!upper ? c : Character.toUpperCase(c));
                upper = false;
            } else
                upper = true;
        }
        return b.toString();
    }

    /** Trennzeichen ist '-'. */
    public static String flattenCamelCase(String string) {
        return flattenCamelCase(string, '-');
    }

    public static String flattenCamelCase(String string, char separator) {
        if (string == null)
            return string;
        StringBuilder b = new StringBuilder(string.length() + 5);
        for (char c : string.toCharArray()) {
            if (!Character.isUpperCase(c))
                b.append(c);
            else {
                if (b.length() > 0)
                    b.append(separator);
                b.append(Character.toLowerCase(c));
            }
        }
        return b.toString();
    }

    /**
     * Verknüpft die Stringrepräsentationen der Objekte <i>obj</i> mit dem String <i>chain</i>.
     *
     * @param chain Glied oder null
     */
    public static String chain(String chain, Object... obj) {
        StringBuilder c = new StringBuilder();
        for (Object o : obj) {
            if (o != null && o.toString().length() > 0) {
                if (c.length() > 0)
                    if (chain != null)
                        c.append(chain);
                c.append(o);
            }
        }
        return c.toString();
    }

    public static String chainDot(Object... obj) {
        return chain(".", obj);
    }

    /**
     * Liefert den String <i>s</i> versehen mit dem Präfix <i>prefix</i> falls <i>s</i> nicht
     * leer ist, sonst <i>s</i>.
     * <p/>
     * Beispiel: prefix(null, "pre") -> null; prefix("x", "pre") -> "prex";
     * prefix("", "pre") -> "";
     */
    public static String prefix(String s, String prefix) {
        return !Tool.empty(s)
                ? prefix + s
                : s;
    }

    public static String extract(String string, String regex) {
        Matcher m = Pattern.compile(regex).matcher(string);
        m.find();
        try {
            return m.groupCount() >= 1 ? m.group(1) : m.group();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Zerlegt einen String. (<code>tokens == null</code>, <code>trim == true</code>, <code>greedyLast == false</code>,
     * <code>keepDelim == false</code>)
     *
     * @see #tokenize(String,String,String[],boolean,boolean,boolean,boolean)
     */
    public static String[] tokenize(String s, String delim) {
        return tokenize(s, delim, null, true, false);
    }

    public static String[] tokenize(String s, String delim, int flags) {
        return tokenize(s, delim, null,
                (flags & TRIM) > 0, (flags & STRIP_EMPTY) > 0, (flags & GREEDY_LAST) > 0, (flags & KEEP_DELIM) > 0);
    }

    /**
     * Zerlegt einen String. (<code>trim == true</code>, <code>greedyLast == false</code>, <code>keepDelim ==
     * false</code>)
     *
     * @return den zerlegten String <code>s</code> als Array oder <code>null</code> falls <code>s == null</code>
     * @see #tokenize(String,String,String[],boolean,boolean,boolean,boolean)
     */
    public static String[] tokenize(String s, String delim, String[] tokens) {
        return tokenize(s, delim, tokens, true, false);
    }

    /**
     * Zerlegt einen String. (<code>greedyLast == false</code>, <code>keepDelim == false</code>)
     *
     * @return den zerlegten String <code>s</code> als Array oder <code>null</code> falls <code>s == null</code>
     * @see #tokenize(String,String,String[],boolean,boolean,boolean,boolean)
     */
    public static String[] tokenize(String s, String delim, String[] tokens, boolean trim) {
        return tokenize(s, delim, tokens, trim, false);
    }

    /**
     * Zerlegt einen String. (<code>keepDelim == false</code>)
     *
     * @return den zerlegten String <code>s</code> als Array oder <code>null</code> falls <code>s == null</code>
     * @see #tokenize(String,String,String[],boolean,boolean,boolean,boolean)
     */
    public static String[] tokenize(String s, String delim, String[] tokens, boolean trim, boolean greedyLast) {
        return tokenize(s, delim, tokens, trim, false, greedyLast, false);
    }

    public static String[] tokenize(String s, String delim, String[] tokens, int flags) {
        return tokenize(s, delim, tokens,
                (flags & TRIM) > 0, (flags & STRIP_EMPTY) > 0, (flags & GREEDY_LAST) > 0, (flags & KEEP_DELIM) > 0);
    }

    /**
     * Zerlegt den String <code>s</code> anhand der Trennzeichen im String <code>delim</code> und übergibt das Ergebnis
     * im Array <code>tokens</code>. Falls <code>tokens == null</code> ist, oder kleiner als erforderlich, so wird ein
     * passendes neues Array erzeugt. Ist <code>trim == true</code> so werden Whitespaces vor und nach einem Token
     * abgeschnitten. Wenn <code>greedyLast == true</code> und <code>tokens</code> zu klein ist, so schluckt das letzte
     * Token den ganzen Rest (mit evtl. noch vorkommenden Trennzeichen). Ist <code>keepDelim == true</code> werden die
     * Trennzeichen ebenfalls zurückgegeben.
     * <p/>
     * Um die Performance zu erhöhen sollten die Trennzeichen absteigend nach der zu erwarteten Häufigkeit geordnet
     * werden, d. h. das häufigste nach vorne
     *
     * @return den zerlegten String <code>s</code> als Array oder <code>null</code> falls <code>s == null</code>
     */
    public static String[] tokenize(String s, String delim, String[] tokens, boolean trim,
                                    boolean stripEmpty, boolean greedyLast, boolean keepDelim) {
        // ohne zu zerlegenden String gibt es keine Tokens
        if (s == null)
            return null;

        // in diesem Vector werden die Tokens gesammelt
        List<String> tokens_list = new ArrayList<String>();

        // max_count gibt an, wieviele Tokens höchstens
        // erzeugt werden sollen, "-1" steht für "keine Beschränkung"
        int token_max_count = tokens != null ? tokens.length : -1;
        int delim_len = delim != null ? delim.length() : 0;
        int s_len = s.length();

        // ohne Trennzeichen gibt es nur ein Token
        // ebenfalls nur ein Token, wenn der String s = "" ist
        if (delim_len == 0 || s_len == 0) {
            if (token_max_count != 0) {
                String add = trim ? s.trim() : s;
                if (!(stripEmpty && Tool.empty(add)))
                    tokens_list.add(add);
            }
        } else {
            // es gibt mindestens ein Delimiter und der zu
            // trennende String enthält mindestens ein Zeichen
            // (also auch mindestens ein Token)
            char[] delim_ch = new char[delim_len];
            char[] s_ch = new char[s_len];
            delim.getChars(0, delim_len, delim_ch, 0);
            s.getChars(0, s_len, s_ch, 0);
            int start_offset = 0;
            for (int offset = 0; offset <= s_len && tokens_list.size() != token_max_count; offset++) {
                String foundDelim = null;
                // prüfen, ob die maximale Anzahl an Tokens erreicht ist
                // und das letzte Token den Rest schlucken soll
                if (greedyLast && tokens_list.size() == token_max_count - 1) {
                    offset = s_len;
                }
                if (offset < s_len) {
                    int d_index;
                    // testen, ob am Index d_offset ein Trennzeichen liegt
                    for (d_index = 0; d_index < delim_len; d_index++) {
                        if (s_ch[offset] == delim_ch[d_index]) {
                            if (keepDelim) {
                                foundDelim = new String(new char[]{delim_ch[d_index]});
                            }
                            break;
                        }
                    }
                    // wenn kein Trennzeichen erkannt worden ist, nächstes Zeichen prüfen
                    if (d_index == delim_len)
                        continue;
                }

                // das Token extrahieren
                String token = new String(s_ch, start_offset, offset - start_offset);
                String trimmed = trim ? token.trim() : token;
                if (!stripEmpty || trimmed.length() > 0)
                    tokens_list.add(trim ? token.trim() : token);
                if (foundDelim != null) {
                    tokens_list.add(foundDelim);
                }
                // Trennzeichen überspringen
                start_offset = offset + 1;
            }
        }

        // das String-Array aus dem String-Vektor erstellen
        if (tokens == null)
            tokens = new String[tokens_list.size()];
        tokens_list.toArray(tokens);
        return tokens;
    }
    
    /**
     * Just checks if a string is empty or null.
     * 
     * @param s The string.
     * @return true of false
     */
    public static boolean isEmpty(String s){
    	return s==null || s.trim().equals("");
    }
}
