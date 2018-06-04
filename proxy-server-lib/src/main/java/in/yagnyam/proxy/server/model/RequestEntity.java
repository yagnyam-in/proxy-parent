package in.yagnyam.proxy.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import lombok.*;

import java.util.Date;

/**
 * Entity to store requests coming-in
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "requestId")
public class RequestEntity {

    @Id
    @NonNull
    private String requestId;

    @NonNull
    private String requestType;

    @Setter
    private Date creationTime;

    private Date expiryTime;

    private String requestPayload;

}
