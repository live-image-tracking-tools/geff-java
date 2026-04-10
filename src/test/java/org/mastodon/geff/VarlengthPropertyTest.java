/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 Ko Sugawara
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.geff;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for VarlengthProperty class
 */
public class VarlengthPropertyTest
{
    private VarlengthProperty property;

    @Before
    public void setUp()
    {
        // Create test data: 3 nodes with variable-length data
        // Node 0: offset=0, shape=[2, 3] (6 elements)
        // Node 1: offset=6, shape=[3, 2] (6 elements)
        // Node 2: offset=12, shape=[1, 4] (4 elements)
        final Object[] data = new Object[ 16 ];
        for ( int i = 0; i < 16; i++ )
        {
            data[ i ] = ( double ) i;
        }

        final long[][] offsets = new long[ 3 ][];
        offsets[ 0 ] = new long[] { 0, 2, 3 };
        offsets[ 1 ] = new long[] { 6, 3, 2 };
        offsets[ 2 ] = new long[] { 12, 1, 4 };

        final boolean[] missing = new boolean[] { false, false, false };

        property = new VarlengthProperty( "test_polygon", "float64", data, offsets, missing );
    }

    @After
    public void tearDown()
    {
        property = null;
    }

    @Test
    public void testBasicProperties()
    {
        assertEquals( "test_polygon", property.getName() );
        assertEquals( "float64", property.getDtype() );
        assertNotNull( property.getData() );
        assertEquals( 16, property.getData().length );
        assertNotNull( property.getOffsets() );
        assertEquals( 3, property.getOffsets().length );
    }

    @Test
    public void testMissingValues()
    {
        assertFalse( property.isMissing( 0 ) );
        assertFalse( property.isMissing( 1 ) );
        assertFalse( property.isMissing( 2 ) );
        assertFalse( property.isMissing( -1 ) ); // Out of bounds
        assertFalse( property.isMissing( 3 ) ); // Out of bounds
    }

    @Test
    public void testGetNodeData()
    {
        // Test Node 0
        final Object nodeData0 = property.getNodeData( 0 );
        assertNotNull( nodeData0 );
        assertTrue( nodeData0 instanceof Object[] );
        assertEquals( 6, ( ( Object[] ) nodeData0 ).length );

        // Test Node 1
        final Object nodeData1 = property.getNodeData( 1 );
        assertNotNull( nodeData1 );
        assertTrue( nodeData1 instanceof Object[] );
        assertEquals( 6, ( ( Object[] ) nodeData1 ).length );

        // Test Node 2
        final Object nodeData2 = property.getNodeData( 2 );
        assertNotNull( nodeData2 );
        assertTrue( nodeData2 instanceof Object[] );
        assertEquals( 4, ( ( Object[] ) nodeData2 ).length );
    }

    @Test
    public void testToString()
    {
        final String str = property.toString();
        assertTrue( str.contains( "test_polygon" ) );
        assertTrue( str.contains( "float64" ) );
        assertTrue( str.contains( "dataLength=16" ) );
        assertTrue( str.contains( "nodeCount=3" ) );
    }

    @Test
    public void testEquals()
    {
        final VarlengthProperty prop2 = new VarlengthProperty( "test_polygon", "float64", new Object[ 10 ], new long[ 3 ][], null );
        // Equals should only compare name and dtype
        assertEquals( property, prop2 );

        final VarlengthProperty prop3 = new VarlengthProperty( "different_name", "float64", property.getData(), property.getOffsets(), null );
        assertNotEquals( property, prop3 );
    }

    @Test
    public void testHashCode()
    {
        // Objects with same name and dtype should have same hash
        final VarlengthProperty prop2 = new VarlengthProperty( "test_polygon", "float64", new Object[ 10 ], new long[ 3 ][], null );
        assertEquals( property.hashCode(), prop2.hashCode() );
    }

    @Test
    public void testGetNodeDataWithMissing()
    {
        final boolean[] missing = new boolean[] { false, true, false };
        final VarlengthProperty propWithMissing = new VarlengthProperty(
                "test",
                "float64",
                property.getData(),
                property.getOffsets(),
                missing );

        // Node 0 should have data
        assertNotNull( propWithMissing.getNodeData( 0 ) );

        // Node 1 should return null (missing)
        assertNull( propWithMissing.getNodeData( 1 ) );

        // Node 2 should have data
        assertNotNull( propWithMissing.getNodeData( 2 ) );
    }

    @Test
    public void testGetNodeDataOutOfBounds()
    {
        assertNull( property.getNodeData( -1 ) );
        assertNull( property.getNodeData( 3 ) );
        assertNull( property.getNodeData( 100 ) );
    }
}
