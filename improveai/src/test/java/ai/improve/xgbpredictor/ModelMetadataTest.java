package ai.improve.xgbpredictor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static ai.improve.DecisionModelTest.DefaultFailMessage;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ai.improve.log.IMPLog;
import biz.k11i.xgboost.util.ModelReader;

public class ModelMetadataTest {
    public static final String Tag = "ModelMetadataTest";

    @Test
    public void testCanParseModel() {
        assertTrue(ModelMetadata.canParseModel(null, "7.0.1"));
        assertTrue(ModelMetadata.canParseModel("7.0.1", "7.0.1"));
        assertTrue(ModelMetadata.canParseModel("7.0.1", "7.0"));
        assertTrue(ModelMetadata.canParseModel("7.0.1", "7"));
        assertTrue(ModelMetadata.canParseModel("7.0", "7.0.1"));
        assertTrue(ModelMetadata.canParseModel("7", "7.0.1"));
        assertTrue(ModelMetadata.canParseModel("7.1.1", "7.0.1"));
        assertFalse(ModelMetadata.canParseModel("6.1.1", "7.0.1"));
        assertFalse(ModelMetadata.canParseModel("V7.1.1", "7.0.1"));
        assertFalse(ModelMetadata.canParseModel("77.1.1", "7.0.1"));
        assertFalse(ModelMetadata.canParseModel("", "7.0.1"));
    }

    @Test
    public void testParseMetadata_valid() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("metadata/metadata_valid");
        ModelReader modelReader = new ModelReader(new FileInputStream(new File(resource.toURI())));
        ModelMetadata metadata = new ModelMetadata(modelReader);
        IMPLog.d(Tag, metadata.getModelName());
    }

    @Test
    public void testParseMetadata_invalid() throws URISyntaxException {
        try {
            URL resource = getClass().getClassLoader().getResource("metadata/metadata_invalid");
            ModelReader modelReader = new ModelReader(new FileInputStream(new File(resource.toURI())));
            ModelMetadata metadata = new ModelMetadata(modelReader);
            IMPLog.d(Tag, metadata.getModelName());
        } catch (IOException e) {
            e.printStackTrace();
            return ;
        }
        fail("An IOException should have been thrown");
    }

    @Test
    public void testParseMetadata_no_version() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("metadata/metadata_no_version");
        ModelReader modelReader = new ModelReader(new FileInputStream(new File(resource.toURI())));
        ModelMetadata metadata = new ModelMetadata(modelReader);
        assertEquals("test", metadata.getModelName());
    }

    @Test
    public void testParseMetadata_outdated_version() throws URISyntaxException {
        try {
            URL resource = getClass().getClassLoader().getResource("metadata/metadata_outdated_version");
            ModelReader modelReader = new ModelReader(new FileInputStream(new File(resource.toURI())));
            ModelMetadata metadata = new ModelMetadata(modelReader);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(e.getMessage().startsWith("Major version don't match"));
            return ;
        }
        fail(DefaultFailMessage);
    }

    // Generate metadata for testing
    // Please copy the generated files to directory 'resources/metadata/'
    @Test
    public void testGen() throws IOException {
        // Valid metadata
        String userDefined = "{\"json\":{\"model_name\":\"test\",\"model_seed\":100000000,\"ai.improve.version\":\"7.0.1\",\"feature_names\":[\"12345678\"]}}";
        String path = "./metadata_valid";
        genMetadata(userDefined, path);

        // Invalid userDefined
        userDefined = "\"json\":{\"model_name\":\"test\",\"model_seed\":100000000,\"ai.improve.version\":\"7.0.1\",\"feature_names\":[\"12345678\"]}}";
        path = "./metadata_invalid";
        genMetadata(userDefined, path);

        // No model version
        userDefined = "{\"json\":{\"model_name\":\"test\",\"model_seed\":100000000,\"feature_names\":[\"12345678\"]}}";
        path = "./metadata_no_version";
        genMetadata(userDefined, path);

        // outdated model version
        userDefined = "{\"json\":{\"model_name\":\"test\",\"model_seed\":100000000,\"ai.improve.version\":\"1.0.1\",\"feature_names\":[\"12345678\"]}}";
        path = "./metadata_outdated_version";
        genMetadata(userDefined, path);
    }

    private void genMetadata(String userDefined, String path) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(path));
        DataOutputStream dos = new DataOutputStream(fos);
        dos.write(longToLittleEndianBytes(1));
        dos.write(longToLittleEndianBytes("user_defined_metadata".getBytes().length));
        dos.write("user_defined_metadata".getBytes());
        dos.write(longToLittleEndianBytes(userDefined.getBytes().length));
        dos.write(userDefined.getBytes());
        dos.close();
    }

    private byte[] longToLittleEndianBytes(long v) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(v);
        return buffer.array();
    }
}
