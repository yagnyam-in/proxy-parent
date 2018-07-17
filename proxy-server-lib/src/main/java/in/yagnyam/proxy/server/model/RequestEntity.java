package in.yagnyam.proxy.server.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

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
