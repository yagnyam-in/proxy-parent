package in.yagnyam.proxy.server.services;

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
     */
    public void saveRequest(@NonNull String requestId, @NonNull String requestType) {
        saveRequest(requestId, requestType, null);
    }


    /**
     * Save Request to DB
     *
     * @param request request to save to DB
     */
    public void saveRequest(@NonNull RequestEntity request) {
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
     */
    public void saveRequest(@NonNull String requestId, @NonNull String requestType, String requestPayload) {
        RequestEntity requestEntity = RequestEntity.builder()
                .requestId(requestId)
                .requestType(requestType)
                .requestPayload(requestPayload)
                .creationTime(new Date())
                .build();
        saveRequest(requestEntity);
    }
}
