package in.yagnyam.digana.authentication;

import in.yagnyam.digana.utils.Errors;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.*;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.jose4j.jwx.HeaderParameterNames.KEY_ID;

@Builder
@Slf4j
public class AuthenticationTokenService {

    @NonNull
    private VerificationKeyResolver verificationKeyResolver;

    /**
     * Sign Authentication Token and return as JWT
     *
     * @param token       Authentication Token
     * @param privateKey  Private Key to sign JWT
     * @param certificate Certificate to use for Validating JWT
     * @return signed JWT for given Authentication Token
     */
    public String signToken(
            @NonNull AuthenticationToken token,
            @NonNull PrivateKey privateKey,
            @NonNull X509Certificate certificate)
            throws JoseException {

        JwtClaims claims = convertTokenToClaims(token);
        claims.setExpirationTimeMinutesInTheFuture(60);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setX509CertSha1ThumbprintHeaderValue(certificate);
        jws.setX509CertSha256ThumbprintHeaderValue(certificate);
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue(certificate.getSerialNumber().toString());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        return jws.getCompactSerialization();
    }

    /**
     * Parse, Validate JWT and create Authentication Token
     *
     * @param jwt              JWT
     * @param expectedAudience Expected Audience from this JWT
     * @return Authentication Token
     */
    public Optional<AuthenticationToken> parseToken(
            @NonNull String jwt, @NonNull String expectedAudience) {
        try {
            JwtContext jwtContext = buildJwtConsumer(expectedAudience).process(jwt);
            return Optional.of(jwtContextToAuthenticationToken(jwtContext));
        } catch (Exception e) {
            log.info("Error extracting user information", e);
            return Optional.empty();
        }
    }

    /**
     * Converts Authentication Token to JWT claims
     *
     * @param token Authentication Token
     * @return Authentication Token as JWT claims
     */
    private JwtClaims convertTokenToClaims(@NonNull AuthenticationToken token) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(token.getIssuer()); // who creates the token and signs it
        claims.setAudience(token.getAudiences());
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow(); // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(
                2); // time before which the token is not yet valid (2 minutes ago)
        claims.setSubject(token.getSubject()); // the subject/principal is whom the token is about
        token.getStringAttributes().forEach(claims::setStringClaim);
        token.getStringListAttributes().forEach(claims::setStringListClaim);
        token
                .getDateAttributes()
                .forEach(
                        (k, v) -> claims.setNumericDateClaim(k, NumericDate.fromMilliseconds(v.getTime())));
        return claims;
    }

    /**
     * Build JWT consumer for given Expected Audience
     *
     * @param expectedAudience Expecting Audience
     * @return JWT Consumer for given Audience to parse the JWT
     */
    private JwtConsumer buildJwtConsumer(@NonNull String expectedAudience) {

        // And set up the allowed/expected algorithms
        AlgorithmConstraints algorithmConstraints =
                new AlgorithmConstraints(
                        AlgorithmConstraints.ConstraintType.WHITELIST,
                        AlgorithmIdentifiers.RSA_USING_SHA256,
                        AlgorithmIdentifiers.RSA_USING_SHA384);

        return new JwtConsumerBuilder()
                .setVerificationKeyResolver(verificationKeyResolver)
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedAudience(expectedAudience)
                .setJwsAlgorithmConstraints(algorithmConstraints)
                .build();
    }

    /**
     * Converts JWT Context/Claims to Authentication Token
     *
     * @param jwtContext JWT Context
     * @return Authentication Token
     * @throws InvalidJwtException     If any problem with parsing and validating JWT
     * @throws MalformedClaimException If any problem with parsing Claims in JWT
     */
    private AuthenticationToken jwtContextToAuthenticationToken(@NonNull JwtContext jwtContext)
            throws InvalidJwtException, MalformedClaimException {
        String keyId = extractKeyId(jwtContext);
        JwtClaims claims = jwtContext.getJwtClaims();

        AuthenticationToken.AuthenticationTokenBuilder tokenBuilder =
                AuthenticationToken.builder()
                        .keyId(keyId)
                        .subject(claims.getSubject())
                        .audiences(claims.getAudience())
                        .issuer(claims.getIssuer());

        claims.getClaimNames().forEach(
                Errors.rethrow()
                        .wrap(
                                claimName -> {
                                    if (claims.isClaimValueString(claimName))
                                        tokenBuilder.stringAttribute(
                                                claimName, claims.getStringClaimValue(claimName));
                                    else if (claims.isClaimValueStringList(claimName))
                                        tokenBuilder.stringListAttribute(
                                                claimName, claims.getStringListClaimValue(claimName));
                                    else if (claims.isClaimValueOfType(claimName, NumericDate.class))
                                        tokenBuilder.dateAttribute(
                                                claimName,
                                                new Date(
                                                        claims.getNumericDateClaimValue(claimName).getValueInMillis()));
                                }));
        return tokenBuilder.build();
    }

    private String extractKeyId(@NonNull JwtContext jwtContext) throws InvalidJwtException {
        return jwtContext
                .getJoseObjects()
                .stream()
                .filter(s -> s instanceof JsonWebSignature)
                .map(s -> (JsonWebSignature) s)
                .findFirst()
                .map(JsonWebStructure::getKeyIdHeaderValue)
                .orElseThrow(() -> invalidJwtException(ErrorCodes.MISCELLANEOUS, KEY_ID + " is missing", jwtContext));
    }

    private InvalidJwtException invalidJwtException(
            int errorCode, String errorMessage, JwtContext jwtContext) {
        return new InvalidJwtException(
                errorMessage,
                Collections.singletonList(new ErrorCodeValidator.Error(errorCode, errorMessage)),
                jwtContext);
    }
}
