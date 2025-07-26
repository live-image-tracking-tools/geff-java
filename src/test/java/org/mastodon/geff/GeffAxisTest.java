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
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.geff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GeffAxis functionality
 */
public class GeffAxisTest
{

    @Test
    @DisplayName( "Test default constructor" )
    void testDefaultConstructor()
    {
        GeffAxis axis = new GeffAxis();
        assertNull( axis.getName() );
        assertNull( axis.getType() );
        assertNull( axis.getUnit() );
        assertNull( axis.getMin() );
        assertNull( axis.getMax() );
        assertFalse( axis.hasBounds() );
    }

    @Test
    @DisplayName( "Test constructor with required fields" )
    void testConstructorWithRequiredFields()
    {
        GeffAxis axis = new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER );

        assertEquals( "x", axis.getName() );
        assertEquals( GeffAxis.TYPE_SPACE, axis.getType() );
        assertEquals( GeffAxis.UNIT_MICROMETER, axis.getUnit() );
        assertNull( axis.getMin() );
        assertNull( axis.getMax() );
        assertFalse( axis.hasBounds() );
    }

    @Test
    @DisplayName( "Test constructor with all fields" )
    void testConstructorWithAllFields()
    {
        GeffAxis axis = new GeffAxis( "t", GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND, 0.0, 125.0 );

        assertEquals( "t", axis.getName() );
        assertEquals( GeffAxis.TYPE_TIME, axis.getType() );
        assertEquals( GeffAxis.UNIT_SECOND, axis.getUnit() );
        assertEquals( 0.0, axis.getMin() );
        assertEquals( 125.0, axis.getMax() );
        assertTrue( axis.hasBounds() );
        assertEquals( 125.0, axis.getRange() );
    }

    @Test
    @DisplayName( "Test time axis factory methods" )
    void testTimeAxisFactoryMethods()
    {
        // With bounds
        GeffAxis timeAxisWithBounds = GeffAxis.createTimeAxis( "t", GeffAxis.UNIT_SECOND, 0.0, 125.0 );
        assertEquals( "t", timeAxisWithBounds.getName() );
        assertEquals( GeffAxis.TYPE_TIME, timeAxisWithBounds.getType() );
        assertEquals( GeffAxis.UNIT_SECOND, timeAxisWithBounds.getUnit() );
        assertTrue( timeAxisWithBounds.hasBounds() );

        // Without bounds
        GeffAxis timeAxisNoBounds = GeffAxis.createTimeAxis( "t", GeffAxis.UNIT_SECOND );
        assertEquals( "t", timeAxisNoBounds.getName() );
        assertEquals( GeffAxis.TYPE_TIME, timeAxisNoBounds.getType() );
        assertEquals( GeffAxis.UNIT_SECOND, timeAxisNoBounds.getUnit() );
        assertFalse( timeAxisNoBounds.hasBounds() );
    }

    @Test
    @DisplayName( "Test space axis factory methods" )
    void testSpaceAxisFactoryMethods()
    {
        // With bounds
        GeffAxis spaceAxisWithBounds = GeffAxis.createSpaceAxis( "x", GeffAxis.UNIT_MICROMETER, 764.42, 2152.3 );
        assertEquals( "x", spaceAxisWithBounds.getName() );
        assertEquals( GeffAxis.TYPE_SPACE, spaceAxisWithBounds.getType() );
        assertEquals( GeffAxis.UNIT_MICROMETER, spaceAxisWithBounds.getUnit() );
        assertTrue( spaceAxisWithBounds.hasBounds() );
        assertEquals( 2152.3 - 764.42, spaceAxisWithBounds.getRange(), 0.001 );

        // Without bounds
        GeffAxis spaceAxisNoBounds = GeffAxis.createSpaceAxis( "y", GeffAxis.UNIT_MICROMETER );
        assertEquals( "y", spaceAxisNoBounds.getName() );
        assertEquals( GeffAxis.TYPE_SPACE, spaceAxisNoBounds.getType() );
        assertEquals( GeffAxis.UNIT_MICROMETER, spaceAxisNoBounds.getUnit() );
        assertFalse( spaceAxisNoBounds.hasBounds() );
    }

    @Test
    @DisplayName( "Test bounds validation" )
    void testBoundsValidation()
    {
        GeffAxis axis = new GeffAxis();

        // Valid bounds
        assertDoesNotThrow( () -> axis.setBounds( 0.0, 100.0 ) );
        assertTrue( axis.hasBounds() );
        assertEquals( 100.0, axis.getRange() );

        // Equal bounds should be valid
        assertDoesNotThrow( () -> axis.setBounds( 50.0, 50.0 ) );
        assertEquals( 0.0, axis.getRange() );

        // Invalid bounds (min > max)
        assertThrows( IllegalArgumentException.class, () -> axis.setBounds( 100.0, 50.0 ) );

        // Test individual setters
        axis.setMin( null );
        axis.setMax( null );
        assertDoesNotThrow( () -> axis.setMin( 10.0 ) );
        assertDoesNotThrow( () -> axis.setMax( 20.0 ) );

        // Setting min > existing max should fail
        assertThrows( IllegalArgumentException.class, () -> axis.setMin( 30.0 ) );
    }

    @Test
    @DisplayName( "Test type validation" )
    void testTypeValidation()
    {
        GeffAxis axis = new GeffAxis();

        // Valid types
        assertDoesNotThrow( () -> axis.setType( GeffAxis.TYPE_TIME ) );
        assertDoesNotThrow( () -> axis.setType( GeffAxis.TYPE_SPACE ) );
        assertDoesNotThrow( () -> axis.setType( null ) ); // null should be
                                                          // allowed for setter

        // Invalid type
        assertThrows( IllegalArgumentException.class, () -> axis.setType( "invalid" ) );
        assertThrows( IllegalArgumentException.class, () -> axis.setType( "dimension" ) );
    }

    @Test
    @DisplayName( "Test axis validation" )
    void testAxisValidation()
    {
        // Valid axis
        GeffAxis validAxis = new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 100.0 );
        assertDoesNotThrow( () -> validAxis.validate() );

        // Missing name
        GeffAxis missingName = new GeffAxis( null, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER );
        assertThrows( IllegalArgumentException.class, () -> missingName.validate() );

        // Empty name
        GeffAxis emptyName = new GeffAxis( "", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER );
        assertThrows( IllegalArgumentException.class, () -> emptyName.validate() );

        // Missing type
        GeffAxis missingType = new GeffAxis( "x", null, GeffAxis.UNIT_MICROMETER );
        assertThrows( IllegalArgumentException.class, () -> missingType.validate() );

        // Invalid type - create axis with valid type then test validation
        // separately
        GeffAxis validAxisForTypeTest = new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER );
        assertDoesNotThrow( () -> validAxisForTypeTest.validate() ); // This
                                                                     // should
                                                                     // pass

        // Test that setType rejects invalid types
        GeffAxis axisForInvalidType = new GeffAxis();
        axisForInvalidType.setName( "x" );
        axisForInvalidType.setUnit( GeffAxis.UNIT_MICROMETER );
        assertThrows( IllegalArgumentException.class, () -> axisForInvalidType.setType( "invalid" ) );

        // Missing unit
        GeffAxis missingUnit = new GeffAxis( "x", GeffAxis.TYPE_SPACE, null );
        assertThrows( IllegalArgumentException.class, () -> missingUnit.validate() );

        // Empty unit
        GeffAxis emptyUnit = new GeffAxis( "x", GeffAxis.TYPE_SPACE, "" );
        assertThrows( IllegalArgumentException.class, () -> emptyUnit.validate() );
    }

    @Test
    @DisplayName( "Test equals and hashCode" )
    void testEqualsAndHashCode()
    {
        GeffAxis axis1 = new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 100.0 );
        GeffAxis axis2 = new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 100.0 );
        GeffAxis axis3 = new GeffAxis( "y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 100.0 );

        // Test equals
        assertEquals( axis1, axis2 );
        assertNotEquals( axis1, axis3 );
        assertNotEquals( axis1, null );
        assertEquals( axis1, axis1 ); // reflexive

        // Test hashCode consistency
        assertEquals( axis1.hashCode(), axis2.hashCode() );
        // Note: axis1 and axis3 may or may not have the same hashCode (hash
        // collision
        // allowed)
    }

    @Test
    @DisplayName( "Test toString" )
    void testToString()
    {
        GeffAxis axisWithBounds = new GeffAxis( "t", GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND, 0.0, 125.0 );
        String str = axisWithBounds.toString();

        assertTrue( str.contains( "name='t'" ) );
        assertTrue( str.contains( "type='time'" ) );
        assertTrue( str.contains( "unit='seconds'" ) );
        assertTrue( str.contains( "min=0.0" ) );
        assertTrue( str.contains( "max=125.0" ) );

        GeffAxis axisNoBounds = new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER );
        String strNoBounds = axisNoBounds.toString();

        assertTrue( strNoBounds.contains( "name='x'" ) );
        assertTrue( strNoBounds.contains( "type='space'" ) );
        assertTrue( strNoBounds.contains( "unit='micrometers'" ) );
        assertFalse( strNoBounds.contains( "min=" ) );
        assertFalse( strNoBounds.contains( "max=" ) );
    }

    @Test
    @DisplayName( "Test example axes from specification" )
    void testSpecificationExamples()
    {
        // Time axis: {'name': 't', 'type': "time", 'unit': "seconds", 'min': 0,
        // 'max':
        // 125}
        GeffAxis timeAxis = new GeffAxis( "t", GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND, 0.0, 125.0 );
        assertDoesNotThrow( () -> timeAxis.validate() );
        assertEquals( "t", timeAxis.getName() );
        assertEquals( GeffAxis.TYPE_TIME, timeAxis.getType() );
        assertEquals( GeffAxis.UNIT_SECOND, timeAxis.getUnit() );
        assertEquals( 0.0, timeAxis.getMin() );
        assertEquals( 125.0, timeAxis.getMax() );

        // Space axis: {'name': 'z', 'type': "space", 'unit': "micrometers",
        // 'min':
        // 1523.36, 'max': 4398.1}
        GeffAxis zAxis = new GeffAxis( "z", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 1523.36, 4398.1 );
        assertDoesNotThrow( () -> zAxis.validate() );
        assertEquals( "z", zAxis.getName() );
        assertEquals( GeffAxis.TYPE_SPACE, zAxis.getType() );
        assertEquals( GeffAxis.UNIT_MICROMETER, zAxis.getUnit() );
        assertEquals( 1523.36, zAxis.getMin() );
        assertEquals( 4398.1, zAxis.getMax() );

        // Space axis: {'name': 'y', 'type': "space", 'unit': "micrometers",
        // 'min':
        // 81.667, 'max': 1877.7}
        GeffAxis yAxis = new GeffAxis( "y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 81.667, 1877.7 );
        assertDoesNotThrow( () -> yAxis.validate() );

        // Space axis: {'name': 'x', 'type': "space", 'unit': "micrometers",
        // 'min':
        // 764.42, 'max': 2152.3}
        GeffAxis xAxis = new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 764.42, 2152.3 );
        assertDoesNotThrow( () -> xAxis.validate() );
    }
}
