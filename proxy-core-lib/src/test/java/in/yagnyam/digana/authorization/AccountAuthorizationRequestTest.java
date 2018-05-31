package in.yagnyam.digana.authorization;

import in.yagnyam.digana.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by IT45SN on 22-8-2016.
 */
@Slf4j
public class AccountAuthorizationRequestTest {

    @Test
    public void testConstruction() {
        AccountAuthorizationRequest request =
                AccountAuthorizationRequest.builder()
                        .version("1")
                        .verificationCertificateSerial("1")
                        .verificationCertificateFingerPrintAlgorithm("RSA")
                        .verificationCertificateFingerPrint("ABC")
                        .signatureOneAlgorithm("SHA256")
                        .signatureTwoAlgorithm("MD5")
                        .requestNumber("RN")
                        .customerNumber("CN")
                        .bankNumber("BN")
                        .bankAccountNumber("AN")
                        .build();
        assertNotNull(request);
    }

    @Test
    public void testEquals() {
        AccountAuthorizationRequest request =
                AccountAuthorizationRequest.builder()
                        .version("1")
                        .verificationCertificateSerial("1")
                        .verificationCertificateFingerPrintAlgorithm("RSA")
                        .verificationCertificateFingerPrint("ABC")
                        .signatureOneAlgorithm("SHA256")
                        .signatureTwoAlgorithm("MD5")
                        .requestNumber("RN")
                        .customerNumber("CN")
                        .bankNumber("BN")
                        .bankAccountNumber("AN")
                        .build();
        assertNotNull(request);
        assertEquals(
                request,
                AccountAuthorizationRequest.builder()
                        .version("2")
                        .verificationCertificateSerial("3")
                        .verificationCertificateFingerPrintAlgorithm("RSA1")
                        .verificationCertificateFingerPrint("ABC1")
                        .signatureOneAlgorithm("SHA2")
                        .signatureTwoAlgorithm("MD5")
                        .requestNumber("RN")
                        .customerNumber("CN")
                        .bankNumber("XX")
                        .bankAccountNumber("YY")
                        .build());
        assertNotEquals(
                request,
                AccountAuthorizationRequest.builder()
                        .version("1")
                        .verificationCertificateSerial("1")
                        .verificationCertificateFingerPrintAlgorithm("RSA")
                        .verificationCertificateFingerPrint("ABC")
                        .signatureOneAlgorithm("SHA256")
                        .signatureTwoAlgorithm("MD5")
                        .requestNumber("RNA")
                        .customerNumber("CN")
                        .bankNumber("BN")
                        .bankAccountNumber("AN")
                        .build());
    }

    @Test
    public void testJson() {
        AccountAuthorizationRequest request =
                AccountAuthorizationRequest.builder()
                        .version("1")
                        .verificationCertificateSerial("1")
                        .verificationCertificateFingerPrintAlgorithm("RSA")
                        .verificationCertificateFingerPrint("ABC")
                        .signatureOneAlgorithm("SHA256")
                        .signatureTwoAlgorithm("MD5")
                        .requestNumber("RN")
                        .customerNumber("CN")
                        .bankNumber("BN")
                        .bankAccountNumber("AN")
                        .build();
        assertNotNull(request);
        log.info("toString:" + request);
        log.info("JSON:" + JsonUtils.toJson(request));
        AccountAuthorizationRequest fromJson =
                JsonUtils.fromJson(JsonUtils.toJson(request), AccountAuthorizationRequest.class);
        assertEquals(request, fromJson);
    }
}
