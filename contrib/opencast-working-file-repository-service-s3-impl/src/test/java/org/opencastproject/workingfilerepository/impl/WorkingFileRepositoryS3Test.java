package org.opencastproject.workingfilerepository.impl;

import static org.opencastproject.workingfilerepository.impl.WorkingFileRepositoryS3Impl.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkingFileRepositoryS3Test {

    private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryS3Test.class);

    private WorkingFileRepository repo;

    @Before
    public void setup() {
        repo = new WorkingFileRepositoryS3Impl();
        InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties");
        Properties props = new Properties();
        try {
            props.load(is);
            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.getProperty(S3_BUCKET_NAME_PROP)).andReturn(props.getProperty(S3_BUCKET_NAME_PROP)).anyTimes();
            EasyMock.expect(bc.getProperty(S3_KEY_DELIM_PROP)).andReturn(props.getProperty(S3_KEY_DELIM_PROP)).anyTimes();
            EasyMock.expect(bc.getProperty(AWS_ACCESS_KEY_ID_PROP)).andReturn(props.getProperty(AWS_ACCESS_KEY_ID_PROP)).anyTimes();
            EasyMock.expect(bc.getProperty(AWS_SECRET_ACCESS_KEY_PROP)).andReturn(props.getProperty(AWS_SECRET_ACCESS_KEY_PROP)).anyTimes();
            EasyMock.expect(bc.getProperty(S3_EXPIRY_MINUTES_PROP)).andReturn(props.getProperty(S3_EXPIRY_MINUTES_PROP)).anyTimes();
            ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
            EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
            EasyMock.replay(bc, cc);
            ((WorkingFileRepositoryS3Impl) repo).activate(cc);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPutObject() {
        InputStream in = null;
        URI uri = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream("audio.mp3");
            Assert.assertNotNull(in);
            uri = repo.put("test", "audio.mp3", in);
            Assert.assertTrue(uri.toString().startsWith("https://johnk.s3.amazonaws.com/test/audio.mp3"));
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testGetObject() {
        InputStream is = repo.get("test", "audio.mp3");
        Assert.assertNotNull(is);
        try {
            is.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetURI() {
        URI uri = repo.getURI("test", "audio.mp3");
        Assert.assertNotNull(uri);
        Assert.assertTrue(uri.toString().startsWith("https://johnk.s3.amazonaws.com/test/audio.mp3"));
    }

    @Test
    public void testDeleteObject() {
        repo.delete("test", "audio.mp3");
        InputStream is = repo.get("test", "audio.mp3");
        Assert.assertNull(is);
    }

    @Test
    public void testPutObjectWithFilename() {
        InputStream in = null;
        URI uri = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream("audio.mp3");
            Assert.assertNotNull(in);
            uri = repo.put("test", "audio", "audio.mp3", in);
            Assert.assertTrue(uri.toString().startsWith("https://johnk.s3.amazonaws.com/test/audio/audio.mp3"));
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testDeleteObjectWithFileName() {
        repo.delete("test/audio", "audio.mp3");
        InputStream is = repo.get("test", "audio.mp3");
        Assert.assertNull(is);
    }
}
