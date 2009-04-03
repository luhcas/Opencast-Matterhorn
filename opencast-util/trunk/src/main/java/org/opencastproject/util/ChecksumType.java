/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *  
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Checksum type represents the method used to generate a checksum.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: ChecksumType.java 678 2008-08-05 14:56:22Z wunden $
 */
public final class ChecksumType implements Serializable {

    /** Serial version uid */
    private static final long serialVersionUID = 1L;    

    /** List of all known checksum types */
	private static Map<String, ChecksumType> types_ = new HashMap<String, ChecksumType>();

	/** Default type md5 */
	public static final ChecksumType DEFAULT_TYPE = new ChecksumType("md5");
	
	/** The type name */
	protected String type = null;
	
	/**
	 * Creates a new checksum type with the given type name.
	 * 
	 * @param type the type name
	 */
	protected ChecksumType(String type) {
		this.type = type;
		types_.put(type, this);
	}
	
	/**
	 * Returns the checksum value.
	 * 
	 * @return the value
	 */
	public String getName() {
		return type;
	}
	
	/**
	 * Returns a checksum type for the given string. <code>Type</code> is considered
	 * to be the name of a checksum type.
	 * 
	 * @param type the type name
	 * @return the checksum type
	 * @throws NoSuchAlgorithmException
	 * 		if the digest is not supported by the java environment
	 */
	public static ChecksumType fromString(String type) throws NoSuchAlgorithmException {
		if (type == null)
			throw new IllegalArgumentException("Argument 'type' is null");
		type = type.toLowerCase();
		ChecksumType checksumType = types_.get(type);
		if (checksumType == null) {
			MessageDigest.getInstance(type);
			checksumType = new ChecksumType(type);
			types_.put(type, checksumType);
		}
		return checksumType;
	}
	
	/**
	 * Returns the type of the checksum gathered from the provided value.
	 * 
	 * @param value the checksum value
	 * @return the type
	 */
	public static ChecksumType fromValue(String value) {
		// TODO: Implement
		throw new IllegalStateException("Not yet implemented");
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ChecksumType) {
			return type.equals(((ChecksumType)obj).type);
		}
		return false;
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return type.hashCode();
	}
	
	@Override
	public String toString() {
		return type;
	}
	
}