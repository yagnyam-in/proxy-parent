package in.yagnyam.proxy.server.services;

import in.yagnyam.proxy.RequestMessage;
import in.yagnyam.proxy.SignableRequestMessage;
import in.yagnyam.proxy.SignedMessage;
import in.yagnyam.proxy.server.BadRequestException;
import in.yagnyam.proxy.server.ServiceException;
import in.yagnyam.proxy.server.db.RequestRepository;
import in.yagnyam.proxy.server.model.RequestEntity;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class RequestService {

    @NonNull
    private final RequestRepository requestRepository;

    @Builder
    public RequestService(@NonNull RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Helper method to validate the request Id
     * @param requestId Request Id
     * @param requestType Request Type
     * @throws BadRequestException if request is not unique
     */
    public void assertNewRequest(@NonNull String requestId, String requestType) throws BadRequestException {
        if (requestExistsWithId(requestId, requestType)) {
            throw ServiceException.badRequest("Duplicate Request Id");
        }
    }

    /**
     * Check if there exists a request given Id
     *
     * @param requestId   Request Id
     * @param requestType Request Type
     * @return true if a request exists with same Id
     */
    public boolean requestExistsWithId(@NonNull String requestId, String requestType) {
        return requestRepository.getRequest(requestId).isPresent();
    }

    /**
     * Save simple Request to DB
     *
     * @param requestId   Request Id
     * @param requestType Request Type
     * @throws BadRequestException if request is already processed
     */
    public void saveRequestEntity(@NonNull String requestId, @NonNull String requestType) throws BadRequestException {
        saveRequestEntity(requestId, requestType, null);
    }

    /**
     * Save Signed Request messages to DB
     *
     * @param request request to save to DB
     * @throws BadRequestException if request is already processed
     */
    public <T extends SignableRequestMessage> void saveSignedRequestMessage(@NonNull SignedMessage<T> request) throws BadRequestException {
        RequestEntity requestEntity = RequestEntity.builder()
                .requestId(request.getMessage().requestId())
                .requestType(request.getType())
                .creationTime(new Date())
                .build();
        requestRepository.saveRequest(requestEntity);
    }

    /**
     * Save Request messages to DB
     *
     * @param request request to save to DB
     * @throws BadRequestException if request is already processed
     */
    public void saveRequestMessage(@NonNull RequestMessage request) throws BadRequestException {
        RequestEntity requestEntity = RequestEntity.builder()
                .requestId(request.requestId())
                .requestType(request.getClass().getName())
                .creationTime(new Date())
                .build();
        requestRepository.saveRequest(requestEntity);
    }


    /**
     * Save Request to DB
     *
     * @param request request to save to DB
     * @throws BadRequestException if request is already processed
     */
    public void saveRequestEntity(@NonNull RequestEntity request) throws BadRequestException {
        if (request.getCreationTime() == null) {
            request.setCreationTime(new Date());
        }
        requestRepository.saveRequest(request);
    }


    /**
     * Save Request with Payload to DB
     *
     * @param requestId      Request ID
     * @param requestType    Request Type
     * @param requestPayload Request Payload
     * @throws BadRequestException if request is already processed
     */
    public void saveRequestEntity(@NonNull String requestId, @NonNull String requestType, String requestPayload) throws BadRequestException {
        RequestEntity requestEntity = RequestEntity.builder()
                .requestId(requestId)
                .requestType(requestType)
                .requestPayload(requestPayload)
                .creationTime(new Date())
                .build();
        saveRequestEntity(requestEntity);
    }
}
