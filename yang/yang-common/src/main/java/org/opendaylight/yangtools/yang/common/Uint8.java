/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.common;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.concepts.Variant;

/**
 * Dedicated type for YANG's 'type uint8' type.
 *
 * @author Robert Varga
 */
@Beta
@NonNullByDefault
public class Uint8 extends Number implements CanonicalValue<Uint8> {
    public static final class Support extends AbstractCanonicalValueSupport<Uint8> {
        public Support() {
            super(Uint8.class);
        }

        @Override
        public Variant<Uint8, CanonicalValueViolation> fromString(final String str) {
            try {
                return Variant.ofFirst(Uint8.valueOf(str));
            } catch (IllegalArgumentException e) {
                return CanonicalValueViolation.variantOf(e);
            }
        }
    }

    private static final CanonicalValueSupport<Uint8> SUPPORT = new Support();

    private static final short MAX_VALUE_SHORT = 255;
    private static final String MAX_VALUE_STR = "255";

    private static final long serialVersionUID = 1L;

    private static final @NonNull Uint8[] CACHE;

    static {
        final Uint8[] c = new Uint8[MAX_VALUE_SHORT + 1];
        for (int i = 0; i <= MAX_VALUE_SHORT; ++i) {
            c[i] = new Uint8((byte)i);
        }
        CACHE = c;
    }

    public static final Uint8 ZERO = valueOf(0);
    public static final Uint8 ONE = valueOf(1);
    public static final Uint8 MAX_VALUE = valueOf(MAX_VALUE_SHORT);

    private final byte value;

    private Uint8(final byte value) {
        this.value = value;
    }

    protected Uint8(final Uint8 other) {
        this(other.value);
    }

    private static Uint8 instanceFor(final byte value) {
        return CACHE[Byte.toUnsignedInt(value)];
    }

    /**
     * Returns an {@code Uint8} corresponding to a given bit representation. The argument is interpreted as an
     * unsigned 8-bit value.
     *
     * @param bits unsigned bit representation
     * @return A Uint8 instance
     */
    public static Uint8 fromByteBits(final byte bits) {
        return instanceFor(bits);
    }

    /**
     * Returns an {@code Uint8} corresponding to a given {@code byteVal}. The inverse operation is {@link #byteValue()}.
     *
     * @param byteVal byte value
     * @return A Uint8 instance
     * @throws IllegalArgumentException if byteVal is less than zero
     */
    public static Uint8 valueOf(final byte byteVal) {
        UintConversions.checkNonNegative(byteVal, MAX_VALUE_STR);
        return instanceFor(byteVal);
    }

    /**
     * Returns an {@code Uint8} corresponding to a given {@code shortVal}. The inverse operation is
     * {@link #shortValue()}.
     *
     * @param shortVal short value
     * @return A Uint8 instance
     * @throws IllegalArgumentException if shortVal is less than zero or greater than 255.
     */
    public static Uint8 valueOf(final short shortVal) {
        UintConversions.checkRange(shortVal, MAX_VALUE_SHORT);
        return instanceFor((byte)shortVal);
    }

    /**
     * Returns an {@code Uint8} corresponding to a given {@code intVal}. The inverse operation is {@link #intValue()}.
     *
     * @param intVal int value
     * @return A Uint8 instance
     * @throws IllegalArgumentException if intVal is less than zero or greater than 255.
     */
    public static Uint8 valueOf(final int intVal) {
        UintConversions.checkRange(intVal, MAX_VALUE_SHORT);
        return instanceFor((byte)intVal);
    }

    /**
     * Returns an {@code Uint8} corresponding to a given {@code longVal}. The inverse operation is
     * {@link #longValue()}.
     *
     * @param longVal long value
     * @return A Uint8 instance
     * @throws IllegalArgumentException if intVal is less than zero or greater than 255.
     */
    public static Uint8 valueOf(final long longVal) {
        UintConversions.checkRange(longVal, MAX_VALUE_SHORT);
        return instanceFor((byte)longVal);
    }

