package io.github.gaming32.stdpy;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * collections module implementation of a deque() datatype<br>
 * Written and maintained by Raymond D. Hettinger &lt;python@rcn.com&gt;<br>
 * Java version by Josiah (Gaming32) Glosson &lt;gaming32i64@gmail.com&gt;
 */
@SuppressWarnings("unchecked")
public class BlockDeque<E> extends AbstractList<E> implements Deque<E> {
    /**
     * The block length may be set to any number over 1.  Larger numbers
     * reduce the number of calls to the memory allocator, give faster
     * indexing and rotation, and reduce the link to data overhead ratio.
     * Making the block length a power of two speeds-up the modulo
     * and division calculations in BlockDeque.get() and BlockDeque.set().
     */
    protected static final int BLOCKLEN = 64;
    /**
     * The center index of each block
     * @see #BLOCKLEN
    */
    protected static final int CENTER = ((BLOCKLEN - 1) / 2);
    /**
     * The maximum number of blocks for freelisting
     * @see #freeblocks
     */
    protected static final int MAXFREEBLOCKS = 16;

    /**
     * <p>Data for deque objects is stored in a doubly-linked list of fixed
     * length blocks.  This assures that appends or pops never move any
     * other data elements besides the one being appended or popped.</p>
     *
     * <p>Another advantage is that it completely avoids use of <code>realloc()</code>,
     * resulting in more predictable performance.</p>
     *
     * <p>Textbook implementations of doubly-linked lists store one datum
     * per link, but that gives them a 200% memory overhead (a prev and
     * next link for each datum) and it costs one <code>malloc()</code> call per data
     * element.  By using fixed-length blocks, the link to data ratio is
     * significantly improved and there are proportionally fewer calls
     * to <code>malloc()</code> and <code>free()</code>.  The data blocks of consecutive pointers
     * also improve cache locality.</p>
     *
     * <p>The list of blocks is never empty, so <code>d.leftblock</code> and <code>d.rightblock</code>
     * are never equal to <code>null</code>.  The list is not circular.</p>
     *
     * <p>A deque <code>d</code>'s first element is at <code>d.leftblock[leftindex]</code>
     * and its last element is at <code>d.rightblock[rightindex]</code>.</p>
     *
     * <p>The indices, <code>d.leftindex</code> and <code>d.rightindex</code> are always in the range:<br>
     *     <code>0 &lt;= index &lt; BLOCKLEN</code></p>
     *
     * <p>And their exact relationship is:<br>
     *     <code>(d.leftindex + d.len - 1) % BLOCKLEN == d.rightindex</code></p>
     *
     * <p>Whenever <code>d.leftblock == d.rightblock</code>, then:<br>
     *     <code>d.leftindex + d.len - 1 == d.rightindex</code></p>
     *
     * <p>However, when <code>d.leftblock != d.rightblock</code>, the <code>d.leftindex</code> and
     * <code>d.rightindex</code> become indices into distinct blocks and either may
     * be larger than the other.</p>
     *
     * <p>Empty deques have:<br>
     *     <code>d.len == 0</code><br>
     *     <code>d.leftblock == d.rightblock</code><br>
     *     <code>d.leftindex == CENTER + 1</code><br>
     *     <code>d.rightindex == CENTER</code></p>
     */
    protected static class Block {
        protected Block leftlink;
        protected Object[] data = new Object[BLOCKLEN];
        protected Block rightlink;
    }

    protected Block leftblock;
    protected Block rightblock;
    /**
     * 0 &lt;= leftindex &lt; BLOCKLEN
     */
    protected int leftindex;
    /**
     * 0 &lt;= rightindex &lt; BLOCKLEN
     */
    protected int rightindex;
    /**
     * incremented whenever the indices move 
     */
    protected long state;
    /**
     * maxlen is Integer.MAX_VALUE for unbounded deques
     */
    protected int maxlen;
    /**
     * The number of free blocks currently stored.
     * @see #freeblocks
     */
    protected int numfreeblocks;
    /**
     * A simple freelisting scheme is used to minimize calls to the memory
     * allocator.  It accommodates common use cases where new blocks are being
     * added at about the same rate as old blocks are being freed.
    */
    protected Block[] freeblocks = new Block[MAXFREEBLOCKS];

