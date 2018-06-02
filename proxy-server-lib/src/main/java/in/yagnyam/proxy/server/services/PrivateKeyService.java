package in.yagnyam.proxy.server.services;

import in.yagnyam.proxy.server.ServiceException;
import in.yagnyam.proxy.server.db.PrivateKeyRepository;
import in.yagnyam.proxy.server.model.PrivateKeyEntity;
import in.yagnyam.proxy.services.PemService;
import in.yagnyam.proxy.utils.StringUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Slf4j
public class PrivateKeyService {

    @NonNull
    private final PrivateKeyRepository privateKeyRepository;

    @NonNull
    private final PemService pemService;

    @Builder
    public PrivateKeyService(@NonNull PrivateKeyRepository privateKeyRepository, @NonNull PemService pemService) {
        this.privateKeyRepository = privateKeyRepository;
        this.pemService = pemService;
    }

    /**
     * Fetch the PrivateKeyEntity for given PrivateKeyEntity Id
     *
     * @param id PrivateKeyEntity Id
     * @return PrivateKeyEntity with given Id
     */
    public Optional<PrivateKeyEntity> getPrivateKey(@NonNull String id) {
        return privateKeyRepository.getPrivateKeyEntity(id).map(this::enrichPrivateEntity);
    }

    private PrivateKeyEntity enrichPrivateEntity(PrivateKeyEntity privateKeyEntity) {
        try {
            if (!StringUtils.isEmpty(privateKeyEntity.getCertificateEncoded())) {
                privateKeyEntity.setCertificate(pemService.decodeCertificate(privateKeyEntity.getCertificateEncoded()));
            }
            if (!StringUtils.isEmpty(privateKeyEntity.getPrivateKeyEncoded())) {
                privateKeyEntity.setPrivateKey(pemService.decodePrivateKey(privateKeyEntity.getPrivateKeyEncoded()));
            }
            return privateKeyEntity;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Unable to fetch privateKeyEntity", e);
            throw ServiceException.internalServerError("Incomplete Setup.", e);
        }
    }
}
