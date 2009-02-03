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
package org.apache.directory.server.core;


import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represents 'Change Sequence Number' in LDUP specification.
 * 
 * A CSN is a composition of a timestamp, a change count, a replica ID and a 
 * operation sequence number.
 * 
 * It's described in http://tools.ietf.org/html/draft-ietf-ldup-model-09.
 * 
 * The CSN syntax is :
 * <pre>
 * <CSN>            ::= <timestamp> # <changeCount> # <replicaId> # <modifierNumber>
 * <timestamp>      ::= A GMT based time, YYYYMMDDhhmmss.SSSSSSZ
 * <changeCount>    ::= [0-9]+
 * <replicaId>      ::= [0-9][0-9][0-9]
 * <modifierNumber> ::= [0-9][0-9][0-9][0-9][0-9][0-9]
 * </pre>
 *  
 * It distinguishes a change made on an object on a server,
 * and if two operations take place during the same timeStamp,
 * the operation sequence number makes those operations distinct.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CSN implements Serializable, Comparable<CSN>
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( CSN.class );

    /** The timeStamp of this operation */
    private final long timestamp;

    /** The server identification */
    private final int replicaId;

    /** The operation number in a modification operation */
    private final int operationNumber;
    
    /** The changeCount to distinguish operations done in the same second */
    private final int changeCount;  

    /** Stores the String representation of the CSN */
    private transient String csnStr;

    /** Stores the byte array representation of the CSN */
    private transient byte[] csnBytes;

    /** The Timestamp syntax. The last 'Z' is _not_ the Time Zone */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMddHHmmss'.'SSSSSS'Z'" );


    /**
     * Creates a new instance.
     * <b>This method should be used only for deserializing a CSN</b> 
     * 
     * @param timestamp GMT timestamp of modification
     * @param changeCount The operation increment
     * @param replicaId Replica ID where modification occurred (<tt>[0-9]{1,3}</tt>)
     * @param operationNumber Operation number in a modification operation. 
     * Currently, '000000'
     */
    /* No qualifier */ CSN( long timestamp, int changeCount, int replicaId, int operationNumber )
    {
        this.timestamp = timestamp;
        this.replicaId = replicaId;
        this.operationNumber = operationNumber;
        this.changeCount = changeCount;
        
        // Build the CSN string now
        StringBuilder sb = new StringBuilder( 40 );
        
        synchronized ( sdf )
        {
            sb.append( sdf.format( timestamp ) );
        }
        
        sb.append( '#' ).append( changeCount );
        sb.append( '#' ).append( replicaId );
        sb.append( "#000000" );
        
        csnStr = sb.toString();
        csnBytes = StringTools.getBytesUtf8( csnStr );
    }


    /**
     * Creates a new instance of SimpleCSN from a String.
     * 
     * The string format must be :
     * &lt;timestamp> # &lt;changeCount> # &lt;replica ID> # &lt;operation number>
     *
     * @param value The String containing the CSN
     */
    /* no qualifier */ CSN( String value ) throws InvalidCSNException
    {
        if ( StringTools.isEmpty( value ) )
        {
            String message = "The CSN must not be null or empty";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }

        // Get the Timestamp
        int sepTS = value.indexOf( '#' );
        
        if ( sepTS < 0 )
        {
            String message = "Cannot find a '#' in the CSN '" + value + "'";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }
        
        String timestampStr = value.substring( 0, sepTS ).trim();
        
        synchronized ( sdf )
        {
            try
            {
                timestamp = sdf.parse( timestampStr ).getTime();
            }
            catch ( ParseException pe )
            {
                String message = "Cannot parse the timestamp: '" + timestampStr + "'";
                LOG.error( message );
                throw new InvalidCSNException( message );
            }
        }

        // Get the changeCount. It should be an hex number prefixed with '0x'
        int sepCC = value.indexOf( '#', sepTS + 1 );
        
        if ( sepCC < 0 )
        {
            String message = "Missing a '#' in the CSN '" + value + "'";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }

        String changeCountStr = value.substring( sepTS + 1, sepCC ).trim();
        
        try
        {
            changeCount = Integer.parseInt( changeCountStr, 16 ); 
        }
        catch ( NumberFormatException nfe )
        {
            String message = "The changeCount '" + changeCountStr + "' is not a valid number";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }
        
        // Get the replicaID
        int sepRI = value.indexOf( '#', sepCC + 1 );
        
        if ( sepRI < 0 )
        {
            String message = "Missing a '#' in the CSN '" + value + "'";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }

        String replicaIdStr = value.substring( sepCC + 1, sepRI).trim();
        
        if ( StringTools.isEmpty( replicaIdStr ) )
        {
            String message = "The replicaID must not be null or empty";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }
        
        try
        {
            replicaId = Integer.parseInt( replicaIdStr, 16 ); 
        }
        catch ( NumberFormatException nfe )
        {
            String message = "The replicaID '" + replicaIdStr + "' is not a valid number";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }
        
        // Get the modification number
        if ( sepCC == value.length() )
        {
            String message = "The operationNumber is absent";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }
        
        String operationNumberStr = value.substring( sepRI + 1 ).trim();
        
        try
        {
            operationNumber = Integer.parseInt( operationNumberStr, 16 ); 
        }
        catch ( NumberFormatException nfe )
        {
            String message = "The operationNumber '" + operationNumberStr + "' is not a valid number";
            LOG.error( message );
            throw new InvalidCSNException( message );
        }
        
        csnStr = value;
        csnBytes = StringTools.getBytesUtf8( csnStr );
    }


    /**
     * Creates a new instance of SimpleCSN from the serialized data
     *
     * @param value The byte array which contains the serialized CSN
     */
    /** Package protected */ CSN( byte[] value )
    {
        csnStr = StringTools.utf8ToString( value );
        CSN csn = new CSN( csnStr );
        timestamp = csn.timestamp;
        changeCount = csn.changeCount;
        replicaId = csn.replicaId;
        operationNumber = csn.operationNumber;
        csnBytes = new byte[value.length];
        System.arraycopy( value, 0, csnBytes, 0, value.length );
    }


    /**
     * Get the CSN as a byte array.
     * 
     * @return A byte array representing theCSN
     */
    public byte[] getBytes()
    {
        if ( csnBytes == null )
        {
            csnBytes = StringTools.getBytesUtf8( csnStr );
        }

        return csnBytes;
    }


    /**
     * @return The timestamp
     */
    public long getTimestamp()
    {
        return timestamp;
    }


    /**
     * @return The changeCount
     */
    public int getChangeCount()
    {
        return changeCount;
    }


    /**
     * @return The replicaId
     */
    public int getReplicaId()
    {
        return replicaId;
    }


    /**
     * @return The operation number
     */
    public int getOperationNumber()
    {
        return operationNumber;
    }


    /**
     * @return The CSN as a String
     */
    public String toString()
    {
        if ( csnStr == null )
        {
            StringBuilder buf = new StringBuilder( 40 );
            
            synchronized( sdf )
            {
                buf.append( sdf.format( new Date( timestamp ) ) );
            }
            
            buf.append( '#' );
            buf.append( "0x" ).append( Integer.toHexString( changeCount ) );
            buf.append( '#' );
            buf.append( replicaId );
            buf.append( '#' );
            buf.append( "0x" ).append( Integer.toHexString( operationNumber ) );
            csnStr = buf.toString();
        }
        
        return csnStr;
    }


    /**
     * Returns a hash code value for the object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        int h = 37;
        
        h = h*17 + (int)(timestamp ^ (timestamp >>> 32));
        h = h*17 + changeCount;
        h = h*17 + replicaId;
        h = h*17 + operationNumber;
        
        return h;
    }


    /**
     * Indicates whether some other object is "equal to" this one
     * 
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj argument; 
     * <code>false</code> otherwise.
     */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof CSN ) )
        {
            return false;
        }

        CSN that = ( CSN ) o;

        return 
            ( timestamp == that.timestamp ) &&
            ( changeCount == that.changeCount ) &&
            ( replicaId == that.replicaId ) &&
            ( operationNumber == that.operationNumber );
    }


    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * 
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *      is less than, equal to, or greater than the specified object.
     */
    public int compareTo( CSN csn )
    {
        if ( csn == null )
        {
            return 1;
        }
        
        // Compares the timestamp first
        if ( this.timestamp < csn.timestamp )
        {
            return -1;
        }
        else if ( this.timestamp > csn.timestamp )
        {
            return 1;
        }

        // Then the change count
        if ( this.changeCount < csn.changeCount )
        {
            return -1;
        }
        else if ( this.changeCount > csn.changeCount )
        {
            return 1;
        }

        // Then the replicaId
        if ( this.replicaId < csn.replicaId)
        {
            return -1;
        }
        else if ( this.replicaId > csn.replicaId )
        {
            return 1;
        }

        // Last, not least, compares the operation number
        if ( this.operationNumber < csn.operationNumber )
        {
            return -1;
        }
        else if ( this.operationNumber > csn.operationNumber )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }
}