    protected int size;

    protected Block newblock() {
        Block b;
        if (this.numfreeblocks > 0) {
            this.numfreeblocks--;
            return this.freeblocks[this.numfreeblocks];
        }
        b = new Block();
        return b;
    }

    protected void freeblock(Block b) {
        if (this.numfreeblocks < MAXFREEBLOCKS) {
            this.freeblocks[this.numfreeblocks] = b;
            this.numfreeblocks++;
        }
    }

    public BlockDeque() {
        Block b;

        b = newblock();
        
        assert(BLOCKLEN >= 2);
        this.size = 0;
        this.leftblock = b;
        this.rightblock = b;
        this.leftindex = CENTER + 1;
        this.rightindex = CENTER;
        this.state = 0;
        this.maxlen = Integer.MAX_VALUE;
        this.numfreeblocks = 0;
    }

    @Override
    public E removeLast() {
        E item;
        Block prevblock;

        if (this.size == 0) {
            throw new NoSuchElementException("pop from an empty deque");
        }
        item = (E)this.rightblock.data[this.rightindex];
        this.rightblock.data[this.rightindex] = null;
        this.rightindex--;
        this.size--;
        this.state++;

        if (this.rightindex < 0) {
            if (this.size != 0) {
                prevblock = this.rightblock.leftlink;
                assert(this.leftblock != this.rightblock);
                freeblock(this.rightblock);
                this.rightblock = prevblock;
                this.rightindex = BLOCKLEN - 1;
            } else {
                assert(this.leftblock == this.rightblock);
                assert(this.leftindex == this.rightindex + 1);
                /* re-center instead of freeing a block */
                this.leftindex = CENTER + 1;
                this.rightindex = CENTER;
            }
        }
        return item;
    }

    @Override
    public E removeFirst() {
        E item;
        Block prevblock;

        if (this.size == 0) {
            throw new NoSuchElementException("pop from an empty deque");
        }
        assert(this.leftblock != null);
        item = (E)this.leftblock.data[this.leftindex];
        this.leftblock.data[this.leftindex] = null;
        this.leftindex++;
        this.size--;
        this.state++;

        if (this.leftindex == BLOCKLEN) {
            if (this.size != 0) {
                assert(this.leftblock != this.rightblock);
                prevblock = this.leftblock.rightlink;
                freeblock(prevblock);
                this.leftblock = prevblock;
                this.leftindex = 0;
            } else {
                assert(this.leftblock == this.rightblock);
                assert(this.leftindex == this.rightindex + 1);
                /* re-center instead of freeing a block */
                this.leftindex = CENTER + 1;
                this.rightindex = CENTER;
            }
        }
        return item;
    }

    /**
     * <p>The deque's size limit is <code>d.maxlen</code>.  The limit can be zero or positive.
     * If there is no limit, then <code>d.maxlen == Integer.MAX_VALUE</code>.</p>
     *
     * <p>After an item is added to a deque, we check to see if the size has
     * grown past the limit. If it has, we get the size back down to the limit
     * by popping an item off of the opposite end.  The methods that can
     * trigger this are <code>append()</code>, <code>appendleft()</code>, <code>extend()</code>, and <code>extendleft()</code>.</p>
     *
     * <p>The method to check whether a deque needs to be trimmed uses a single
     * unsigned test that returns <code>true</code> whenever <code>0 &lt;= maxlen &lt; deque.size</code>.</p>
     */
    protected boolean needsTrim(int maxlen) {
        return maxlen < this.size;
    }

    private void appendInternal(E item, int maxlen) {
        if (this.rightindex == BLOCKLEN - 1) {
            Block b = newblock();
            b.leftlink = this.rightblock;
            this.rightblock.rightlink = b;
            this.rightblock = b;
            this.rightindex = -1;
        }
        this.size++;
        this.rightindex++;
        this.rightblock.data[this.rightindex] = item;
        if (needsTrim(maxlen)) {
            removeFirst();
        } else {
            this.state++;
        }
    }

