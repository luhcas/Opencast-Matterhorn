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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reflection utility class.
 * <p/>
 * Code copied from class <code>appetizer.util.Reflection</code> of project
 * "appetizer", originally create May 22, 2006. Donated to REPLAY by the author.
 * <p/>
 * Todo translate original german documentation
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class ReflectionSupport {

  private ReflectionSupport() {
  }

  private static final int GETTER = 1;
  private static final int SETTER = 2;
  private static final int BOTH = 3;

  /**
   * Copies the properties of <code>source</code> to <code>dest</code>.
   * 
   * @see #copy(Object,Object,String[])
   * @see #init(Object,Object)
   */
  public static void copy(Object source, Object dest) {
    Assert.notNull(source, "source");
    Assert.notNull(dest, "dest");
    //
    _copy(source, dest, false, (String) null);
  }

  /**
   * Kopiert das Objekt <i>source</i> ins Objekt <i>dest</i>. Die Werte, die die
   * Getter von <i>source</i> liefern werden den passenden Settern von
   * <i>dest</i> übergeben. Die Properties in der Liste <i>exclude</i> werden
   * nicht kopiert.
   * 
   * @see #copy(Object,Object)
   * @see #init(Object,Object)
   */
  public static void copy(Object source, Object dest, String... exclude) {
    Assert.notNull(source, "source");
    Assert.notNull(exclude, "exclude");
    //
    _copy(source, dest, false, exclude);
  }

  /**
   * Initialisiert Objekt <i>init</i> durch Objekt <i>from</i>. Es werden nur
   * die Properties von <i>init</i> gesetzt, die noch null sind. Properties, die
   * keinen Getter haben gelten immer als null - werden also immer gesetzt -
   * die, die einen primitiven Typ liefern gelten immer als gesetzt, können
   * demnach nicht initialisiert werden.
   * 
   * @see #copy(Object,Object)
   * @see #copy(Object,Object,String[])
   */
  public static void init(Object init, Object from) {
    _copy(from, init, true);
  }

  /**
   * .
   * 
   * @param source
   *          Quellobjekt
   * @param dest
   *          Zielobjekt
   * @param init
   *          true: nur null-Properties des Zielobjekts werden gesetzt
   * @param exclude
   *          Properties, die nicht kopiert werden sollen
   */
  private static void _copy(Object source, Object dest, boolean init,
      String... exclude) {
    assert source != null;
    assert dest != null;
    //
    METHODS: for (Method sourceMethod : source.getClass().getMethods()) {
      String rawPropertyName = rawProperty(sourceMethod, GETTER);
      if (rawPropertyName != null) {
        // Methode ist ein Getter
        if (exclude != null) {
          // evtl. überspringen, falls das Property ausgenommen ist
          String propertyName = StringSupport.decapitalize(rawPropertyName);
          for (String ex : exclude)
            if (propertyName.equals(ex))
              continue METHODS;
        }
        if (init) {
          // nur Initialisierung
          try {
            Method destGetter = dest.getClass().getMethod(
                sourceMethod.getName(), sourceMethod.getParameterTypes());
            if (destGetter.invoke(dest) != null)
              // hat bereits einen Wert: nicht setzen
              continue METHODS;
          } catch (NoSuchMethodException e) {
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        try {
          // Setter suchen
          Method setter = dest.getClass().getMethod("set" + rawPropertyName,
              sourceMethod.getReturnType());
          // Wert holen
          Object value = sourceMethod.invoke(source);
          // und kopieren
          setter.invoke(dest, value);
        } catch (NoSuchMethodException ignore) {
          // gesuchter Setter wurde nicht gefunden
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * todo evtl. an die neue Pen.fill-Methode anpassen Füllt das Object
   * <var>dest</var> mit den Elementen der Map <var>source</var>. Es werden
   * nacheinander alle öffentlichen Setter von <var>dest</var> (und aller
   * Superklassen) aufgerufen, die ein {@link #isSimpleType(Class) SimpleType}
   * als Argument haben. Komplexe Typen können nicht gesetzt werden, sie werden
   * einfach übergangen. Übergeben wird der Wert aus der Map, der unter dem
   * Propertynamen des Setters gespeichert ist. Gibt es keinen Eintrag in der
   * Map wird null übergeben, <em>außer</em> das Methodenargument ist ein
   * primitiver Typ, dann kann die Methode nicht aufgerufen werden.
   * <p/>
   * Die Methode bricht mit einer
   * {@link java.lang.reflect.InvocationTargetException} ab wenn in einem Setter
   * eine Exception auftritt.
   * <p/>
   * Die Keys der Map sind die Namen der Properties. Bei der Konvertierung der
   * Werte wird verfahren wie {@linkplain #convert hier} beschrieben.
   * 
   * @return die Anzahl der erfolgreich aufgerufenen Setter
   * @throws InvocationTargetException
   *           enthält die, in einem Setter aufgetretene, Exception
   * @see #dump
   */
  public static int fill(Object dest, Map source)
      throws InvocationTargetException {
    Assert.notNull(dest, "dest");
    Assert.notNull(source, "source");
    //
    // alle Methoden durchgehen und aufrufen, falls es sich um Setter handelt
    int callCount = 0;
    for (Method m : dest.getClass().getMethods()) {
      String property = propertySetter(m);
      if (property == null)
        continue;
      // Methode ist ein Setter
      Class argType = m.getParameterTypes()[0];
      if (isComplexType(argType))
        // komplexe Typen können nicht gefüllt werden
        continue;
      Object value = source.get(property);
      Object arg = value != null ? convert(argType, value) : null;
      if (arg == null && argType.isPrimitive())
        // Setter mit primitiven Argumenten können nicht aufgerufen werden wenn
        // das Argument null ist
        continue;
      // noinspection RedundantArrayCreation
      try {
        m.invoke(dest, new Object[] { arg });
        callCount++;
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return callCount;
  }

  /**
   * Füllt die Map <var>dest</var> mit den Werten des Beans <var>source</var>.
   * Alle Property-Getter von <var>source</var> werden aufgerufen und die
   * gelieferten Werte unter dem Propertynamen in der Map gespeichert.
   * Properties mit dem Wert null werden ebenfalls in die Map eingetragen, wenn
   * die Map null-Values zulässt.
   * <p/>
   * Die Methode bricht mit einer
   * {@link java.lang.reflect.InvocationTargetException} ab wenn in einem Getter
   * eine Exception auftritt.
   * 
   * @return die Anzahl der erfolgreich aufgerufenen Getter
   * @throws InvocationTargetException
   *           enthält die, in einem Getter aufgetretene, Exception
   * @see #fill
   */
  public static int dump(Object source, Map dest)
      throws InvocationTargetException {
    Assert.notNull(source, "source");
    Assert.notNull(dest, "dest");
    //
    // alle Methoden durchgehen und aufrufen falls es sich um Getter handelt
    int callCount = 0;
    for (Method m : source.getClass().getMethods()) {
      String propertyName = propertyGetter(m);
      if (propertyName == null)
        continue;
      Object property;
      try {
        property = m.invoke(source);
        callCount++;
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      try {
        dest.put(propertyName, property);
      } catch (NullPointerException ignore) {
      }
    }
    return callCount;
  }

  /**
   * Setzt das Property <var>name</var> des Objekts <var>dest</var> mit dem Wert
   * <var>value</var>.
   * 
   * @throws InvocationTargetException
   *           enthält die, im Setter aufgetretene, Exception
   */
  public static void setProperty(String name, Object dest, Object value)
      throws InvocationTargetException {
    Method m = findSetter(dest, name, value.getClass(), true);
    if (m == null)
      throw new IllegalArgumentException(name + ": "
          + value.getClass().getName() + " is not a property of "
          + dest.getClass().getName());
    try {
      try {
        m.setAccessible(true);
      } catch (SecurityException ignore) {
      }
      m.invoke(dest, value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Liefert das Property <var>name</var> des Objekts <var>source</var>. Es ist
   * zu beachten, daß bei Properties, die einen primitiven Typ haben immer der
   * entsprechende Objekttyp geliefert wird.
   * 
   * @throws InvocationTargetException
   *           enthält die, im Getter aufgetretene, Exception
   */
  public static Object getProperty(String name, Object source)
      throws InvocationTargetException {
    Method m = findGetter(source, name, null);
    if (m == null)
      throw new IllegalArgumentException("'" + name + "' is not a property of "
          + source);
    try {
      return m.invoke(source);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Eine Methode ist ein Getter wenn sie keine Parameter hat, eine Wert
   * zurückgibt und ihr Name dem Muster <i>get[A-Z].*</i>, oder sie einen
   * boolschen Wert liefert und ihr Name dem Muster <i>is[A-Z].*</i> entspricht.
   */
  public static boolean isGetter(Method m) {
    Assert.notNull(m, "m");
    //
    return propertyIndex(m, GETTER) != -1;
  }

  /**
   * Prüft, ob die Methode <i>m</i> ein Getter des Properties <i>property</i>
   * ist.
   * 
   * @see #isGetter(java.lang.reflect.Method)
   */
  public static boolean isGetterOf(Method m, String property) {
    Assert.notNull(m, "m");
    Assert.notNull(property, "property");
    //
    return property.equals(StringSupport.decapitalize(rawProperty(m, GETTER)));
  }

  /**
   * Eine Methode ist ein Setter wenn sie genau einen Parameter hat und ihr Name
   * dem Muster <i>set[A-Z].*</i> entspricht.
   */
  public static boolean isSetter(Method m) {
    Assert.notNull(m, "m");
    //
    return propertyIndex(m, SETTER) != -1;
  }

  /**
   * Prüft, ob die Methode <i>m</i> ein Setter des Properties <i>property</i>
   * ist.
   * 
   * @see #isGetter(java.lang.reflect.Method)
   */
  public static boolean isSetterOf(Method m, String property) {
    Assert.notNull(m, "m");
    Assert.notNull(property, "property");
    //
    return property.equals(StringSupport.decapitalize(rawProperty(m, SETTER)));
  }

  /** Prüft, ob die Methode <i>m</i> einen bool'schen Wert liefert. */
  public static boolean returnsBoolean(Method m) {
    Assert.notNull(m, "m");
    //
    return isBoolean(m.getReturnType());
  }

  /** Prüft, ob die Methode <i>m</i> genau einen bool'schen Wert annimmt. */
  public static boolean takesBoolean(Method m) {
    Assert.notNull(m, "m");
    //
    return m.getParameterTypes().length == 1
        && isBoolean(m.getParameterTypes()[0]);
  }

  /**
   * Prüft, ob die Klasse <i>c</i> einen bool'schen Wert repräsentiert.
   * 
   * @param c
   *          darf null sein
   */
  public static boolean isBoolean(Class c) {
    return Boolean.class.equals(c) || boolean.class.equals(c);
  }

  /**
   * Sucht in der Klasse des Objekts <i>o</i> (oder <i>o</i> selbst, falls
   * <i>o</i> vom Typ Class) nach dem passenden Getter zum Setter <i>setter</i>.
   * 
   * @param o
   *          Objekt oder Klasse
   * @return Getter oder null
   */
  public static Method findMatchingGetter(Method setter, Object o) {
    Assert.notNull(setter, "setter");
    Assert.notNull(o, "o");
    //
    String raw = rawProperty(setter, SETTER);
    if (raw != null) {
      Class c = o instanceof Class ? ((Class) o) : o.getClass();
      try {
        Method getter = c.getMethod("get" + raw);
        if (setter.getParameterTypes()[0].isAssignableFrom(getter
            .getReturnType()))
          return getter;
      } catch (NoSuchMethodException e) {
      }
    }
    return null;
  }

  /**
   * Sucht in der Klasse des Objekts <i>o</i> (oder <i>o</i> selbst, falls
   * <i>o</i> vom Typ Class) nach dem passenden Setter zum Getter <i>getter</i>.
   * 
   * @param o
   *          Objekt oder Klasse
   * @return Setter oder null
   */
  public static Method findMatchingSetter(Method getter, Object o) {
    Assert.notNull(getter, "getter");
    Assert.notNull(o, "o");
    //
    String raw = rawProperty(getter, GETTER);
    if (raw != null) {
      Class c = o instanceof Class ? ((Class) o) : o.getClass();
      try {
        return c.getMethod("set" + raw, getter.getReturnType());
      } catch (NoSuchMethodException e) {
      }
    }
    return null;
  }

  /**
   * Sucht in der Klasse von <var>o</var> (oder <var>o</var> selbst, falls vom
   * Typ Class) nach dem Getter des Properties <var>property</var> vom Typ
   * <var>propertyType</var>.
   * 
   * @param propertyType
   *          Der Typ des gesuchten Properties. Ist der Typ egal: null.
   * @return Getter oder null
   */
  public static Method findGetter(Object o, String property, Class propertyType) {
    Assert.notNull(o, "o");
    Assert.notNull(property, "property");
    //
    String mNameSuffix = StringSupport.capitalize(property);
    if (isBoolean(propertyType)) {
      Method m = _findGetter(o, "is" + mNameSuffix, propertyType);
      if (m != null)
        return m;
    }
    return _findGetter(o, "get" + mNameSuffix, propertyType);
  }

  private static Method _findGetter(Object o, String methodName,
      Class propertyType) {
    try {
      Class c = o instanceof Class ? ((Class) o) : o.getClass();
      Method m = c.getMethod(methodName);
      if (propertyIndex(m, GETTER) != -1
          && (propertyType == null || m.getReturnType().equals(propertyType)))
        return m;
    } catch (NoSuchMethodException ignore) {
    }
    return null;
  }

  /**
   * Sucht in der Klasse des Objekts <var>o</var> (oder <var>o</var> selbst,
   * falls <var>o</var> vom Typ Class) nach dem Setter des Properties
   * <var>property</var> vom Typ <var>propertyType</var>.
   * 
   * @return Setter oder null
   */
  public static Method findSetter(Object o, String property, Class propertyType) {
    return findSetter(o, property, propertyType, false);
  }

  /**
   * Sucht in der Klasse des Objekts <var>o</var> (oder <var>o</var> selbst,
   * falls <var>o</var> vom Typ Class) nach dem Setter des Properties
   * <var>property</var> vom Typ <var>propertyType</var>. Bei Typen, die über
   * einen passenden primitiven Type verfügen wird auch probiert diesen Setter
   * zu finden und umgekehrt.
   * 
   * @return Setter oder null
   */
  public static Method findSetter(Object o, String property,
      Class propertyType, boolean autobox) {
    Assert.notNull(o, "o");
    Assert.notNull(property, "property");
    Assert.notNull(propertyType, "propertyType");
    //
    String mNameSuffix = StringSupport.capitalize(property);
    String mName = "set" + mNameSuffix;
    Class c = o instanceof Class ? (Class) o : o.getClass();
    try {
      return c.getMethod(mName, propertyType);
    } catch (NoSuchMethodException ignore) {
    }
    if (autobox) {
      Class autoboxed = autobox(propertyType);
      try {
        return c.getMethod(mName, autoboxed);
      } catch (NoSuchMethodException ignore) {
      }
    }
    return null;
  }

  /**
   * Liefert alle Methoden des Namens <var>name</var> der Klasse <var>c</var>.
   * Gibt es keine Methoden ist die Collection leer.
   */
  public static Collection<Method> findMethodsByName(Class c, String name) {
    Assert.notNull(c, "class");
    //
    Collection<Method> found = new ArrayList<Method>();
    for (Method m : c.getMethods()) {
      if (m.getName().equals(name))
        found.add(m);
    }
    return found;
  }

  /**
   * Liefert den Propertynamen der Setter- bzw. Gettermethode <var>m</var> oder
   * null, falls <var>m</var> keine Setter- bzw. Gettermethode ist.
   * <p/>
   * Beispiel:
   * <code>getName() -> name; getGreenFish() -> greenFish; doThis() -> null</code>
   */
  public static String property(Method m) {
    Assert.notNull(m, "m");
    //
    return StringSupport.decapitalize(rawProperty(m, BOTH));
  }

  /**
   * Liefert den Propertynamen des Setter- bzw. Gettermethodennamens
   * <var>m</var> oder null, falls <var>m</var> kein Setter- bzw.
   * Gettermethodenname ist.
   * <p/>
   * Beispiel:
   * <code>getName -> name; getGreenFish -> greenFish; doThis -> null</code>
   */
  public static String property(String m) {
    Assert.notNull(m, "m");
    //
    String e = StringSupport.extract(m, "^(?:set|get|is)(.+)");
    return e != null ? StringSupport.decapitalize(e) : e;
  }

  /**
   * Liefert den Propertynamen des Getters <var>getter</var> oder null, falls
   * <var>getter</var> kein Getter ist.
   * <p/>
   * Beispiel:
   * <code>getName() -> name; getGreenFish() -> greenFish; setName(...) -> null</code>
   */
  public static String propertyGetter(Method getter) {
    Assert.notNull(getter, "getter");
    //
    return StringSupport.decapitalize(rawProperty(getter, GETTER));
  }

  /**
   * Liefert den Propertynamen des Setters <var>setter</var> oder null, falls
   * <var>setter</var> kein Setter ist. Beispiel: <i>getName()</i> ->
   * <i>name</i>; <i>getGreenFish()</i> -> <i>greenFish</i>; <i>setName(...)</i>
   * -> null
   */
  public static String propertySetter(Method setter) {
    Assert.notNull(setter, "setter");
    //
    return StringSupport.decapitalize(rawProperty(setter, SETTER));
  }

  /**
   * Liefert den Startindex des "rohen" Properties.
   * 
   * @param constraint
   *          {@link #GETTER}: <var>m</var> muss Getter sein | {@link #SETTER}:
   *          muss Setter sein | {@link #BOTH}: egal
   * @return Index oder -1
   * @see #rawProperty(java.lang.reflect.Method,int)
   */
  private static int propertyIndex(Method m, int constraint) {
    assert m != null;
    assert constraint >= 1 && constraint <= 3;
    //
    if ((constraint & GETTER) > 0 && m.getParameterTypes().length == 0
        && !m.getReturnType().equals(void.class)) {
      if (m.getName().matches("get[A-Z].*"))
        return 3;
      if (m.getName().matches("is[A-Z].*") && returnsBoolean(m))
        return 2;
    } else if ((constraint & SETTER) > 0 && m.getParameterTypes().length == 1
        && m.getName().matches("set[A-Z].*"))
      return 3;
    return -1;
  }

  /**
   * Liefert den "rohen" Propertynamen, d.h. es wird nur das Präfix set|get|is
   * abgeschnitten.
   * 
   * @param constraint
   *          {@link #GETTER}: <var>m</var> muss Getter sein | {@link #SETTER}:
   *          muss Setter sein | {@link #BOTH}: egal
   * @return Propertyname oder null
   */
  private static String rawProperty(Method m, int constraint) {
    int i = propertyIndex(m, constraint);
    return i != -1 ? m.getName().substring(i) : null;
  }

  /**
   * Liefert den Namen der Methode in der <var>methodName</var> aufgerufen
   * wurde.
   */
  public static String methodName() {
    Exception e = new Exception();
    return e.getStackTrace()[1].getMethodName();
  }

  /**
   * Liefert den Namen der Methode, die die Methode aufgerufen hat in der
   * <var>methodName</var> aufgerufen wurde.
   */
  public static String callerMethodName() {
    Exception e = new Exception();
    return e.getStackTrace()[2].getMethodName();
  }

  //

  private static interface Converter {

    /**
     * Konvertiert <var>v</var> in ein Objekt vom Typ <var>type</var> oder
     * liefert null, falls <var>v</var> nicht konvertiert werden kann.
     */
    Object convert(Class type, Object v);
  }

  private static final Map<Class, Class> PrimitiveToObject = new MapBuilder<Class, Class>()
      .put(int.class, Integer.class).put(int[].class, Integer[].class).put(
          long.class, Long.class).put(long[].class, Long[].class).put(
          short.class, Short.class).put(short[].class, Short[].class).put(
          float.class, Float.class).put(float[].class, Float[].class).put(
          double.class, Double.class).put(double[].class, Double[].class).put(
          boolean.class, Boolean.class).put(boolean[].class, Boolean[].class)
      .toMap();

  private static final Map<Class, Class> ObjectToPrimitive = CollectionSupport
      .swap(PrimitiveToObject);

  /** Konverter, abgelegt nach ihrem Zieltypen. */
  private static final Map<Class, Converter> SimpleNonArrayTypes = new MapBuilder<Class, Converter>(
      new HashMap<Class, Converter>())
  // String-Converter
      .put(String.class, new Converter() {
        public Object convert(Class type, Object v) {
          return v;
        }
      }).putMultiple(
          new Converter() {
            public Object convert(Class type, Object v) {
              Class objType = box(type);
              // Ist v == null kann es - bei einem primitiven Typen - nicht
              // konvertiert werden,
              // bzw. es bleibt wie es ist.
              if (v == null) {
                return null;
              }
              // Passt v zu type? Denn einfach zurückgeben.
              else if (objType.isAssignableFrom(v.getClass())) {
                return v;
              }
              // Oder ist v ein String?
              else if (v instanceof String) {
                try {
                  Method converter = objType.getMethod("valueOf", String.class);
                  return converter.invoke(objType, v);
                } catch (InvocationTargetException e) {
                  if (e.getCause() instanceof NumberFormatException)
                    return null;
                } catch (Exception ignore) {
                  //
                }
              }
              throw new RuntimeException("error converting '" + v
                  + "' of type " + v.getClass().getName() + " to "
                  + type.getName());
            }
          }, Integer.class, int.class, Long.class, long.class, Short.class,
          short.class, Float.class, float.class, Double.class, double.class,
          Boolean.class, boolean.class).toMap();

  private static final Set<Class> SimpleArrayTypes = CollectionSupport
      .<Class, Set<Class>> addAll(new HashSet<Class>(), String[].class,
          Integer[].class, int[].class, Long[].class, long[].class,
          Short[].class, short[].class, Float[].class, float[].class,
          Double[].class, double[].class, Boolean[].class, boolean[].class);

  private static final Set<Class> SimpleTypes = CollectionSupport
      .<Class, Set<Class>> addAll(new HashSet<Class>(), SimpleNonArrayTypes
          .keySet(), SimpleArrayTypes);

  /**
   * Verpackt den primitive Datentypen <var>type</var> in den passenden
   * Objekt-Typen. Ist <var>type</var> kein primitiver Typ wird er einfach
   * zurückgegeben.
   * <p/>
   * Beispiel: <code>int -> Integer; short[] -> Short[]; String -> String</code>
   */
  public static Class box(Class type) {
    Assert.notNull(type, "type");
    //
    return type.isPrimitive() ? PrimitiveToObject.get(type) : type;
  }

  /**
   * Liefert für Objekttypen den passenden primitiven Typen. Primitive Typen
   * werden einfach zurückgegeben. Hat das Objekt keine primitive Entsprechung
   * wird null geliefert.
   * <p/>
   * Beispiel:
   * <code>Integer -> int; Short[] -> short[]; int -> int; String -> null</code>
   * 
   * @return primitiver Typ oder null
   */
  public static Class unbox(Class type) {
    Assert.notNull(type, "type");
    //
    return !type.isPrimitive() ? ObjectToPrimitive.get(type) : type;
  }

  /** Fasst die Methode {@link #box} und {@link #unbox} zusammen. */
  public static Class autobox(Class type) {
    Assert.notNull(type, "type");
    //
    if (type.isPrimitive())
      return PrimitiveToObject.get(type);
    else
      return ObjectToPrimitive.get(type);
  }

  /**
   * Die Simple-Types sind:
   * <ul>
   * <li>String
   * <li>Integer, int
   * <li>Long, long
   * <li>Short, short
   * <li>Float, float
   * <li>Double, double,
   * <li>Boolean, boolean
   * </ul>
   * und die jeweiligen Array-Typen.
   * 
   * @see #isComplexType(Class)
   */
  public static boolean isSimpleType(Class t) {
    return SimpleTypes.contains(t);
  }

  /**
   * Alles was kein Simple-Type ist.
   * 
   * @see #isSimpleType(Class)
   */
  public static boolean isComplexType(Class t) {
    return t != null && !SimpleTypes.contains(t);
  }

  /**
   * Konvertiert <var>value</var> in ein Objekt vom Typ <var>simpleType</var>.
   * Hierbei wird wie folgt vorgegangen:
   * <ul>
   * <li><var>value</var> ist vom Typ <var>simpleType</var> -> Rückgabe
   * <var>value</var>
   * <li>ist <var>value</var> vom Typ String, wird versucht <var>value</var> zu
   * konvertieren
   * <li>ist <var>simpleType</var> ein Arraytyp wird <var>value</var> oder die
   * Werte von <var>value</var> in dieses Array konvertiert
   * <li>ist <var>simpleType</var> <em>kein</em> Arraytyp aber <var>value</var>,
   * so wird nur der erste Wert von <var>value</var> konvertiert
   * </ul>
   * 
   * @return das Konvertierungsergebnis oder null falls <var>value</var> nicht
   *         konvertiert werden konnte
   */
  public static Object convert(Class simpleType, Object value) {
    Assert.notNull(simpleType, "simpleType");
    Assert.that(isSimpleType(simpleType), "not a simple type "
        + simpleType.getName());
    Assert.notNull(value, "value");
    // Assert.that(value instanceof String || value instanceof String[],
    // "invalid value type");
    //
    if (simpleType.isArray()) {
      return _convertArray(simpleType, value.getClass().isArray() ? value
          : new Object[] { value });
    } else if (value.getClass().isArray()) {
      if (Array.getLength(value) != 1)
        return null;
      value = Array.get(value, 0);
      // if (value == null)
      // throw new
      // IllegalArgumentException("First element in value array is null.");
    }
    return _convert(simpleType, value);
  }

  /**
   * Konvertiert den String <var>value</var> in den Simple-Type
   * <var>simpleType</var>. Bei einem angeforderten Array wird Index 0 mit dem
   * Konvertierungsergebnis belegt. Von primitiven Typen wird der entsprechenden
   * Objekttyp geliefert.
   * 
   * @return das Konvertierungsergebnis oder null falls <var>value</var> nicht
   *         konvertiert werden konnte
   * @throws IllegalArgumentException
   *           <var>simpleType</var> ist kein Simple-Type
   * @see #isSimpleType(Class)
   */
  private static Object _convert(Class simpleType, Object value) {
    assert simpleType != null;
    assert !simpleType.isArray();
    // assert value != null;
    //
    Converter converter = SimpleNonArrayTypes.get(simpleType);
    return converter.convert(simpleType, value);
  }

  /**
   * Konvertiert jeden Wert aus <var>values</var> in einen Simple-Type
   * <var>simpleType</var> und speichert ihn in einem Array vom Typ
   * <code>simpleType[]</code> am gleichen Index. Werte, die nicht konvertiert
   * werden können, werden nicht im Array gespeichert, so daß deren Indizes
   * null bleiben, bzw. bei primitiven Typen die jeweiligen Initialwerte
   * behalten.
   * 
   * @return Array der Länge <code>values.length</code>
   * @throws IllegalArgumentException
   *           <var>simpleType</var> ist kein Simple-Type
   * @see #isSimpleType(Class)
   */
  private static Object _convertArray(Class simpleType, Object values) {
    assert simpleType != null;
    assert simpleType.isArray();
    assert values != null;
    //
    Class componentType = simpleType.getComponentType();
    int valuesLength = Array.getLength(values);
    Object array = Array.newInstance(componentType, valuesLength);
    for (int i = 0; i < valuesLength; i++) {
      Object value = Array.get(values, i);
      if (value != null) {
        Object converted = _convert(componentType, value);
        if (converted != null)
          Array.set(array, i, converted);
      }
    }
    return array;
  }
}
