/*
 * BabyFish, Object Model Framework for Java and JPA.
 * https://github.com/babyfish-ct/babyfish
 *
 * Copyright (c) 2008-2016, Tao Chen
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * Please visit "http://opensource.org/licenses/LGPL-3.0" to know more.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 */
package org.babyfish.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.babyfish.lang.spi.UsingInstrumenter;

/**
 * Example:
 * <pre>
 * public class YourModule {
 * 
 *    &#64;I18N
 *    private static native String message1(Class&lt;?&gt; a);
 * 
 *    &#64;I18N
 *    public static native String message2(int a, int b, Class&lt;?&gt;[] c);
 * }
 * </pre>
 * @author Tao Chen
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@UsingInstrumenter("org.babyfish.lang.i18n.instrument.TypedI18NInstrumenter")
public @interface I18N {}