    @Override
    public void addLast(E e) {
        appendInternal(e, this.maxlen);
    }

    private void appendleftInternal(E item, int maxlen) {
        if (this.leftindex == 0) {
            Block b = newblock();
            b.rightlink = this.leftblock;
            this.leftblock.leftlink = b;
            this.leftblock = b;
            this.leftindex  = BLOCKLEN;
        }
        this.size++;
        this.leftindex--;
        this.leftblock.data[this.leftindex] = item;
        if (needsTrim(maxlen)) {
            removeLast();
        } else {
            this.state++;
        }
    }

    @Override
    public void addFirst(E e) {
        appendleftInternal(e, this.maxlen);
    }

    /**
     * Run an iterator to exhaustion.  Shortcut for
     * the extend/extendleft methods when maxlen == 0.
     * @return <code>false</code>
     */
    protected boolean consumeIterator(Iterator<? extends E> it) {
        while (it.hasNext()) it.next();
        return false;
    }

    public boolean addAllLast(Collection<? extends E> c) {
        Iterator<? extends E> it;
        E item;
        int maxlen = this.maxlen;

        /* Handle case where this == c */
        if (this == c) {
            return addAllLast(new ArrayList<>(this));
        }

        it = c.iterator();
        if (it == null) {
            return false;
        }

        if (maxlen == 0) {
            return consumeIterator(it);
        }

        /* Space saving heuristic.  Start filling from the right */
        if (this.size == 0) {
            assert(this.leftblock == this.rightblock);
            assert(this.leftindex == this.rightindex + 1);
            this.leftindex = 1;
            this.rightindex = 0;
        }

        boolean changed = it.hasNext();
        if (changed) {
            do {
                item = it.next();
                appendInternal(item, maxlen);
            } while (it.hasNext());
        }
        return changed;
    }

    public boolean addAllFirst(Collection<? extends E> c) {
        Iterator<? extends E> it;
        E item;
        int maxlen = this.maxlen;

        /* Handle case where this == c */
        if (this == c) {
            return addAllFirst(new ArrayList<>(this));
        }

        it = c.iterator();
        if (it == null) {
            return false;
        }

        if (maxlen == 0) {
            return consumeIterator(it);
        }

        /* Space saving heuristic.  Start filling from the left */
        if (this.size == 0) {
            assert(this.leftblock == this.rightblock);
            assert(this.leftindex == this.rightindex + 1);
            this.leftindex = BLOCKLEN - 1;
            this.rightindex = BLOCKLEN - 2;
        }

        boolean changed = it.hasNext();
        if (changed) {
            do {
                item = it.next();
                appendleftInternal(item, maxlen);
            } while (it.hasNext());
        }
        return changed;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAllLast(c);
    }

    @Override
    public void clear() {
        Block b, prevblock, leftblock;
        int leftindex, n, m;
        Object[] itemprtData;
        int itemptr, limit;

        if (this.size == 0) {
            return;
        }

        /* During the process of clearing a deque, decrefs can cause the
           deque to mutate.  To avoid fatal confusion, we have to make the
           deque empty before clearing the blocks and never refer to
           anything via deque->ref while clearing.  (This is the same
           technique used for clearing lists, sets, and dicts.)
           Making the deque empty requires allocating a new empty block.  In
           the unlikely event that memory is full, we fall back to an
           alternate method that doesn't require a new block.  Repeating
           pops in a while-loop is slower, possibly re-entrant (and a clever
           adversary could cause it to never terminate).
        */

        try {
            b = newblock();
        } catch (OutOfMemoryError e) {
            while (this.size > 0) {
                this.pop();
            }
            return;
        }

        /* Remember the old size, leftblock, and leftindex */
        n = this.size;
        leftblock = this.leftblock;
        leftindex = this.leftindex;

        /* Set the deque to be empty using the newly allocated block */
        this.size = 0;
        this.leftblock = b;
        this.rightblock = b;
        this.leftindex = CENTER + 1;
        this.rightindex = CENTER;
        this.state++;

        /* Now the old size, leftblock, and leftindex are disconnected from
           the empty deque and we can use them to decref the pointers.
        */
        m = (BLOCKLEN - leftindex > n) ? n : BLOCKLEN - leftindex;
        itemprtData = leftblock.data;
        itemptr = leftindex;
        limit = itemptr + m;
        n -= m;
        while (true) {
            if (itemptr == limit) {
                if (n == 0) {
                    break;
                }
                prevblock = leftblock;
                leftblock = leftblock.rightlink;
                m = (n > BLOCKLEN) ? BLOCKLEN : n;
                itemprtData = leftblock.data;
                itemptr = 0;
                limit = itemptr + m;
                n -= m;
                freeblock(prevblock);
            }
            itemprtData[itemptr++] = null;
        }
        freeblock(leftblock);
    }

