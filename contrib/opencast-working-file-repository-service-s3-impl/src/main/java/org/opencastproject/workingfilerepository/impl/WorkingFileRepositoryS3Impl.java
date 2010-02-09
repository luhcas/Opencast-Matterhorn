/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.workingfilerepository.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkingFileRepositoryS3Impl implements WorkingFileRepository {

    private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryS3Impl.class);

    public static final String S3_BUCKET_NAME_PROP = "workingfilerepo.s3.bucketname";
    public static final String S3_KEY_DELIM_PROP = "workingfilerepo.s3.keydelimiter";
    public static final String AWS_ACCESS_KEY_ID_PROP = "workingfilerepo.s3.awsaccesskeyid";
    public static final String AWS_SECRET_ACCESS_KEY_PROP = "workingfilerepo.s3.awssecretaccesskey";
    public static final String S3_EXPIRY_MINUTES_PROP = "workingfilerepo.s3.expiryminutes";

    private String s3BucketName = null;
    private String s3Delimiter = null;
    private String awsAccessKeyID = null;
    private String awsSecretAccessKey = null;
    private int s3ExpiryMinutes = 0;
    private S3Bucket s3Bucket = null;
    private AWSCredentials awsCredentials;
    private RestS3Service s3Service;

    public WorkingFileRepositoryS3Impl() {
    }

    public void activate(ComponentContext cc) {
        String temp = null;
        if (cc != null) {
            s3BucketName = (temp = cc.getBundleContext().getProperty(S3_BUCKET_NAME_PROP)) != null ? temp.trim() : "johnk";
            s3Delimiter = (temp = cc.getBundleContext().getProperty(S3_KEY_DELIM_PROP)) != null ? temp.trim() : "/";
            awsAccessKeyID = (temp = cc.getBundleContext().getProperty(AWS_ACCESS_KEY_ID_PROP)) != null ? temp.trim() : "";
            awsSecretAccessKey = (temp = cc.getBundleContext().getProperty(AWS_SECRET_ACCESS_KEY_PROP)) != null ? temp.trim() : "";
            s3ExpiryMinutes = (temp = cc.getBundleContext().getProperty(S3_EXPIRY_MINUTES_PROP)) != null ? Integer.parseInt(temp.trim()) : 60;
        }
        else {
            s3BucketName = "johnk";
            s3Delimiter = "/";
            awsAccessKeyID = "";
            awsSecretAccessKey = "";
            s3ExpiryMinutes = 60;
        }
        try {
            checkService();
            s3Bucket = s3Service.getBucket(s3BucketName);
        }
        catch (S3ServiceException e) {
            logger.error("Couldn't get S3Bucket " + s3BucketName, e);
        }
    }

    public void delete(String mediaPackageID, String mediaPackageElementID) {
        checkService();
        S3Object s3Obj = null;
        String objectKey = null;
        try {
            s3Obj = findS3Object(mediaPackageID, mediaPackageElementID);
            if (s3Obj != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("deleting object: " + s3Obj);
                }
                s3Service.deleteObject(s3Bucket, s3Obj.getKey());
            }
            else {
                logger.error("Couldn't find object to delete with mediaPackageID: " + mediaPackageID + " and mediaPackageElementID: " + mediaPackageElementID);
            }
        }
        catch (S3ServiceException e) {
            logger.error("Can't delete object with mediaPackageID: " + mediaPackageID + " and mediaPackageElementID: " + mediaPackageElementID);
        }
    }

    public InputStream get(String mediaPackageID, String mediaPackageElementID) {
        checkService();
        InputStream objStream = null;
        S3Object s3Obj = null;
        try {
            s3Obj = findS3Object(mediaPackageID, mediaPackageElementID);
            if (logger.isInfoEnabled()) {
                logger.info("retrieved S3Object: " + s3Obj);
            }
            if (s3Obj != null) {
                // unfortunately the object returned from findS3Object() has no
                // input stream so go get it with stream
                if (s3Obj.getDataInputStream() == null) {
                    s3Obj = s3Service.getObject(s3Bucket, s3Obj.getKey());
                    objStream = s3Obj.getDataInputStream();
                }
            }
            else {
                logger.error("Couldn't find object with mediaPackageID: " + mediaPackageID + " and mediaPackageElementID: " + mediaPackageElementID);
            }
        }
        catch (S3ServiceException e) {
            logger.error("Can't create S3Service or find objects in bucket: " + s3Bucket, e);
        }
        return objStream;
    }

    public URI getURI(String mediaPackageID, String mediaPackageElementID) {
        checkService();
        String uriString = null;
        URI uri = null;
        S3Object s3Obj = null;
        try {
            s3Obj = findS3Object(mediaPackageID, mediaPackageElementID);
            if (s3Obj != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("getting URI for object: " + s3Obj);
                }
                uriString = S3Service.createSignedGetUrl(s3Bucket.getName(), s3Obj.getKey(), awsCredentials, new Date(System.currentTimeMillis()
                        + s3ExpiryMinutes * 60 * 1000));
                uri = new URI(uriString);
            }
            else {
                logger.error("Can't find object for mediaPackageID: " + mediaPackageID + " mediaPackageElementID: " + mediaPackageElementID);
            }
        }
        catch (S3ServiceException e) {
            logger.error("Can't create Get URI for mediaPackageID: " + mediaPackageID + " mediaPackageElementID: " + mediaPackageElementID, e);
        }
        catch (URISyntaxException e) {
            logger.error("Bad URI: " + uriString, e);
        }
        return uri;
    }

    public URI put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
        checkService();
        String objectKey = makeObjectKey(mediaPackageID, mediaPackageElementID);
        if (logger.isInfoEnabled()) {
            logger.info("putting object: " + objectKey);
        }
        return putObject(objectKey, in);
    }

    public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in) {
        checkService();
        String objectKey = makeObjectKey(mediaPackageID, mediaPackageElementID, filename);
        if (logger.isInfoEnabled()) {
            logger.info("putting object: " + objectKey);
        }
        return putObject(objectKey, in);
    }

    private URI putObject(String fullObjectKey, InputStream in) {
        URI uri = null;
        String uriString = null;
        S3Object s3Obj = null;
        try {
            s3Obj = new S3Object(s3Bucket, fullObjectKey);
            s3Obj.setDataInputStream(in);
            s3Obj.setContentLength(in.available());
            MimeType mimeType = determineMimeType(fullObjectKey);
            s3Obj.setContentType(mimeType.toString());
            s3Obj = s3Service.putObject(s3Bucket.getName(), s3Obj);
            s3Obj.closeDataInputStream();
            uriString = S3Service.createSignedGetUrl(s3Bucket.getName(), fullObjectKey, awsCredentials, new Date(System.currentTimeMillis() + s3ExpiryMinutes
                    * 60 * 1000));
            uri = new URI(uriString);
        }
        catch (S3ServiceException e) {
            logger.error("Can't putObject: " + s3Obj, e);
        }
        catch (URISyntaxException e) {
            logger.error("Bad URI: " + uriString, e);
        }
        catch (IOException e) {
            logger.error("Couldn't get available bytes for input stream", e);
        }
        catch (UnknownFileTypeException e) {
            logger.error("Couldn't determime mime type for objectKey: " + fullObjectKey, e);
        }
        return uri;
    }

    private S3Object findS3Object(String mediaPackageID, String mediaPackageElementID) throws S3ServiceException {
        String objectKey = null;
        S3Object foundS3Obj = null;
        S3Object[] s3Objs = s3Service.listObjects(s3Bucket);
        for (S3Object s3Object : s3Objs) {
            objectKey = s3Object.getKey();
            if (objectKey.contains(mediaPackageID) && objectKey.contains(mediaPackageElementID)) {
                foundS3Obj = s3Object;
            }
        }
        return foundS3Obj;
    }

    private S3Object findS3Object(String mediaPackageID, String mediaPackageElementID, String fileName) throws S3ServiceException {
        String objectKey = null;
        S3Object foundS3Obj = null;
        S3Object[] s3Objs = s3Service.listObjects(s3Bucket);
        for (S3Object s3Object : s3Objs) {
            objectKey = s3Object.getKey();
            if (objectKey.contains(mediaPackageID) && objectKey.contains(mediaPackageElementID) && objectKey.contains(fileName)) {
                foundS3Obj = s3Object;
            }
        }
        return foundS3Obj;
    }

    private MimeType determineMimeType(String mediaPackageElementID) throws UnknownFileTypeException {
        String suffix = null;
        int separatorPos = mediaPackageElementID.lastIndexOf('.');
        if (separatorPos > 0 && separatorPos < mediaPackageElementID.length() - 1) {
            suffix = mediaPackageElementID.substring(separatorPos + 1);
        }
        else {
            throw new UnknownFileTypeException("Unable to get mime type without suffix");
        }
        MimeType mimeType = MimeTypes.fromSuffix(suffix);
        return mimeType;
    }

    private URI makeObjectURI(String objectKey) throws URISyntaxException {
        Jets3tProperties s3Props = s3Service.getJetS3tProperties();
        String host = S3Service.generateS3HostnameForBucket(s3Bucket.getName(), false);
        String scheme = s3Service.isHttpsOnly() ? "https" : "http";
        URI uri = new URI(scheme, host, objectKey);
        return uri;
    }

    private void checkService() {
        if (s3Service == null) {
            awsCredentials = new AWSCredentials(awsAccessKeyID, awsSecretAccessKey);
            try {
                s3Service = new RestS3Service(awsCredentials);
            }
            catch (S3ServiceException e) {
                logger.error("Couldn't log into S3 service", e);
            }
        }
    }

    private String makeObjectKey(String mediaPackageID, String mediaPackageElementID) {
        checkId(mediaPackageID);
        checkId(mediaPackageElementID);
        StringBuilder sb = new StringBuilder().append(mediaPackageID).append(s3Delimiter).append(mediaPackageElementID);
        return sb.toString();
    }

    private String makeObjectKey(String mediaPackageID, String mediaPackageElementID, String filename) {
        checkId(mediaPackageID);
        checkId(mediaPackageElementID);
        StringBuilder sb = new StringBuilder().append(mediaPackageID).append(s3Delimiter).append(mediaPackageElementID).append(s3Delimiter).append(filename);
        return sb.toString();
    }

    private void checkId(String id) {
        if (id == null)
            throw new NullPointerException("IDs can not be null");
        if (id.indexOf("..") > -1) { // || id.indexOf(File.separator) > -1
            throw new IllegalArgumentException("Invalid media package / element ID");
        }
    }

}
