/*
// $Id$
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// Copyright (C) 2011-2011 Julian Hyde and others
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package mondrian.util;

import junit.framework.TestCase;

import java.util.*;

/**
 * Unit test for {@link PartiallyOrderedSet}.
 *
 * @version $Id$
 */
public class PartiallyOrderedSetTest extends TestCase {
    private static final boolean debug = false;
    private final int SCALE = 250; // 100, 1000, 3000 are reasonable values
    final long seed = new Random().nextLong();
    final Random random = new Random(seed);

    static final PartiallyOrderedSet.Ordering<String> stringSubsetOrdering =
        new PartiallyOrderedSet.Ordering<String>() {
            public boolean lessThan(String e1, String e2) {
                // e1 < e2 if every char in e1 is also in e2
                for (int i = 0; i < e1.length(); i++) {
                    if (e2.indexOf(e1.charAt(i)) < 0) {
                        return false;
                    }
                }
                return true;
            }
        };

    // Integers, ordered by division. Top is 1, its children are primes,
    // etc.
    static final PartiallyOrderedSet.Ordering<Integer> isDivisor =
        new PartiallyOrderedSet.Ordering<Integer>() {
            public boolean lessThan(Integer e1, Integer e2) {
                return e2 % e1 == 0;
            }
        };

    // Bottom is 1, parents are primes, etc.
    static final PartiallyOrderedSet.Ordering<Integer> isDivisorInverse =
        new PartiallyOrderedSet.Ordering<Integer>() {
            public boolean lessThan(Integer e1, Integer e2) {
                return e1 % e2 == 0;
            }
        };

    // Ordered by bit inclusion. E.g. the children of 14 (1110) are
    // 12 (1100), 10 (1010) and 6 (0110).
    static final PartiallyOrderedSet.Ordering<Integer> isBitSubset =
        new PartiallyOrderedSet.Ordering<Integer>() {
            public boolean lessThan(Integer e1, Integer e2) {
                return (e2 & e1) == e2;
            }
        };

    public PartiallyOrderedSetTest(String s) {
        super(s);
    }

    public void testPoset() {
        String empty = "''";
        String abcd = "'abcd'";
        PartiallyOrderedSet<String> poset =
            new PartiallyOrderedSet<String>(stringSubsetOrdering);
        assertEquals(0, poset.size());

        poset.add("a");
        printValidate(poset);
        poset.add("b");
        printValidate(poset);

        poset.clear();
        assertEquals(0, poset.size());
        poset.add(empty);
        printValidate(poset);
        poset.add(abcd);
        printValidate(poset);
        assertEquals(2, poset.size());
        assertEquals("['abcd']", poset.getNonChildren().toString());
        assertEquals("['']", poset.getNonParents().toString());

        final String ab = "'ab'";
        poset.add(ab);
        printValidate(poset);
        assertEquals(3, poset.size());
        assertEquals("[]", poset.getChildren(empty).toString());
        assertEquals("['ab']", poset.getParents(empty).toString());
        assertEquals("['ab']", poset.getChildren(abcd).toString());
        assertEquals("[]", poset.getParents(abcd).toString());
        assertEquals("['']", poset.getChildren(ab).toString());
        assertEquals("['abcd']", poset.getParents(ab).toString());

        // "bcd" is child of "abcd" and parent of ""
        final String bcd = "'bcd'";
        poset.add(bcd);
        printValidate(poset);
        assertTrue(poset.isValid(false));
        assertEquals("['']", poset.getChildren(bcd).toString());
        assertEquals("['abcd']", poset.getParents(bcd).toString());
        assertEquals("['ab', 'bcd']", poset.getChildren(abcd).toString());

        final String b = "'b'";
        poset.add(b);
        printValidate(poset);
        assertEquals("['abcd']", poset.getNonChildren().toString());
        assertEquals("['']", poset.getNonParents().toString());
        assertEquals("['']", poset.getChildren(b).toString());
        assertEquals(
            "['ab', 'bcd']",
            new TreeSet<String>(poset.getParents(b)).toString());
        assertEquals("['']", poset.getChildren(b).toString());
        assertEquals("['ab', 'bcd']", poset.getChildren(abcd).toString());
        assertEquals("['b']", poset.getChildren(bcd).toString());
        assertEquals("['b']", poset.getChildren(ab).toString());
    }