    public void rotate(int n) {
        Block b = null;
        Block leftblock = this.leftblock;
        Block rightblock = this.rightblock;
        int leftindex = this.leftindex;
        int rightindex = this.rightindex;
        int len = this.size, halflen = len >> 1;

        if (len <= 1) {
            return;
        }
        if (n > halflen || n < -halflen) {
            n %= len;
            if (n > halflen) {
                n -= len;
            } else if (n < -halflen) {
                n += len;
            }
        }
        assert(len > 1);
        assert(-halflen <= n && n <= halflen);

        this.state++;
        while (n > 0) {
            if (leftindex == 0) {
                if (b == null) {
                    try {
                        b = newblock();
                    } catch (OutOfMemoryError e) {
                        this.leftblock = leftblock;
                        this.rightblock = rightblock;
                        this.leftindex = leftindex;
                        this.rightindex = rightindex;
                        throw e;
                    }
                }
                b.rightlink = leftblock;
                leftblock.leftlink = b;
                leftblock = b;
                leftindex = BLOCKLEN;
                b = null;
            }
            assert(leftindex > 0);
            {
                Object[] srcData, destData;
                int src, dest;
                int m = n;

                if (m > rightindex + 1) {
                    m = rightindex + 1;
                }
                if (m > leftindex) {
                    m = leftindex;
                }
                assert(m > 0 && m <= len);
                rightindex -= m;
                leftindex -= m;
                srcData = rightblock.data;
                src = rightindex + 1;
                destData = leftblock.data;
                dest = leftindex;
                n -= m;
                System.arraycopy(srcData, src, destData, dest, m);
                Arrays.fill(srcData, src, src + m, null);
            }
            if (rightindex < 0) {
                assert(leftblock != rightblock);
                assert(b == null);
                b = rightblock;
                rightblock = rightblock.leftlink;
                rightindex = BLOCKLEN - 1;
            }
        }
        while (n < 0) {
            if (rightindex == BLOCKLEN - 1) {
                if (b == null) {
                    try {
                        b = newblock();
                    } catch (OutOfMemoryError e) {
                        this.leftblock = leftblock;
                        this.rightblock = rightblock;
                        this.leftindex = leftindex;
                        this.rightindex = rightindex;
                        throw e;
                    }
                }
                b.leftlink = rightblock;
                rightblock.rightlink = b;
                rightblock = b;
                rightindex = -1;
                b = null;
            }
            assert(rightindex < BLOCKLEN - 1);
            {
                Object[] srcData, destData;
                int src, dest;
                int m = -n;

                if (m > BLOCKLEN - leftindex) {
                    m = BLOCKLEN - leftindex;
                }
                if (m > BLOCKLEN - 1 - rightindex) {
                    m = BLOCKLEN - 1 - rightindex;
                }
                assert(m > 0 && m <= len);
                srcData = leftblock.data;
                src = leftindex;
                destData = rightblock.data;
                dest = rightindex + 1;
                leftindex += m;
                rightindex += m;
                n += m;
                System.arraycopy(srcData, src, destData, dest, m);
                Arrays.fill(srcData, src, src + m, null);
            }
            if (leftindex == BLOCKLEN) {
                assert(leftblock != rightblock);
                assert(b == null);
                b = leftblock;
                leftblock = leftblock.rightlink;
                leftindex = 0;
            }
        }
        if (b != null) {
            freeblock(b);
        }
        this.leftblock = leftblock;
        this.rightblock = rightblock;
        this.leftindex = leftindex;
        this.rightindex = rightindex;
    }

