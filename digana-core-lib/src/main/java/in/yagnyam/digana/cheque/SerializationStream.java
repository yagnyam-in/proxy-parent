package in.yagnyam.digana.cheque;

import java.io.IOException;
import java.util.Date;

public interface SerializationStream {

    void writeVersion(String version) throws IOException;

    void writeDate(Date date) throws IOException;

    void writeNumber(long number) throws IOException;

    void writeString(String string) throws IOException;

    void writeNumber(double value) throws IOException;

    byte[] finish() throws IOException;
}
