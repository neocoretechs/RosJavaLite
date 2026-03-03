package org.ros.concurrent;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A deque that removes head or tail elements when the number of elements
 * exceeds the limit and blocks on {@link #takeFirst()} and {@link #takeLast()} when
 * there are no elements available.
 * 
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2017, 2021
 */
public class CircularBlockingDeque<T> implements BlockingQueue<T>, Iterable<T>, List<T> {
	private final T[] deque;
	private final Object mutex;
	/**
	 * The maximum number of entries in the queue.
	 */
	private final int limit;
	/**
	 * Points to the next entry that will be returned by {@link #takeFirst()} unless
	 * {@link #isEmpty()}.
	 */
	private int start;
	/**
	 * The number of entries currently in the queue.
	 */
	private int length;
	/**
	 * @param capacity
	 *          the maximum number of elements allowed in the queue
	 */
	@SuppressWarnings("unchecked")
	public CircularBlockingDeque(int capacity) {
		deque = (T[]) new Object[capacity];
		mutex = new Object();
		limit = capacity;
		start = 0;
		length = 0;
	}
	/**
	 * Sets start and length to 0
	 */
	@Override
	public void clear() {
		start = 0;
		length = 0;
	}
	/**
	 * Adds the specified entry to the tail of the queue, overwriting older
	 * entries if necessary.
	 * 
	 * @param entry
	 *          the entry to add
	 * @return {@code true} if entry overwrote
	 */
	@Override
	public void addLast(T entry) {
		//boolean overwrite = false;
		synchronized (mutex) {
			deque[(start + length) % limit] = entry;
			if (length == limit) {
				start = (start + 1) % limit;
				//overwrite = true;
			} else {
				length++;
			}
			mutex.notify();
		}
		//return overwrite;
	}

	public boolean addLast(List<T> lentry) {
		boolean overwrite = false;
		synchronized (mutex) {
			for(T entry: lentry) {	
				deque[(start + length) % limit] = entry;
				if (length == limit) {
					start = (start + 1) % limit;
					overwrite = true;
				} else {
					length++;
				}
			}
			mutex.notify();
		}
		return overwrite;
	}

	/**
	 * Adds the specified entry to the tail of the queue, 
	 * blocking at max entries if necessary. Use takeFirstNotify.
	 * 
	 * @param entry
	 *          the entry to add
	 * @return {@code true}
	 * @throws InterruptedException 
	 */
	public boolean addLastWait(T entry) throws InterruptedException {
		synchronized (mutex) {
			if (length == limit) {
				mutex.wait();
				deque[(start + length) % limit] = entry;
				start = (start + 1) % limit;
			} else {
				deque[(start + length) % limit] = entry;
				length++;
			}
			mutex.notify();
		}
		return true;
	}

	/**
	 * Adds the specified entry to the head of the queue, overwriting older
	 * entries if necessary.
	 * 
	 * @param entry
	 *          the entry to add
	 * @return {@code true} if entry overwrote
	 */
	@Override
	public void addFirst(T entry) {
		//boolean overwrite = false;
		synchronized (mutex) {
			if (start - 1 < 0) {
				start = limit - 1;
				//overwrite = true;
			} else {
				start--;
			}
			deque[start] = entry;
			if (length < limit) {
				length++;
			}
			mutex.notify();
		}
		//return overwrite;
	}

	/**
	 * Retrieves the head of the queue, blocking if necessary until an entry is
	 * available.
	 * 
	 * @return the head of the queue
	 * @throws InterruptedException
	 */
	public T takeFirst() throws InterruptedException {
		T entry;
		synchronized (mutex) {
			while (true) {
				if (length > 0) {
					entry = deque[start];
					start = (start + 1) % limit;
					length--;
					break;
				}
				mutex.wait();
			}
		}
		return entry;
	}

	/**
	 * Retrieves the head of the queue, blocking if necessary until an entry is
	 * available. Notifies addLastWait that a take has occurred.
	 * 
	 * @return the head of the queue
	 * @throws InterruptedException
	 */
	public T takeFirstNotify() throws InterruptedException {
		T entry;
		synchronized (mutex) {
			while (true) {
				if (length > 0) {
					entry = deque[start];
					start = (start + 1) % limit;
					length--;
					mutex.notify();
					break;
				}
				mutex.wait();
			}
		}
		return entry;
	}

	/**
	 * Retrieves, but does not remove, the head of this queue, returning
	 * {@code null} if this queue is empty.
	 * 
	 * @return the head of this queue, or {@code null} if this queue is empty
	 */
	public T peekFirst() {
		synchronized (mutex) {
			if (length > 0) {
				return deque[start];
			}
			return null;
		}
	}

	/**
	 * Retrieves the tail of the queue, blocking if necessary until an entry is
	 * available.
	 * 
	 * @return the tail of the queue
	 * @throws InterruptedException
	 */
	public T takeLast() throws InterruptedException {
		T entry;
		synchronized (mutex) {
			while (true) {
				if (length > 0) {
					entry = deque[(start + length - 1) % limit];
					length--;
					break;
				}
				mutex.wait();
			}
		}
		return entry;
	}

	/**
	 * Retrieves, but does not remove, the tail of this queue, returning
	 * {@code null} if this queue is empty.
	 * 
	 * @return the tail of this queue, or {@code null} if this queue is empty
	 */
	public T peekLast() {
		synchronized (mutex) {
			if (length > 0) {
				return deque[(start + length - 1) % limit];
			}
			return null;
		}
	}
	@Override
	public boolean isEmpty() {
		return length == 0;
	}
	/**
	 * The number of elements currently in the queue.
	 * @return Number of elements currently in the queue
	 */
	public int length() {
		return length;
	}

	public Object getMutex() {return mutex;}

	/**
	 * Returns an iterator over the queue.
	 * <p>
	 * Note that this is not thread-safe and that {@link Iterator#remove()} is
	 * unsupported.
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int offset = 0;

			@Override
			public boolean hasNext() {
				return offset < length;
			}

			@Override
			public T next() {
				if (offset == length) {
					throw new NoSuchElementException();
				}
				T entry = deque[(start + offset) % limit];
				offset++;
				return entry;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int size() {
		return deque.length;
	}

	@Override
	public boolean contains(Object o) {
		for(Object o2 : deque)
			if(o.equals((o2))) return true;
		return false;
	}

	@Override
	public Object[] toArray() {
		return deque;
	}

	@Override
	public Object[] toArray(Object[] a) {
		Object n = Array.newInstance(a.getClass(), a.length);
		Iterator<T> it = iterator();
		for(int i = 0; i < a.length; i++) {
			if(!it.hasNext())
				break;
			((Object[])n)[i] = it.next();
		}
		return (Object[]) n;	
	}

	@Override
	public boolean add(Object e) {
		if (length == limit) throw new IllegalStateException();
		addLast((T) e);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		int i = indexOf(o);
		if( i == -1 )
			return false;
		remove(i);
		return true;
	}

	@Override
	public boolean containsAll(Collection c) {
		boolean allContains = true;
		for(Object o : c) {
			if(!contains(o))
				allContains = false;
		}
		return allContains;
	}

	@Override
	public boolean addAll(Collection c) {
		if(c.isEmpty())
			return false;
		for(Object o: c) {
			add(o);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection c) {
		throw new RuntimeException(this.getClass().getName()+".addAll unimplemented");
	}

	@Override
	public boolean removeAll(Collection c) {
		boolean hasRemoved = false;
		for(Object o : c) {
			if(remove(o))
				hasRemoved = true;
		}
		return hasRemoved;
	}

	@Override
	public boolean retainAll(Collection c) {
		throw new RuntimeException(this.getClass().getName()+".retainAll unimplemented");
	}

	@Override
	public Object set(int index, Object element) {
		if (index < 0 || index >= length)
			throw new IndexOutOfBoundsException(
					"index=" + index + " length=" + length);
		Object o = deque[(start + index) % limit];
		deque[(start + index) % limit] = (T) element;
		return o;
	}

	@Override
	public void add(int index, Object element) {
		if (index < 0 || index > length)
			throw new IndexOutOfBoundsException("index=" + index + " length=" + length);
		synchronized (mutex) {
			int l = ((start + length) % limit); 
			int i = ((start + index) % limit);
			while(l != i) {
				int j = ((l-1) % limit);
				deque[l] = deque[j];
				l = j;
			}
			deque[(start + index) % limit] = (T)element;
			if (length == limit) {
				start = (start + 1) % limit;
			} else {
				length++;
			}
			mutex.notify();
		}
	}

	@Override
	public T remove(int index) {
		synchronized (mutex) {
			int l = ((start + length) % limit); 
			int i = ((start + index) % limit);
			Object elem = deque[i];
			while(l != i) {
				int j = ((i+1) % limit);
				deque[i] = deque[j];
				i = j;
			}
			if (index == limit) {
				start = (start - 1) % limit;
			} else {
				length--;
			}
			mutex.notify();
			return (T)elem;
		}
	}

	@Override
	public int indexOf(Object o) {
		int j = 0;
		int index = -1;
		Iterator<?> it = iterator();
		while(it.hasNext()) {
			if(it.next().equals(o)) {
				index = j;
				break;
			}
			++j;
		}
		return index;
	}

	@Override
	public int lastIndexOf(Object o) {
		int j = 0;
		int index = -1;
		Iterator<?> it = iterator();
		while(it.hasNext()) {
			if(it.next().equals(o)) {
				index = j;
			}
			++j;
		}
		return index;
	}

	@Override
	public ListIterator listIterator() {
		throw new RuntimeException(this.getClass().getName()+".listIterator unimplemented");
	}

	@Override
	public ListIterator listIterator(int index) {
		throw new RuntimeException(this.getClass().getName()+".listIterator unimplemented");
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		throw new RuntimeException(this.getClass().getName()+".subList unimplemented");
	}

	@Override
	public T get(int index) {
		return deque[(start + index) % limit];
	}

	@Override
	public T remove() {
		return remove(0);
	}

	@Override
	public T poll() {
		try {
			return takeFirst();
		} catch (InterruptedException e) {
			return null;
		}
	}

	@Override
	public T element() {
		T elem = peekFirst();
		if(elem == null)
			throw new NoSuchElementException();
		return elem;
	}

	@Override
	public T peek() {
		return peekFirst();
	}

	@Override
	public boolean offer(T e) {
		if(length == limit)
			return false;
		addLast(e);
		return true;
	}

	@Override
	public void put(T e) throws InterruptedException {
		while(length == limit) {
			Thread.sleep(1);
		}
		synchronized(mutex) {
			addLast(e);
		}

	}

	@Override
	public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
		long waited = 0;
		if(e == null)
			throw new NullPointerException();
		while(length == limit) {
			Thread.sleep(1);
			if(waited++ == timeout)
				return false;
		}
		addLast(e);
		return true;
	}

	@Override
	public T take() throws InterruptedException {
		return takeFirst();
	}

	@Override
	public T poll(long timeout, TimeUnit unit) throws InterruptedException {
		long waited = 0;
		while(length == 0) {
			Thread.sleep(1);
			if(waited++ == timeout)
				return null;
		}
		return takeFirst();
	}

	@Override
	public int remainingCapacity() {
		return length-limit;
	}

	@Override
	public int drainTo(Collection<? super T> c) {
		int len = 0;
		if(c == null)
			throw new NullPointerException();
		synchronized(mutex) {
			len = length;
			while(length > 0)
				c.add(remove());
		}
		return len;
	}

	@Override
	public int drainTo(Collection<? super T> c, int maxElements) {
		int len = 0;
		if(c == null)
			throw new NullPointerException();
		synchronized(mutex) {
			while(length > 0 && len < maxElements) {
				c.add(remove());
				++len;
			}
		}
		return len;
	}

}