    /**
     * Returns an {@code Uint8} corresponding to a given {@code uint}.
     *
     * @param uint Uint16 value
     * @return A Uint8 instance
     * @throws NullPointerException if uint is null
     * @throws IllegalArgumentException if uint is greater than 255.
     */
    public static Uint8 valueOf(final Uint16 uint) {
        return valueOf(uint.intValue());
    }

    /**
     * Returns an {@code Uint8} corresponding to a given {@code uint}.
     *
     * @param uint Uint32 value
     * @return A Uint8 instance
     * @throws NullPointerException if uint is null
     * @throws IllegalArgumentException if uint is greater than 255.
     */
    public static Uint8 valueOf(final Uint32 uint) {
        return valueOf(uint.longValue());
    }

    /**
     * Returns an {@code Uint8} corresponding to a given {@code uint}.
     *
     * @param uint Uint64 value
     * @return A Uint8 instance
     * @throws NullPointerException if uint is null
     * @throws IllegalArgumentException if uint is greater than 255.
     */
    public static Uint8 valueOf(final Uint64 uint) {
        return valueOf(uint.longValue());
    }

    /**
     * Returns an {@code Uint8} holding the value of the specified {@code String}, parsed as an unsigned {@code short}
     * value.
     *
     * @param string String to parse
     * @return A Uint8 instance
     * @throws NullPointerException if string is null
     * @throws IllegalArgumentException if the parsed value is less than zero or greater than 255
     * @throws NumberFormatException if the string does not contain a parsable unsigned {@code short} value.
     */
    public static Uint8 valueOf(final String string) {
        return valueOf(string, 10);
    }

    /**
     * Returns an {@code Uint8} holding the value of the specified {@code String}, parsed as an unsigned {@code short}
     * value.
     *
     * @param string String to parse
     * @param radix Radix to use
     * @return A Uint8 instance
     * @throws NullPointerException if string is null
     * @throws IllegalArgumentException if the parsed value is less than zero or greater than 255
     * @throws NumberFormatException if the string does not contain a parsable unsigned {@code short} value, or if the
     *                               {@code radix} is outside of allowed range.
     */
    public static Uint8 valueOf(final String string, final int radix) {
        return valueOf(Short.parseShort(requireNonNull(string), radix));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The inverse operation is {@link #fromByteBits(byte)}. In case this value is greater than {@link Byte#MAX_VALUE},
     * the returned value will be equal to {@code this - 2^8}.
     */
    @Override
    public final byte byteValue() {
        return value;
    }

    @Override
    public final int intValue() {
        return Byte.toUnsignedInt(value);
    }

    @Override
    public final long longValue() {
        return Byte.toUnsignedLong(value);
    }

    @Override
    public final float floatValue() {
        return intValue();
    }

    @Override
    public final double doubleValue() {
        return intValue();
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public final int compareTo(final Uint8 o) {
        return Byte.compareUnsigned(value, o.value);
    }

    @Override
    public final String toCanonicalString() {
        return Integer.toString(intValue());
    }

    @Override
    public final CanonicalValueSupport<Uint8> support() {
        return SUPPORT;
    }

    /**
     * Convert this value to a {@code short}.
     *
     * @return A short
     */
    public final short toJava() {
        return shortValue();
    }

    @Override
    public final int hashCode() {
        return Byte.hashCode(value);
    }

    @Override
    public final boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof Uint8 && value == ((Uint8)obj).value;
    }

    /**
     * A slightly faster version of {@link #equals(Object)}.
     *
     * @param obj Uint8 object
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     */
    public final boolean equals(final @Nullable Uint8 obj) {
        return this == obj || obj != null && value == obj.value;
    }

    @Override
    public final String toString() {
        return toCanonicalString();
    }

    private Object readResolve() {
        return instanceFor(value);
    }
}
