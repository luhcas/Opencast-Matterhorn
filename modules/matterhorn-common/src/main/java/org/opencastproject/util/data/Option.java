/**
 *  Copyright 2009, 2010 The Regents of the University of California
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

package org.opencastproject.util.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * The option type encapsulates on optional value. It contains either some value or is empty.
 */
public abstract class Option<A> implements Iterable<A> {

  private Option() {
  }

  public static <A> Option<A> some(final A a) {
    return new Option<A>() {
      @Override
      public <B> B fold(Match<A, B> visitor) {
        return visitor.some(a);
      }

      @Override
      public void foreach(Function<A, Void> f) {
        f.apply(a);
      }

      @Override
      public <B> Option<B> map(Function<A, B> f) {
        return some(f.apply(a));
      }

      @Override
      public <B> Option<B> flatMap(Function<A, Option<B>> f) {
        return f.apply(a);
      }

      @Override
      public boolean isSome() {
        return true;
      }

      @Override
      public A get() {
        return a;
      }

      @Override
      public A getOrElse(A none) {
        return a;
      }

      @Override
      public A getOrElse(Function0<A> none) {
        return a;
      }

      @Override
      public Iterator<A> iterator() {
        return Collections.singletonList(a).iterator();
      }
    };
  }

  public static <A> Option<A> none() {
    return new Option<A>() {
      @Override
      public <B> B fold(Match<A, B> visitor) {
        return visitor.none();
      }

      @Override
      public void foreach(Function<A, Void> f) {
      }

      @Override
      public <B> Option<B> map(Function<A, B> f) {
        return none();
      }

      @Override
      public <B> Option<B> flatMap(Function<A, Option<B>> f) {
        return none();
      }

      @Override
      public boolean isSome() {
        return false;
      }

      @Override
      public A get() {
        throw new IllegalStateException("a none does not contain a value");
      }

      @Override
      public A getOrElse(A none) {
        return none;
      }

      @Override
      public A getOrElse(Function0<A> none) {
        return none.apply();
      }

      @Override
      public Iterator<A> iterator() {
        return new ArrayList<A>().iterator();
      }
    };
  }

  /**
   * Wrap an arbitrary object into an option with <code>null</code> being mapped to none.
   */
  public static <A> Option<A> wrap(A a) {
    if (a != null)
      return some(a);
    else
      return none();
  }

  public interface Match<A, B> {
    B some(A a);

    B none();
  }

  /**
   * Safe decomposition of the option type.
   */
  public abstract <B> B fold(Match<A, B> visitor);

  public abstract void foreach(Function<A, Void> f);

  public abstract <B> Option<B> map(Function<A, B> f);

  public abstract <B> Option<B> flatMap(Function<A, Option<B>> f);

  public abstract boolean isSome();

  public boolean isNone() {
    return !isSome();
  }

  /**
   * Get the contained value or throw an exception.
   */
  public abstract A get();

  /**
   * Get the contained value in case of being "some" or return parameter <code>none</code> otherwise.
   */
  public abstract A getOrElse(A none);

  /**
   * Get the contained value in case of being "some" or return the result of
   * evaluating <code>none</code> otherwise.
   */
  public abstract A getOrElse(Function0<A> none);

  /**
   * Use this function in <code>getOrElse</code> if it is an error being none.
   */
  public static <A> Function0<A> error(final String message) {
    return new Function0<A>() {
      @Override
      public A apply() {
        throw new RuntimeException(message);
      }
    };
  }

}