    public void rotate() {
        rotate(1);
    }

    public void reverse() {
        Block leftblock = this.leftblock;
        Block rightblock = this.rightblock;
        int leftindex = this.leftindex;
        int rightindex = this.rightindex;
        int n = this.size >> 1;
        E tmp;

        while (--n >= 0) {
            /* Validate that pointers haven't met in the middle */
            assert(leftblock != rightblock || leftindex < rightindex);

            /* Swap */
            tmp = (E)leftblock.data[leftindex];
            leftblock.data[leftindex] = rightblock.data[rightindex];
            rightblock.data[rightindex] = tmp;

            /* Advance left block/index pair */
            leftindex++;
            if (leftindex == BLOCKLEN) {
                leftblock = leftblock.rightlink;
                leftindex = 0;
            }

            /* Step backwards with the right block/index pair */
            rightindex--;
            if (rightindex < 0) {
                rightblock = rightblock.leftlink;
                rightindex = BLOCKLEN - 1;
            }
        }
    }

    public int count(Object o) {
        Block b = this.leftblock;
        int index = this.leftindex;
        int n = this.size;
        int count = 0;
        long startState = this.state;
        E item;
        boolean cmp;

        while (--n >= 0) {
            item = (E)b.data[index];
            cmp = item.equals(o);
            count += cmp ? 1 : 0;

            if (startState != this.state) {
                throw new IllegalStateException("deque mutated during iteration");
            }

            /* Advance left block/index pair */
            index++;
            if (index == BLOCKLEN) {
                b = b.rightlink;
                index = 0;
            }
        }
        return count;
    }

