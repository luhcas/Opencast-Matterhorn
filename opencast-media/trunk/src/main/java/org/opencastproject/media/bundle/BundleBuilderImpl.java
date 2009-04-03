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
package org.opencastproject.media.bundle;

import org.opencastproject.media.bundle.handle.Handle;
import org.opencastproject.media.bundle.handle.HandleBuilder;
import org.opencastproject.media.bundle.handle.HandleBuilderFactory;
import org.opencastproject.media.bundle.handle.HandleException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.IdBuilderFactory;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UnknownFileTypeException;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

/**
 * This class provides factory methods for the creation of bundles from manifest files,
 * directories or from sratch.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleBuilderImpl implements BundleBuilder {

	/** The directory used to build bundles */
	private static String TMP_DIR_NAME = "bundlebuilder";
	
	/** Temp directory for the creation of bundles */
	private static File TMP_DIR = null;
	
	/** The handle builder */
	private HandleBuilder handleBuilder = null;
	
	/**
	 * Creates a new bundle builder. The builder will try to setup a temporary directory
	 * from the java environment and throws Exceptions if this operation fails.
	 * 
	 * @throws IllegalStateException
	 * 				if the temporary directory cannot be created or is not accessible
	 */
	public BundleBuilderImpl() {
		TMP_DIR = FileSupport.getTempDirectory(TMP_DIR_NAME);
		HandleBuilderFactory builderFactory = HandleBuilderFactory.newInstance();
		handleBuilder = builderFactory.newHandleBuilder();
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#createNew()
	 */
	public Bundle createNew() throws BundleException {
		String bundleRootPath = PathSupport.concat(new String[] {
			TMP_DIR.getAbsolutePath(),
			IdBuilderFactory.newInstance().newIdBuilder().createNew()
		});
		return createNew(null, new File(bundleRootPath));
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#createNew(org.opencastproject.media.bundle.handle.Handle)
	 */
	public Bundle createNew(Handle identifier) throws BundleException {
		String bundleRootPath = PathSupport.concat(new String[] {
			TMP_DIR.getAbsolutePath(),
			identifier.getNamingAuthority(),
			identifier.getLocalName()
		});
		return createNew(identifier, new File(bundleRootPath));
	}
	
	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#createNew(org.opencastproject.media.bundle.handle.Handle)
	 */
	public Bundle createNew(Handle identifier, File bundleRoot) throws BundleException {
		try {
            if (bundleRoot.exists())
                FileSupport.delete(bundleRoot, true);
			bundleRoot = createBundleFilesystem(bundleRoot, identifier);
			return new BundleImpl(bundleRoot, identifier);
		} catch (IOException e) {
			throw new BundleException("Bundle creation failed: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			throw new BundleException("File type not supported: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new BundleException("Unsupported digest algorithm: " + e.getMessage());
		}
	}
	
	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#loadFromManifest(File)
	 */
	public Bundle loadFromManifest(File manifestFile) throws BundleException {
		return loadFromManifest(manifestFile, false);
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#loadFromManifest(java.io.File, boolean)
	 */
	public Bundle loadFromManifest(File manifestFile, boolean wrap) throws BundleException {
		ManifestImpl manifest = null;
		
		// Look for manifest
		if (!manifestFile.exists() || !manifestFile.canRead())
			throw new BundleException("Bundle manifest missing");
		
		if (!manifestFile.getName().equals(Manifest.FILENAME))
			throw new BundleException("Manifest file must be named " + Manifest.FILENAME);
		
		// Read the manifest
		try {
			manifest = ManifestImpl.fromFile(manifestFile, false, wrap, false);
		} catch (NoSuchAlgorithmException e) {
			throw new BundleException("Unsupported digest algorithm: " + e.getMessage());
		} catch (IOException e) {
			throw new BundleException("I/O error while accessing manifest: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			throw new BundleException("Bundle manifest is of unsuported mime type: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new BundleException("Bundle manifest cannot be parsed: " + e.getMessage());
		} catch (TransformerException e) {
			throw new BundleException("Error while updating bundle manifest: " + e.getMessage());
		} catch (SAXException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (XPathExpressionException e) {
			throw new BundleException("Bundle manifest cannot be parsed: " + e.getMessage());
		} catch (ConfigurationException e) {
			throw new BundleException("Configuration error while accessing manifest: " + e.getMessage());
		} catch (HandleException e) {
			throw new BundleException("Handle system error while accessing manifest: " + e.getMessage());
		} catch (ParseException e) {
			throw new BundleException("Error while parsing invalid bundle start date: " + e.getMessage());
		}
		
		return new BundleImpl(manifest);
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#loadFromRepository(java.lang.String)
	 */
	public Bundle loadFromRepository(String path) throws BundleException {
		ManifestImpl manifest = null;
		
		// TODO: Implement
		throw new UnsupportedOperationException("Not yet implemented");

//		// Look for manifest
//		File manifestFile = new File(dir, Manifest.FILENAME);
//		if (!manifestFile.exists() || !manifestFile.canRead()) {
//			throw new BundleException("Bundle manifest missing in " + dir);
//		}
//		
//		// Read the manifest
//		try {
//			manifest = ManifestImpl.fromFile(manifestFile, false, false, false);
//		} catch (NoSuchAlgorithmException e) {
//			throw new BundleException("Unsupported digest algorithm: " + e.getMessage());
//		} catch (IOException e) {
//			throw new BundleException("IO Exception while accessing manifest: " + e.getMessage());
//		} catch (UnknownFileTypeException e) {
//			throw new BundleException("Bundle manifest is of unsuported mime type: " + e.getMessage());
//		} catch (ParserConfigurationException e) {
//			throw new BundleException("Bundle manifest cannot be parsed: " + e.getMessage());
//		} catch (SAXException e) {
//			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
//		} catch (TransformerException e) {
//			throw new BundleException("Error while updating bundle manifest: " + e.getMessage());
//		} catch (XPathExpressionException e) {
//			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
//		} catch (ConfigurationException e) {
//			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
//		} catch (HandleException e) {
//			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
//		} catch (ParseException e) {
//			throw new BundleException("Error while parsing bundle start date: " + e.getMessage());
//		}
//		
//		return new BundleImpl(manifest);
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#loadFromDirectory(java.io.File)
	 */
	public Bundle loadFromDirectory(File dir) throws BundleException {
		ManifestImpl manifest = null;
		
		// Look for manifest
		File manifestFile = new File(dir, Manifest.FILENAME);
		if (!manifestFile.exists() || !manifestFile.canRead()) {
			throw new BundleException("Bundle manifest missing in " + dir);
		}
		
		// Read the manifest
		try {
			manifest = ManifestImpl.fromFile(manifestFile, false, false, false);
		} catch (NoSuchAlgorithmException e) {
			throw new BundleException("Unsupported digest algorithm: " + e.getMessage());
		} catch (IOException e) {
			throw new BundleException("IO Exception while accessing manifest: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			throw new BundleException("Bundle manifest is of unsuported mime type: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new BundleException("Bundle manifest cannot be parsed: " + e.getMessage());
		} catch (SAXException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (TransformerException e) {
			throw new BundleException("Error while updating bundle manifest: " + e.getMessage());
		} catch (XPathExpressionException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (ConfigurationException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (HandleException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (ParseException e) {
			throw new BundleException("Error while parsing bundle start date: " + e.getMessage());
		}
		
		return new BundleImpl(manifest);
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#rebuildFromDirectory(java.io.File)
	 */
	public Bundle rebuildFromDirectory(File dir) throws BundleException {
		return rebuildFromDirectory(dir, false);
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#rebuildFromDirectory(java.io.File, boolean)
	 */
	public Bundle rebuildFromDirectory(File dir, boolean ignoreChecksums) throws BundleException {
		return rebuildFromDirectory(dir, ignoreChecksums, false);
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#rebuildFromDirectory(java.io.File, boolean, boolean)
	 */
	public Bundle rebuildFromDirectory(File dir, boolean ignoreChecksums, boolean verify) throws BundleException {
		ManifestImpl manifest = null;
		
		// Look for manifest
		File manifestFile = new File(dir, Manifest.FILENAME);
		if (!manifestFile.exists() || !manifestFile.canRead()) {
			throw new BundleException("Bundle manifest missing in " + dir);
		}
		
		// Read the manifest
		try {
			manifest = ManifestImpl.fromFile(manifestFile, true, ignoreChecksums, verify);
		} catch (NoSuchAlgorithmException e) {
			throw new BundleException("Unsupported digest algorithm: " + e.getMessage());
		} catch (IOException e) {
			throw new BundleException("IO Exception while accessing manifest: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			throw new BundleException("Bundle manifest is of unsuported mime type: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new BundleException("Bundle manifest cannot be parsed: " + e.getMessage());
		} catch (SAXException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (TransformerException e) {
			throw new BundleException("Error while updating bundle manifest: " + e.getMessage());
		} catch (XPathExpressionException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (ConfigurationException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (HandleException e) {
			throw new BundleException("Error while parsing bundle manifest: " + e.getMessage());
		} catch (ParseException e) {
			throw new BundleException("Error while parsing bundle start date: " + e.getMessage());
		}
		
		return new BundleImpl(manifest);
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#createFromElements(File, boolean)
	 */
	public Bundle createFromElements(File dir, boolean ignoreUnknown) throws BundleException, UnsupportedBundleElementException {
		Handle identifier = null;
		
		// Create handle
		try {
			identifier = handleBuilder.createNew();
		} catch (HandleException e) {
			throw new BundleException("Unable to retreive new handle: " + e.getMessage());
		}
		
		// Create bundle root
		File bundleRoot = null;
		Bundle bundle = null;
		File manifest = null;
		try {
			bundleRoot = createBundleFilesystem(dir, identifier);
			bundle = new BundleImpl(bundleRoot, identifier);
			manifest = bundle.getManifest().getFile();
			
			// Create a bundle element builder
			BundleElementBuilderFactory elementBuilderFactory = BundleElementBuilderFactory.newInstance();
			BundleElementBuilder elementBuilder = elementBuilderFactory.newElementBuilder();
			
			// Look for elements and add them to the bundle
			Stack<File> files = new Stack<File>();
			for (File f : bundleRoot.listFiles())
				files.push(f);
			while (!files.empty()) {
				File f = files.pop();
				
				// Process directories
				if (f.isDirectory()) {
					for (File f2 : f.listFiles())
						files.push(f2);
					continue;
				}

				// Ignore manifest
				else if (f.getAbsolutePath().equals(manifest.getAbsolutePath()))
					continue;
				
				// This might be an element
				BundleElement element = null;
				try {
					element = elementBuilder.elementFromFile(f);
					bundle.add(element);
				} catch (BundleException e) {
					if (!ignoreUnknown)
						throw new UnsupportedBundleElementException(e.getMessage());
					else
						continue;
				} catch (UnsupportedBundleElementException e) {
					if (!ignoreUnknown)
						throw e;
				}
			}
            // Save the (recreated) manifest
			bundle.save();
			return bundle;
		} catch (IOException e) {
			throw new BundleException("I/O error while reading bundle elements: " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			throw new BundleException("Unsupported filetype: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new BundleException("Unsupported digest algorithm: " + e.getMessage());
		}
	}

	/**
	 * @see org.opencastproject.media.bundle.BundleBuilder#loadFromPackage(org.opencastproject.media.bundle.BundlePackager, java.io.InputStream)
	 */
	public Bundle loadFromPackage(BundlePackager packager, InputStream in) throws IOException, BundleException {
		if (packager == null)
			throw new IllegalArgumentException("The packager must not be null");
		if (in == null)
			throw new IllegalArgumentException("The input stream must not be null");
		return packager.unpack(in);
	}
	
	/**
	 * Creates a new bundle by creating a directory within <code>TMP_DIR</code> and
	 * an emtpy manifest file.
	 * 
	 * @param bundleRoot the base dir
	 * @param identifier the bundle identifier
	 * @throws BundleException
	 * 				if the bundle cannot be created
	 */
	private File createBundleFilesystem(File bundleRoot, Handle identifier) throws BundleException {
		File manifestFile = new File(bundleRoot, Manifest.FILENAME);
		try {
            // Create bundle root dir if not existing
            if (!bundleRoot.exists())
                bundleRoot.mkdirs();
			if (!bundleRoot.exists())
				throw new BundleException("Unable to create bundle directory " + bundleRoot.getAbsolutePath());
            // Always create a new manifest
            if (manifestFile.exists() && !manifestFile.delete()) {
                throw new BundleException("Unable to delete existing manifest on bundle filesystem recreation");
            }
            manifestFile.createNewFile();
            ManifestImpl.newInstance(bundleRoot, identifier);
            return bundleRoot;
		} catch (IOException e) {
			throw new BundleException("Unable to create bundle " + identifier + ": " + e.getMessage());
		} catch (UnknownFileTypeException e) {
			throw new BundleException("Unable to create bundle " + identifier + ": " + e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new BundleException("Unable to create bundle " + identifier + ": " + e.getMessage());
		} catch (TransformerException e) {
			throw new BundleException("Unable to create bundle " + identifier + ": " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new BundleException("Error creating bundle " + identifier + ": " + e.getMessage());
		}
	}
	
}