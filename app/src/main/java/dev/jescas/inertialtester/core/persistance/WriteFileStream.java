package dev.jescas.inertialtester.core.persistance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteFileStream {
    private ByteArrayOutputStream byteStream;
    private File outputFile;
    private final int MAX_BUFFER_SIZE = 100 * 1024 * 1024;  // 100 MB
    String filename;

    public WriteFileStream(File directory, String prefix) {
        this.byteStream = new ByteArrayOutputStream();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());
         filename = prefix + currentDateAndTime + ".bin";
        this.outputFile = new File(directory, filename);
        byteStream.reset();
    }

    public String GetFilePath(){
        return filename;
    }


    /**
     * Append sensor data to the buffer.
     * @param values The accelerometer data to be written (3 float values).
     */
    public void AppendData(float[] values) {
        ByteBuffer block = CreateBufferBlock(values);
        byteStream.write(block.array(), 0, block.capacity());

        // Check if buffer exceeds the limit, and write to file if necessary
        if (byteStream.size() >= MAX_BUFFER_SIZE) {
            WriteBufferToFile();
            byteStream.reset();  // Reset buffer after writing to file
        }
    }

    /**
     * Create a byte buffer block from the sensor values and timestamp.
     * @param values The sensor values.
     * @return A ByteBuffer containing the timestamp and sensor data.
     */
    private ByteBuffer CreateBufferBlock(float[] values) {
        long timestamp = System.currentTimeMillis();
        ByteBuffer byteBuffer = ByteBuffer.allocate(20);  // 8 bytes for timestamp, 12 for 3 floats
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.putLong(timestamp);  // Add timestamp
        for (float value : values) byteBuffer.putFloat(value);  // Add accelerometer values
        return byteBuffer;
    }

    /**
     * Write the current buffer content to the file.
     */
    private void WriteBufferToFile() {
        try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
            byteStream.writeTo(fos);  // Write buffer to file
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the stream and write any remaining data to the file.
     */
    public void CloseStream() {
        if (byteStream.size() > 0) {
            WriteBufferToFile();  // Ensure any remaining data is written to the file
        }
        byteStream.reset();
    }
}