    @Override
    public boolean contains(Object o) {
        Block b = this.leftblock;
        int index = this.leftindex;
        int n = this.size;
        long startState = this.state;
        E item;
        boolean cmp;

        while (--n >= 0) {
            item = (E)b.data[index];
            cmp = item.equals(o);
            if (cmp) {
                return cmp;
            }
            if (startState != this.state) {
                throw new IllegalStateException("deque mutated during iteration");
            }
            index++;
            if (index == BLOCKLEN) {
                b = b.rightlink;
                index = 0;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return this.size;
    }

    public int indexOf(Object o, int start, int stop) {
        int i, n;
        E item;
        Block b = this.leftblock;
        int index = this.leftindex;
        long startState = this.state;
        boolean cmp;

        if (stop > this.size) {
            stop = this.size;
        }
        if (start > stop) {
            start = stop;
        }
        assert(0 <= start && start <= stop && stop <= this.size);

        for (i = 0; i < start - BLOCKLEN; i += BLOCKLEN) {
            b = b.rightlink;
        }
        for (; i < start; i++) {
            index++;
            if (index == BLOCKLEN) {
                b = b.rightlink;
                index = 0;
            }
        }

        n = stop - i;
        while (--n >= 0) {
            item = (E)b.data[index];
            cmp = item.equals(o);
            if (cmp) {
                return stop - n - 1;
            }
            if (startState != this.state) {
                throw new IllegalStateException("deque mutated during iteration");
            }
            index++;
            if (index == BLOCKLEN) {
                b = b.rightlink;
                index = 0;
            }
        }
        return -1;
    }

    public int indexOf(Object o, int start) {
        return indexOf(o, start, this.size);
    }

    @Override
    public int indexOf(Object o) {
        return indexOf(o, 0, this.size);
    }

    @Override
    public void add(int index, E element) {
        int n = this.size;

        if (this.maxlen == this.size) {
            throw new IllegalStateException("deque already at its maximum size");
        }
        if (index == n) {
            this.addLast(element);
            return;
        }
        if (index == 0) {
            this.addFirst(element);
            return;
        }
        this.rotate(-index);
        this.addFirst(element);
        this.rotate(index);
    }

    protected boolean validIndex(int i, int limit) {
        return 0 <= i && i < limit;
    }

    protected IndexOutOfBoundsException indexOutOfBounds(int i) {
        return new IndexOutOfBoundsException("Index " + i + " out of bounds for deque of length " + this.size);
    }

    @Override
    public E get(int i) {
        Block b;
        int n, index = i;

        if (!validIndex(i, this.size)) {
            throw this.indexOutOfBounds(i);
        }

        if (i == 0) {
            i = this.leftindex;
            b = this.leftblock;
        } else if (i == this.size - 1) {
            i = this.rightindex;
            b = this.rightblock;
        } else {
            i += this.leftindex;
            n = i / BLOCKLEN;
            i = i % BLOCKLEN;
            if (index < (this.size >> 1)) {
                b = this.leftblock;
                while (--n >= 0) {
                    b = b.rightlink;
                }
            } else {
                n = (this.leftindex + this.size - 1) / BLOCKLEN - n;
                b = this.rightblock;
                while (--n >= 0) {
                    b = b.leftlink;
                }
            }
        }
        return (E)b.data[i];
    }

    @Override
    public E remove(int index) {
        E item;

        if (!validIndex(index, this.size)) {
            throw this.indexOutOfBounds(index);
        }
        this.rotate(-index);
        item = this.removeFirst();
        this.rotate(index);
        return item;
    }

    @Override
    public boolean remove(Object o) {
        E item;
        Block b = this.leftblock;
        int i, n = this.size, index = this.leftindex;
        long startState = this.state;
        boolean cmp;

        for (i = 0; i < n; i++) {
            item = (E)b.data[index];
            cmp = item.equals(o);
            if (startState != this.state) {
                throw new IllegalStateException("deque mutated during iteration");
            }
            if (cmp) {
                break;
            }
            index++;
            if (index == BLOCKLEN) {
                b = b.rightlink;
                index = 0;
            }
        }
        if (i == n) {
            return false;
        }
        remove(i);
        return true;
    }

    @Override
    public E set(int index, E element) {
        E oldValue;
        Block b;
        int n, len = this.size, halflen = (len + 1) >> 1, index2 = index;

        if (!validIndex(index, len)) {
            throw this.indexOutOfBounds(index);
        }

        index += this.leftindex;
        n = index / BLOCKLEN;
        index = index % BLOCKLEN;
        if (index2 <= halflen) {
            b = this.leftblock;
            while (--n >= 0) {
                b = b.rightlink;
            }
        } else {
            n = (this.leftindex + this.size - 1) / BLOCKLEN - n;
            b = this.rightblock;
            while (--n >= 0) {
                b = b.leftlink;
            }
        }
        oldValue = (E)b.data[index];
        b.data[index] = element;
        return oldValue;
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Block b;
        E item;
        int index;
        int indexlo = this.leftindex;
        int indexhigh;

        for (b = this.leftblock; b != this.rightblock; b = b.rightlink) {
            for (index = indexlo; index < BLOCKLEN; index++) {
                item = (E)b.data[index];
                action.accept(item);
            }
            indexlo = 0;
        }
        indexhigh = this.rightindex;
        for (index = indexlo; index <= indexhigh; index++) {
            item = (E)b.data[index];
            action.accept(item);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof BlockDeque<?>)) {
            return false;
        } else {
            BlockDeque<?> w = (BlockDeque<?>)o;
            Iterator<? extends E> it1 = null;
            Iterator<?> it2 = null;
            E x;
            Object y;
            int vs, ws;
            boolean b;

            /* Shortcuts */
            vs = this.size;
            ws = w.size;
            if (this == w) {
                return true;
            }
            if (vs != ws) {
                return false;
            }

            /* Search for the first index where items are different */
            it1 = this.iterator();
            it2 = w.iterator();
            for (;;) {
                if (!it1.hasNext() || !it2.hasNext()) {
                    break;
                }
                x = it1.next();
                y = it2.next();
                b = x.equals(y);
                if (!b) {
                    return false;
                }
            }
            /* We reached the end of one deque or both */
            return true;
        }
    }

    public BlockDeque(Collection<? extends E> c) {
        this();
        if (c != null) {
            this.addAll(c);
        }
    }

    public BlockDeque(Collection<? extends E> c, int maxlen) {
        this();
        this.maxlen = maxlen;
        if (c != null) {
            this.addAll(c);
        }
    }

    public int getMaxlen() {
        return this.maxlen == Integer.MAX_VALUE ? -1 : this.maxlen;
    }

    @Override
    public Iterator<E> iterator() {
        return new DequeIter(this);
    }

    protected class DequeIter implements Iterator<E> {
        Block b;
        int index;
        BlockDeque<E> deque;
        long state;     /* state when the iterator is created */
        int counter;    /* number of items remaining for iteration */

        private DequeIter() {
        }

        DequeIter(BlockDeque<E> deque) {
            this.b = deque.leftblock;
            this.index = deque.leftindex;
            this.deque = deque;
            this.state = deque.state;
            this.counter = deque.size;
        }

        @Override
        public boolean hasNext() {
            return this.counter > 0;
        }

        @Override
        public E next() {
            E item;

            if (this.deque.state != this.state) {
                this.counter = 0;
                throw new IllegalStateException("deque mutated during iteration");
            }
            if (this.counter == 0) {
                throw new NoSuchElementException();
            }
            assert(!(this.b == this.deque.rightblock && this.index > this.deque.rightindex));

            item = (E)this.b.data[this.index];
            this.index++;
            this.counter--;
            if (this.index == BLOCKLEN && this.counter > 0) {
                this.b = this.b.rightlink;
                this.index = 0;
            }
            return item;
        }
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DequeRevIter(this);
    }

    protected class DequeRevIter extends DequeIter {
        DequeRevIter(BlockDeque<E> deque) {
            this.b = deque.rightblock;
            this.index = deque.rightindex;
            this.deque = deque;
            this.state = deque.state;
            this.counter = deque.size;
        }

        @Override
        public E next() {
            E item;
            if (this.counter == 0) {
                throw new NoSuchElementException();
            }

            if (this.deque.state != this.state) {
                this.counter = 0;
                throw new IllegalStateException("deque mutated during iteration");
            }
            assert(!(this.b == this.deque.leftblock && this.index < this.deque.leftindex));

            item = (E)this.b.data[this.index];
            this.index--;
            this.counter--;
            if (this.index < 0 && this.counter > 0) {
                this.b = this.b.leftlink;
                this.index = BLOCKLEN - 1;
            }
            return item;
        }
    }

    // ****************************************************
    // * Remaining implementation of List<E> and Deque<E> *
    // ****************************************************

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E getLast() {
        if (this.size == 0) {
            throw new NoSuchElementException();
        }
        return (E)this.rightblock.data[this.rightindex];
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    @Override
    public boolean offer(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E getFirst() {
        if (this.size == 0) {
            throw new NoSuchElementException();
        }
        return (E)this.leftblock.data[this.leftindex];
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public E peekFirst() {
        return this.size == 0 ? null : (E)this.leftblock.data[this.leftindex];
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public E pollFirst() {
        return this.size == 0 ? null : removeFirst();
    }

    @Override
    public E peekLast() {
        return this.size == 0 ? null : (E)this.rightblock.data[this.rightindex];
    }

    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E pollLast() {
        return this.size == 0 ? null : removeLast();
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        E item;
        Block b = this.rightblock;
        int i, n = this.size, index = this.rightindex;
        long startState = this.state;
        boolean cmp;

        for (i = n - 1; i >= 0; i--) {
            item = (E)b.data[index];
            cmp = item.equals(o);
            if (startState != this.state) {
                throw new IllegalStateException("deque mutated during iteration");
            }
            if (cmp) {
                break;
            }
            index--;
            if (index == -1) {
                b = b.leftlink;
                index = BLOCKLEN - 1;
            }
        }
        if (i < 0) {
            return false;
        }
        remove(i);
        return true;
    }
}
