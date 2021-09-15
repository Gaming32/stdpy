package io.github.gaming32.stdpy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class BlockDequeTest {
    public BlockDeque<Integer> initTestDeque() {
        return new BlockDeque<>(Arrays.asList(1, 2, 3, 4, 5, 6));
    }

    @Test
    public void testAdd() {
        BlockDeque<Integer> deque = new BlockDeque<Integer>() {{
            add(1);
            add(2);
            add(3);
            add(4);
            add(5);
            add(6);
        }};
        assertEquals(deque.toString(), "[1, 2, 3, 4, 5, 6]");
    }

    @Test
    public void testAddAll() {
        BlockDeque<Integer> deque = new BlockDeque<Integer>() {{
            addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        }};
        assertEquals(deque.toString(), "[1, 2, 3, 4, 5, 6]");
    }

    @Test
    public void testAddAllFirst() {
        BlockDeque<Integer> deque = new BlockDeque<Integer>() {{
            addAllFirst(Arrays.asList(1, 2, 3, 4, 5, 6));
        }};
        assertEquals(deque.toString(), "[6, 5, 4, 3, 2, 1]");
    }

    @Test
    public void testAddWithMax() {
        BlockDeque<Integer> deque = new BlockDeque<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6), 4);
        assertEquals(deque.toString(), "[3, 4, 5, 6]");
    }

    @Test
    public void testGet() {
        BlockDeque<Integer> deque = initTestDeque();
        assertEquals(deque.get(3), Integer.valueOf(4));
    }

    @Test
    public void testRotate() {
        BlockDeque<Integer> deque = initTestDeque();
        deque.rotate(2);
        assertEquals(deque.toString(), "[5, 6, 1, 2, 3, 4]");
        deque.rotate(-4);
        assertEquals(deque.toString(), "[3, 4, 5, 6, 1, 2]");
    }

    @Test
    public void testReverse() {
        BlockDeque<Integer> deque = initTestDeque();
        deque.reverse();
        assertEquals(deque.toString(), "[6, 5, 4, 3, 2, 1]");
    }

    @Test
    public void testRemoveIndex() {
        BlockDeque<Integer> deque = initTestDeque();
        deque.remove(2);
        assertEquals(deque.toString(), "[1, 2, 4, 5, 6]");
    }

    @Test
    public void testRemoveValue() {
        BlockDeque<Integer> deque = initTestDeque();
        deque.remove(Integer.valueOf(4));
        assertEquals(deque.toString(), "[1, 2, 3, 5, 6]");
    }

    @Test
    public void testIter() {
        BlockDeque<Integer> deque = initTestDeque();
        List<Integer> list = new ArrayList<>();
        for (Integer e : deque) {
            list.add(e);
        }
        assertEquals(list.toString(), "[1, 2, 3, 4, 5, 6]");
        assertEquals(deque.toString(), "[1, 2, 3, 4, 5, 6]"); // Test that it didn't get corrupted
    }

    @Test
    public void testReverseIter() {
        BlockDeque<Integer> deque = initTestDeque();
        List<Integer> list = new ArrayList<>();
        Iterator<Integer> revIter = deque.descendingIterator();
        while (revIter.hasNext()) {
            list.add(revIter.next());
        }
        assertEquals(list.toString(), "[6, 5, 4, 3, 2, 1]");
        assertEquals(deque.toString(), "[1, 2, 3, 4, 5, 6]"); // Test that it didn't get corrupted
    }

    @Test
    public void testBlockData() {
        BlockDeque<Integer> deque = initTestDeque();
        deque.push(35);
        deque.push(40);
        deque.push(45);
        deque.pop();
        assertArrayEquals(deque.leftblock.data,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, 40 });
        deque.pop();
        deque.removeLast();
        assertArrayEquals(deque.leftblock.data,
                new Integer[] { 35, 1, 2, 3, 4, 5, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null });
    }
}
