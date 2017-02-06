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

import gnu.trove.procedure.TCharProcedure;
import gnu.trove.impl.HashFunctions;
import gnu.trove.impl.Constants;

import java.util.Arrays;

//////////////////////////////////////////////////
// THIS IS A GENERATED CLASS. DO NOT HAND EDIT! //
//////////////////////////////////////////////////


/**
 * An open addressed hashing implementation for char primitives.
 *
 * Created: Sun Nov  4 08:56:06 2001
 *
 * @author Eric D. Friedman, Rob Eden, Jeff Randall
 * @version $Id: _E_Hash.template,v 1.1.2.6 2009/11/07 03:36:44 robeden Exp $
 */
abstract public class TCharHash extends TPrimitiveHash {
	static final long serialVersionUID = 1L;

    /** the set of chars */
    public transient char[] _set;

    /**
     * value that represents null
     *
     * NOTE: should not be modified after the Hash is created, but is
     *       not final because of Externalization
     *
     */
    protected char no_entry_value;

    protected boolean consumeFreeSlot;


    /**
     * Creates a new <code>TCharHash</code> instance with the default
     * capacity and load factor.
     */
    public TCharHash() {
        super();
        no_entry_value = Constants.DEFAULT_CHAR_NO_ENTRY_VALUE;
        //noinspection RedundantCast
        if ( no_entry_value != ( char ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Creates a new <code>TCharHash</code> instance whose capacity
     * is the next highest prime above <tt>initialCapacity + 1</tt>
     * unless that value is already prime.
     *
     * @param initialCapacity an <code>int</code> value
     */
    public TCharHash( int initialCapacity ) {
        super( initialCapacity );
        no_entry_value = Constants.DEFAULT_CHAR_NO_ENTRY_VALUE;
        //noinspection RedundantCast
        if ( no_entry_value != ( char ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Creates a new <code>TCharHash</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     */
    public TCharHash( int initialCapacity, float loadFactor ) {
        super(initialCapacity, loadFactor);
        no_entry_value = Constants.DEFAULT_CHAR_NO_ENTRY_VALUE;
        //noinspection RedundantCast
        if ( no_entry_value != ( char ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Creates a new <code>TCharHash</code> instance with a prime
     * value at or near the specified capacity and load factor.
     *
     * @param initialCapacity used to find a prime capacity for the table.
     * @param loadFactor used to calculate the threshold over which
     * rehashing takes place.
     * @param no_entry_value value that represents null
     */
    public TCharHash( int initialCapacity, float loadFactor, char no_entry_value ) {
        super(initialCapacity, loadFactor);
        this.no_entry_value = no_entry_value;
        //noinspection RedundantCast
        if ( no_entry_value != ( char ) 0 ) {
            Arrays.fill( _set, no_entry_value );
        }
    }


    /**
     * Returns the value that is used to represent null. The default
     * value is generally zero, but can be changed during construction
     * of the collection.
     *
     * @return the value that represents null
     */
    public char getNoEntryValue() {
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
        _set = new char[capacity];
        return capacity;
    }


    /**
     * Searches the set for <tt>val</tt>
     *
     * @param val an <code>char</code> value
     * @return a <code>boolean</code> value
     */
    public boolean contains( char val ) {
        return index(val) >= 0;
    }


    /**
     * Executes <tt>procedure</tt> for each element in the set.
     *
     * @param procedure a <code>TObjectProcedure</code> value
     * @return false if the loop over the set terminated because
     * the procedure returned false for some value.
     */
    public boolean forEach( TCharProcedure procedure ) {
        byte[] states = _states;
        char[] set = _set;
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
        _set[index] = no_entry_value;
        super.removeAt( index );
    }


    /**
     * Locates the index of <tt>val</tt>.
     *
     * @param val an <code>char</code> value
     * @return the index of <tt>val</tt> or -1 if it isn't in the set.
     */
    protected int index( char val ) {
        int hash, probe, index, length;

        final byte[] states = _states;
        final char[] set = _set;
        length = states.length;
        hash = HashFunctions.hash( val ) & 0x7fffffff;
        index = hash % length;
        byte state = states[index];

        if (state == FREE)
            return -1;

        if (state == FULL && set[index] == val)
            return index;

        return indexRehashed(val, index, hash, state);
    }

    int indexRehashed(char key, int index, int hash, byte state) {
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
     * @param val an <code>char</code> value
     * @return an <code>int</code> value
     */
    protected int insertKey( char val ) {
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

    int insertKeyRehash(char val, int index, int hash, byte state) {
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

    void insertKeyAt(int index, char val) {
        _set[index] = val;  // insert value
        _states[index] = FULL;
    }

} // TCharHash
