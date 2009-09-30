/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@CleanupLevel( Level.CLASS )
public class MetaAttributeTypeHandlerIT
{
    private static final String DESCRIPTION0 = "A test attributeType";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.2.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.2.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.2.100002";


    public static DirectoryService service;

    
    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the a schema's attributeType entity container
     * @throws Exception on failure
     */
    private LdapDN getAttributeTypeContainer( String schemaName ) throws Exception
    {
        return new LdapDN( "ou=attributeTypes,cn=" + schemaName );
    }


    private static AttributeTypeRegistry getAttributeTypeRegistry()
    {
        return service.getRegistries().getAttributeTypeRegistry();
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    @Test
    public void testAddAttributeType() throws Exception
    {
        Attributes attrs = AttributeUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaAttributeType",
            "m-oid:" + OID,
            "m-syntax:" + SchemaConstants.INTEGER_SYNTAX,
            "m-description:" + DESCRIPTION0,
            "m-equality: caseIgnoreMatch",
            "m-singleValue: FALSE",
            "m-usage: directoryOperation" );
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( service.getRegistries().getAttributeTypeRegistry().contains( OID ) );
        assertEquals( getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    @Test
    public void testDeleteAttributeType() throws Exception
    {
        testAddAttributeType();

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "attributeType should be removed from the registry after being deleted", 
            getAttributeTypeRegistry().contains( OID ) );
        
        try
        {
            getAttributeTypeRegistry().lookup( OID );
            fail( "attributeType lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    @Test
    public void testRenameAttributeType() throws Exception
    {
        testAddAttributeType();

        LdapContext schemaRoot = getSchemaContext( service );
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );
        
        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( "m-oid=" + NEW_OID );
        schemaRoot.rename( dn, newdn );

        assertFalse( "old attributeType OID should be removed from the registry after being renamed", 
            getAttributeTypeRegistry().contains( OID ) );
        
        try
        {
            getAttributeTypeRegistry().lookup( OID );
            fail( "attributeType lookup should fail after renaming the attributeType" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( getAttributeTypeRegistry().contains( NEW_OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeType() throws Exception
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( "m-oid=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "attributeType OID should still be present",
                getAttributeTypeRegistry().contains( OID ) );
        
        assertEquals( "attributeType schema should be set to apache not apachemeta", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeAndChangeRdn() throws Exception
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( "m-oid=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old attributeType OID should NOT be present", 
            getAttributeTypeRegistry().contains( OID ) );
        
        assertTrue( "new attributeType OID should be present", 
            getAttributeTypeRegistry().contains( NEW_OID ) );
        
        assertEquals( "attributeType with new oid should have schema set to apache NOT apachemeta", 
            getAttributeTypeRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    @Test
    public void testModifyAttributeTypeWithModificationItems() throws Exception
    {
        testAddAttributeType();
        
        AttributeType at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.INTEGER_SYNTAX );

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );
        
        ModificationItem[] mods = new ModificationItem[2];
        Attribute attr = new BasicAttribute( "m-description", DESCRIPTION1 );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new BasicAttribute( "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX );
        mods[1] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "attributeType OID should still be present", 
            getAttributeTypeRegistry().contains( OID ) );
        
        assertEquals( "attributeType schema should be set to apachemeta", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
        
        at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.DIRECTORY_STRING_SYNTAX );
    }

    
    @Test
    public void testModifyAttributeTypeWithAttributes() throws Exception
    {
        testAddAttributeType();
        
        AttributeType at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.INTEGER_SYNTAX );

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );
        
        Attributes mods = new BasicAttributes( true );
        mods.put( "m-description", DESCRIPTION1 );
        mods.put( "m-syntax", SchemaConstants.DIRECTORY_STRING_SYNTAX );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "attributeType OID should still be present", 
            getAttributeTypeRegistry().contains( OID ) );
        
        assertEquals( "attributeType schema should be set to apachemeta", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );

        at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), SchemaConstants.DIRECTORY_STRING_SYNTAX );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    private void addDependeeAttributeType() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaAttributeType" );
        attrs.put( oc );
        attrs.put( "m-oid", DEPENDEE_OID );
        attrs.put( "m-syntax", SchemaConstants.INTEGER_SYNTAX );
        attrs.put( "m-description", DESCRIPTION0 );
        attrs.put( "m-equality", "caseIgnoreMatch" );
        attrs.put( "m-singleValue", "FALSE" );
        attrs.put( "m-usage", "directoryOperation" );
        attrs.put( "m-supAttributeType", OID );
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + DEPENDEE_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getAttributeTypeRegistry().contains( DEPENDEE_OID ) );
        assertEquals( getAttributeTypeRegistry().getSchemaName( DEPENDEE_OID ), "apachemeta" );
    }


    @Test
    public void testDeleteAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeType();

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );
        addDependeeAttributeType();
        
        try
        {
            getSchemaContext( service ).destroySubcontext( dn );
            fail( "should not be able to delete a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after delete failure", 
            getAttributeTypeRegistry().contains( OID ) );
    }
    
    
    @Test
    @Ignore
    public void testMoveAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeType();
        addDependeeAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( "m-oid=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeAndChangeRdnWhenInUse() throws Exception
    {
        testAddAttributeType();
        addDependeeAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( "m-oid=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().contains( OID ) );
    }

    
    @Test
    public void testRenameAttributeTypeWhenInUse() throws Exception
    {
        testAddAttributeType();

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );
        addDependeeAttributeType();
        
        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( "m-oid=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to rename a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after rename failure", 
            getAttributeTypeRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    @Ignore
    public void testMoveAttributeTypeToTop() throws Exception
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );

        LdapDN top = new LdapDN();
        top.add( "m-oid=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a attributeType up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeToComparatorContainer() throws Exception
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( "m-oid=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a attributeType into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().contains( OID ) );
    }
    
    
    @Test
    public void testAddAttributeTypeToDisabledSchema() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaAttributeType" );
        attrs.put( oc );
        attrs.put( "m-oid", OID );
        attrs.put( "m-syntax", SchemaConstants.INTEGER_SYNTAX );
        attrs.put( "m-description", DESCRIPTION0 );
        attrs.put( "m-equality", "caseIgnoreMatch" );
        attrs.put( "m-singleValue", "FALSE" );
        attrs.put( "m-usage", "directoryOperation" );
        
        LdapDN dn = getAttributeTypeContainer( "nis" );
        dn.add( "m-oid=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( "adding new attributeType to disabled schema should not register it into the registries", 
            getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveAttributeTypeToDisabledSchema() throws Exception
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( "m-oid=" + OID );

        // nis is inactive by default
        LdapDN newdn = getAttributeTypeContainer( "nis" );
        newdn.add( "m-oid=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "attributeType OID should no longer be present", 
            getAttributeTypeRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveMatchingRuleToEnabledSchema() throws Exception
    {
        testAddAttributeTypeToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getAttributeTypeContainer( "nis" );
        dn.add( "m-oid=" + OID );

        assertFalse( "attributeType OID should NOT be present when added to disabled nis schema", 
            getAttributeTypeRegistry().contains( OID ) );

        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( "m-oid=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "attributeType OID should be present when moved to enabled schema", 
            getAttributeTypeRegistry().contains( OID ) );
        
        assertEquals( "attributeType should be in apachemeta schema after move", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
}
