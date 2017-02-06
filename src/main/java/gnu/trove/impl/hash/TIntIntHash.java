///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001, Eric D. Friedman All Rights Reserved.
// Copyright (c) 2009, Rob Eden All Rights Reserved.
// Copyright (c) 2009, Jeff Randall All Rights Reserved.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////

package gnu.trove.impl.hash;

import gnu.trove.procedure.*;
import gnu.trove.impl.HashFunctions;

import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;


//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////


/**
 * An open addressed hashing implementation for int/int primitive entries.
 *
 * Created: Sun Nov  4 08:56:06 2001
 *
 * @author Eric D. Friedman
 * @author Rob Eden
 * @author Jeff Randall
 * @version $Id: _K__V_Hash.template,v 1.1.2.6 2009/11/07 03:36:44 robeden Exp $
 */
abstract public class TIntIntHash extends TPrimitiveHash {
	static final long serialVersionUID = 1L;

    /** the set of ints */
    public transient int[] _set;


    /**
     * key that represents null
     *
     * NOTE: should not be modified after the Hash is created, but is
     *       not final because of Externalization
     *
     */
    protected int no_entry_key;


    /**
     * value that represents null
     *
     * NOTE: should not be modified after the Hash is created, but is
     *       not final because of Externalization
     *
     */
    protected int no_entry_value;

    protected boolean consumeFreeSlot;

    /**
     * Creates a new <code>T#E#Hash</code> instance with the default
     * capacity and load factor.
     */
    public TIntIntHash() {
        super();
        no_entry_key = ( int ) 0;
        no_entry_value = ( int ) 0;
    }


    /**
     * Creates a new <code>T#E#Hash</code> instance whose capacity
     * is the next highest prime above <tt>initialCapacity + 1</tt>
     * unless that value is already prime.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public TIntIntHash( int initialCapacity ) {
        super( initialCapacity );
        no_entry_key = ( int ) 0;
        no_entry_value = ( int ) 0;
    }


    /**
     * Creates a new <code>TIntIntHash</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     */
    public TIntIntHash( int initialCapacity, float loadFactor ) {
        super(initialCapacity, loadFactor);
        no_entry_key = ( int ) 0;
        no_entry_value = ( int ) 0;
    }


    /**
     * Creates a new <code>TIntIntHash</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     * @param no_entry_value value that represents null
     */
    public TIntIntHash( int initialCapacity, float loadFactor,
        int no_entry_key, int no_entry_value ) {
        super(initialCapacity, loadFactor);
        this.no_entry_key = no_entry_key;
        this.no_entry_value = no_entry_value;
    }


    /**
     * Returns the value that is used to represent null as a key. The default
     * value is generally zero, but can be changed during construction
     * of the collection.
     *
     * @return the value that represents null
     */
    public int getNoEntryKey() {
        return no_entry_key;
    }


    /**
     * Returns the value that is used to represent null. The default
     * value is generally zero, but can be changed during construction
     * of the collection.
     *
     * @return the value that represents null
     */
    public int getNoEntryValue() {
        return no_entry_value;
    }


    /**
     * initializes the hashtable to a prime capacity which is at least
     * <tt>initialCapacity + 1</tt>.
     *
     * @param initialCapacity an <code>int</code> value
     * @return the actual capacity chosen
     */
    protected int setUp( int initialCapacity ) {
        int capacity;

        capacity = super.setUp( initialCapacity );
        _set = new int[capacity];
        return capacity;
    }


