package in.yagnyam.digana.cheque;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

@NoArgsConstructor(staticName = "create")
public class DERSerializationStream implements SerializationStream {

    private final ASN1EncodableVector asnVector = new ASN1EncodableVector();

    @Override
    public void writeVersion(@NonNull String version) {
        asnVector.add(new DERUTF8String(version));
    }

    @Override
    public void writeDate(@NonNull Date date) {
        asnVector.add(new DERGeneralizedTime(date));
    }

    @Override
    public void writeNumber(long number) {
        asnVector.add(new DERNumericString(Long.toString(number)));
    }

    @Override
    public void writeString(String string) {
        asnVector.add(new DERUTF8String(string == null ? "" : string));
    }

    @Override
    public void writeNumber(double value) {
        asnVector.add(new DERNumericString(String.format(Locale.ENGLISH, "%.6f", value + 0.0000001)));
    }

    @Override
    public byte[] finish() throws IOException {
        DERSequence seq = new DERSequence(asnVector);
        return seq.getEncoded();
    }
}
