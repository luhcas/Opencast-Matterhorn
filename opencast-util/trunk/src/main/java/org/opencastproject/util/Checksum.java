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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class stores value and type of a generated checksum.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: Checksum.java 1639 2008-12-08 15:45:01Z wunden $
 */
public final class Checksum implements Serializable {

    /** Serial version uid */
    private static final long serialVersionUID = 1L;

    /** The checksum value */
	private String value_ = null;
	
	/** The checksum type */
	private ChecksumType type_ = null;
	
	/**
	 * Creates a new checksum object of the specified value and checksum type.
	 * 
	 * @param value the value
	 * @param type the type
	 */
	private Checksum(String value, ChecksumType type) {
		if (value == null)
			throw new IllegalArgumentException("Checksum value is null");
		if (type == null)
			throw new IllegalArgumentException("Checksum type is null");
		value_ = value;
		type_ = type;
	}

	/**
	 * Returns the checksum type.
	 * 
	 * @return the type
	 */
	public ChecksumType getType() {
		return type_;
	}
	
	/**
	 * Returns the checksum value.
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value_;
	}
	
	/**
	 * Converts the checksum to a hex string.
	 * 
	 * @param data the digest
	 * @return the digest hex representation
	 */
	private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
        	int halfbyte = (data[i] >>> 4) & 0x0F;
        	int two_halfs = 0;
        	do {
	        	if ((0 <= halfbyte) && (halfbyte <= 9))
	                buf.append((char) ('0' + halfbyte));
	            else
	            	buf.append((char) ('a' + (halfbyte - 10)));
	        	halfbyte = data[i] & 0x0F;
        	} while(two_halfs++ < 1);
        }
        return buf.toString();
    }

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Checksum) {
			Checksum c = (Checksum)obj;
			return type_.equals(c.type_) && value_.equals(c.value_);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (new String(value_)).hashCode();
	}

	@Override
	public String toString() {
		String str = new String(value_);
		str += " (" + type_ + ")";
		return str;
	}

	/**
	 * Creates a checksum of type <code>type</code> and value <code>value</code>.
	 * 
	 * @param type the checksum type name
	 * @param value the checksum value
	 * @return the checksum
	 * @throws NoSuchAlgorithmException
	 * 		if the checksum of the specified type cannot be created
	 */
	public static Checksum create(String type, String value) throws NoSuchAlgorithmException {
		ChecksumType t = ChecksumType.fromString(type);
	    return new Checksum(value, t);
	}

	/**
	 * Creates a checksum of type <code>type</code> and value <code>value</code>.
	 * 
	 * @param type the checksum type
	 * @param value the checksum value
	 * @return the checksum
	 */
	public static Checksum create(ChecksumType type, String value) {
	    return new Checksum(value, type);
	}

	/**
	 * Creates a checksum of type <code>type</code> from the given file.
	 * 
	 * @param type the checksum type
	 * @param file the file
	 * @return the checksum
	 * @throws NoSuchAlgorithmException
	 * 		if the checksum of the specified type cannot be created
	 * @throws IOException
	 * 		if the file cannot be accessed
	 */
	public static Checksum create(ChecksumType type, File file) throws NoSuchAlgorithmException, IOException {
		MessageDigest checksum = MessageDigest.getInstance(type.getName());
    	BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
    	try {
	    	byte[] bytes = new byte[1024];
	    	int len = 0;
	    	while ((len = is.read(bytes)) >= 0) {
	    		checksum.update(bytes, 0, len);
	    	}
    	} finally {
    		if (is != null)
    			is.close();
    	}
	    return new Checksum(convertToHex(checksum.digest()), type);
	}
	
}