    /**
     * Searches the set for <tt>val</tt>
     *
     * @param val an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    public boolean contains( int val ) {
        return index(val) >= 0;
    }


    /**
     * Executes <tt>procedure</tt> for each key in the map.
     *
     * @param procedure a <code>TIntProcedure</code> value
     * @return false if the loop over the set terminated because
     * the procedure returned false for some value.
     */
    public boolean forEach( TIntProcedure procedure ) {
        byte[] states = _states;
        int[] set = _set;
        for ( int i = set.length; i-- > 0; ) {
            if ( states[i] == FULL && ! procedure.execute( set[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Releases the element currently stored at <tt>index</tt>.
     *
     * @param index an <code>int</code> value
     */
    protected void removeAt( int index ) {
        _set[index] = no_entry_key;
        super.removeAt( index );
    }


    /**
     * Locates the index of <tt>val</tt>.
     *
     * @param key an <code>int</code> value
     * @return the index of <tt>val</tt> or -1 if it isn't in the set.
     */
    protected int index( int key ) {
        int hash, index, length;

        final byte[] states = _states;
        final int[] set = _set;
        length = states.length;
        hash = HashFunctions.hash( key ) & 0x7fffffff;
        index = hash % length;
        byte state = states[index];

        if (state == FREE)
            return -1;

        if (state == FULL && set[index] == key)
            return index;

        return indexRehashed(key, index, hash, state);
    }

    int indexRehashed(int key, int index, int hash, byte state) {
        // see Knuth, p. 529
        int length = _set.length;
        int probe = 1 + (hash % (length - 2));
        final int loopIndex = index;

        do {
            index -= probe;
            if (index < 0) {
                index += length;
            }
            state = _states[index];
            //
            if (state == FREE)
                return -1;

            //
            if (key == _set[index] && state != REMOVED)
                return index;
        } while (index != loopIndex);

        return -1;
    }


    /**
     * Locates the index at which <tt>val</tt> can be inserted.  if
     * there is already a value equal()ing <tt>val</tt> in the set,
     * returns that value as a negative integer.
     *
     * @param key an <code>int</code> value
     * @return an <code>int</code> value
     */
         protected int insertKey( int val ) {
             int hash, index;

             hash = HashFunctions.hash(val) & 0x7fffffff;
             index = hash % _states.length;
             byte state = _states[index];

             consumeFreeSlot = false;

             if (state == FREE) {
                 consumeFreeSlot = true;
                 insertKeyAt(index, val);

                 return index;       // empty, all done
             }

             if (state == FULL && _set[index] == val) {
                 return -index - 1;   // already stored
             }

             // already FULL or REMOVED, must probe
             return insertKeyRehash(val, index, hash, state);
         }

         int insertKeyRehash(int val, int index, int hash, byte state) {
             // compute the double hash
             final int length = _set.length;
             int probe = 1 + (hash % (length - 2));
             final int loopIndex = index;
             int firstRemoved = -1;

             /**
              * Look until FREE slot or we start to loop
              */
             do {
                 // Identify first removed slot
                 if (state == REMOVED && firstRemoved == -1)
                     firstRemoved = index;

                 index -= probe;
                 if (index < 0) {
                     index += length;
                 }
                 state = _states[index];

                 // A FREE slot stops the search
                 if (state == FREE) {
                     if (firstRemoved != -1) {
                         insertKeyAt(firstRemoved, val);
                         return firstRemoved;
                     } else {
                         consumeFreeSlot = true;
                         insertKeyAt(index, val);
                         return index;
                     }
                 }

                 if (state == FULL && _set[index] == val) {
                     return -index - 1;
                 }

                 // Detect loop
             } while (index != loopIndex);

             // We inspected all reachable slots and did not find a FREE one
             // If we found a REMOVED slot we return the first one found
             if (firstRemoved != -1) {
                 insertKeyAt(firstRemoved, val);
                 return firstRemoved;
             }

             // Can a resizing strategy be found that resizes the set?
             throw new IllegalStateException("No free or removed slots available. Key set full?!!");
         }

         void insertKeyAt(int index, int val) {
             _set[index] = val;  // insert value
             _states[index] = FULL;
         }

    protected int XinsertKey( int key ) {
        int hash, probe, index, length;

        final byte[] states = _states;
        final int[] set = _set;
        length = states.length;
        hash = HashFunctions.hash( key ) & 0x7fffffff;
        index = hash % length;
        byte state = states[index];

        consumeFreeSlot = false;

        if ( state == FREE ) {
            consumeFreeSlot = true;
            set[index] = key;
            states[index] = FULL;

            return index;       // empty, all done
        } else if ( state == FULL && set[index] == key ) {
            return -index -1;   // already stored
        } else {                // already FULL or REMOVED, must probe
            // compute the double hash
            probe = 1 + ( hash % ( length - 2 ) );

            // if the slot we landed on is FULL (but not removed), probe
            // until we find an empty slot, a REMOVED slot, or an element
            // equal to the one we are trying to insert.
            // finding an empty slot means that the value is not present
            // and that we should use that slot as the insertion point;
            // finding a REMOVED slot means that we need to keep searching,
            // however we want to remember the offset of that REMOVED slot
            // so we can reuse it in case a "new" insertion (i.e. not an update)
            // is possible.
            // finding a matching value means that we've found that our desired
            // key is already in the table

            if ( state != REMOVED ) {
				// starting at the natural offset, probe until we find an
				// offset that isn't full.
				do {
					index -= probe;
					if (index < 0) {
						index += length;
					}
					state = states[index];
				} while ( state == FULL && set[index] != key );
            }

            // if the index we found was removed: continue probing until we
            // locate a free location or an element which equal()s the
            // one we have.
            if ( state == REMOVED) {
                int firstRemoved = index;
                while ( state != FREE && ( state == REMOVED || set[index] != key ) ) {
                    index -= probe;
                    if (index < 0) {
                        index += length;
                    }
                    state = states[index];
                }

                if (state == FULL) {
                    return -index -1;
                } else {
                    set[index] = key;
                    states[index] = FULL;

                    return firstRemoved;
                }
            }
            // if it's full, the key is already stored
            if (state == FULL) {
                return -index -1;
            } else {
                consumeFreeSlot = true;
                set[index] = key;
                states[index] = FULL;

                return index;
            }
        }
    }


    /** {@inheritDoc} */
    public void writeExternal( ObjectOutput out ) throws IOException {
        // VERSION
    	out.writeByte( 0 );

        // SUPER
    	super.writeExternal( out );

    	// NO_ENTRY_KEY
    	out.writeInt( no_entry_key );

    	// NO_ENTRY_VALUE
    	out.writeInt( no_entry_value );
    }


    /** {@inheritDoc} */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
        // VERSION
    	in.readByte();

        // SUPER
    	super.readExternal( in );

    	// NO_ENTRY_KEY
    	no_entry_key = in.readInt();

    	// NO_ENTRY_VALUE
    	no_entry_value = in.readInt();
    }
} // TIntIntHash