    public void testPosetTricky() {
        PartiallyOrderedSet<String> poset =
            new PartiallyOrderedSet<String>(stringSubsetOrdering);

        // A tricky little poset with 4 elements:
        // {a <= ab and ac, b < ab, ab, ac}
        poset.clear();
        poset.add("'a'");
        printValidate(poset);
        poset.add("'b'");
        printValidate(poset);
        poset.add("'ac'");
        printValidate(poset);
        poset.add("'ab'");
        printValidate(poset);
    }

    public void testDivisorPoset() {
        PartiallyOrderedSet<Integer> integers =
            new PartiallyOrderedSet<Integer>(isDivisor, range(1, 1000));
        assertEquals(
            "[1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 24, 30, 40, 60]",
            new TreeSet<Integer>(integers.getDescendants(120)).toString());
        assertEquals(
            "[240, 360, 480, 600, 720, 840, 960]",
            new TreeSet<Integer>(integers.getAncestors(120)).toString());
        assertTrue(integers.isValid(true));
    }

    public void testDivisorSeries() {
        checkPoset(isDivisor, debug, range(1, SCALE * 3));
    }

    public void testDivisorRandom() {
        boolean ok = false;
        try {
            checkPoset(isDivisor, debug, random(random, SCALE, SCALE * 3));
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    public void testDivisorInverseSeries() {
        checkPoset(isDivisorInverse, debug, range(1, SCALE * 3));
    }

    public void testDivisorInverseRandom() {
        boolean ok = false;
        try {
            checkPoset(
                isDivisorInverse, debug, random(random, SCALE, SCALE * 3));
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    public void testSubsetSeries() {
        checkPoset(isBitSubset, debug, range(1, SCALE / 2));
    }

    public void testSubsetRandom() {
        boolean ok = false;
        try {
            checkPoset(isBitSubset, debug, random(random, SCALE / 4, SCALE));
            ok = true;
        } finally {
            if (!ok) {
                System.out.println("Random seed: " + seed);
            }
        }
    }

    private void printValidate(PartiallyOrderedSet<String> poset) {
        if (debug) {
            dump(poset);
        }
        assertTrue(poset.isValid(debug));
    }

    public void checkPoset(
        PartiallyOrderedSet.Ordering<Integer> ordering,
        boolean debug,
        Iterable<Integer> generator)
    {
        final PartiallyOrderedSet<Integer> poset =
            new PartiallyOrderedSet<Integer>(ordering);
        int n = 0;
        for (int i : generator) {
            if (debug) {
                System.out.println("add " + i);
            }
            poset.add(i);
            if (debug) {
                dump(poset);
            }
            assertEquals(++n, poset.size());
            if (i < 100) {
                if (!poset.isValid(false)) {
                    dump(poset);
                }
                assertTrue(poset.isValid(true));
            }
        }
        assertTrue(poset.isValid(true));

        final StringBuilder buf = new StringBuilder();
        poset.out(buf);
        assertTrue(buf.length() > 0);
    }

    private <E> void dump(PartiallyOrderedSet<E> poset) {
        final StringBuilder buf = new StringBuilder();
        poset.out(buf);
        System.out.println(buf);
    }

    private static Collection<Integer> range(
        final int start, final int end)
    {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                return start + index;
            }

            @Override
            public int size() {
                return end - start;
            }
        };
    }

    private static Iterable<Integer> random(
        Random random, final int size, final int max)
    {
        final Set<Integer> set = new LinkedHashSet<Integer>();
        while (set.size() < size) {
            set.add(random.nextInt(max) + 1);
        }
        return set;
    }
}

// End PartiallyOrderedSetTest.java